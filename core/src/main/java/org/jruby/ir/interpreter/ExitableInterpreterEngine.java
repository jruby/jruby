package org.jruby.ir.interpreter;

import org.jruby.RubyModule;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.boxing.AluInstr;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * InterpreterEngine capable of exiting part way through execution up to a particular instruction.  Continued
 * execution will assign the result of that instruction which we pass in and continue executing from that point.
 * The mechanism for this interpreter to work is a context object which contains the IPC, the result, and the
 * temporary variables.
 *
 * Extra Notes:
 *   - We may need to make all natural split arities here if a JIT calls this directly.
 *   - I dislike replicating this code but we do not want our default interpreter to have an extra stack frame.
 */
public class ExitableInterpreterEngine extends InterpreterEngine {
    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass, String name,
                                 IRubyObject[] args, Block blockArg, ExitableInterpreterEngineContext executionContext) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp      = executionContext.getTemporaryVariables(interpreterContext);
        double[] floats    = executionContext.getTemporaryFloatVariables(interpreterContext);
        long[] fixnums   = executionContext.getTemporaryFixnumVariables(interpreterContext);
        boolean[] booleans  = executionContext.getTemporaryBooleanVariables(interpreterContext);
        int n = instrs.length;
        int ipc = executionContext.getIPC();
        Object exception = null;
        boolean acceptsKeywordArgument = interpreterContext.receivesKeywordArguments();

        if (acceptsKeywordArgument) {
            args = IRRuntimeHelpers.frobnicateKwargsArgument(context, args, interpreterContext.getRequiredArgsCount());
        }

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? Profiler.initProfiling(interpreterContext.getScope()) : 0;

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {" + ipc + "} " + instr);
                Interpreter.interpInstrsCount++;
            } else if (profile) {
                Profiler.instrTick(operation);
                Interpreter.interpInstrsCount++;
            }

            ipc++;

            try {
                switch (operation.opClass) {
                    case INT_OP:
                        interpretIntOp((AluInstr) instr, operation, fixnums, booleans);
                        break;
                    case FLOAT_OP:
                        interpretFloatOp((AluInstr) instr, operation, floats, booleans);
                        break;
                    case ARG_OP:
                        receiveArg(context, instr, operation, args, acceptsKeywordArgument, currDynScope, temp, exception, blockArg);
                        break;
                    case CALL_OP:
                        if (profile) Profiler.updateCallSite(instr, interpreterContext.getScope(), scopeVersion);
                        processCall(context, instr, operation, currDynScope, currScope, temp, self);
                        break;
                    case RET_OP:
                        return processReturnOp(context, block, instr, operation, currDynScope, temp, self, currScope);
                    case BRANCH_OP:
                        switch (operation) {
                            case JUMP: ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC(); break;
                            default: ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc); break;
                        }
                        break;
                    case BOOK_KEEPING_OP:
                        // IMPORTANT: Preserve these update to currDynScope, self, and args.
                        // They affect execution of all following instructions in this scope.
                        switch (operation) {
                            case PUSH_METHOD_BINDING:
                                currDynScope = interpreterContext.newDynamicScope(context);
                                context.pushScope(currDynScope);
                                break;
                            case PUSH_BLOCK_BINDING:
                                currDynScope = IRRuntimeHelpers.pushBlockDynamicScopeIfNeeded(context, block, interpreterContext.pushNewDynScope(), interpreterContext.reuseParentDynScope());
                                break;
                            case UPDATE_BLOCK_STATE:
                                self = IRRuntimeHelpers.updateBlockState(block, self);
                                break;
                            case PREPARE_NO_BLOCK_ARGS:
                                args = IRRuntimeHelpers.prepareNoBlockArgs(context, block, args);
                                break;
                            case PREPARE_SINGLE_BLOCK_ARG:
                                args = IRRuntimeHelpers.prepareSingleBlockArgs(context, block, args);
                                break;
                            case PREPARE_FIXED_BLOCK_ARGS:
                                args = IRRuntimeHelpers.prepareFixedBlockArgs(context, block, args);
                                break;
                            case PREPARE_BLOCK_ARGS:
                                args = IRRuntimeHelpers.prepareBlockArgs(context, block, args, acceptsKeywordArgument);
                                break;
                            default:
                                processBookKeepingOp(context, block, instr, operation, name, args, self, blockArg, implClass, currDynScope, temp, currScope);
                                break;
                        }
                        break;
                    case OTHER_OP:
                        processOtherOp(context, block, instr, operation, currDynScope, currScope, temp, self, floats, fixnums, booleans);
                        break;
                }
            } catch (Throwable t) {
                if (debug) extractToMethodToAvoidC2Crash(instr, t);

                // StartupInterpreterEngine never calls this method so we know it is a full build.
                ipc = ((FullInterpreterContext) interpreterContext).determineRPC(ipc);

                if (debug) {
                    Interpreter.LOG.info("in : " + interpreterContext.getScope() + ", caught Java throwable: " + t + "; excepting instr: " + instr);
                    Interpreter.LOG.info("ipc for rescuer: " + ipc);
                }

                if (ipc == -1) {
                    Helpers.throwException(t);
                } else {
                    exception = t;
                }
            }
        }

        // Control should never get here!
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }
}
