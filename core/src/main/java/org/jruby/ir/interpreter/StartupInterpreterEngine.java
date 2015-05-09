package org.jruby.ir.interpreter;

import java.util.Stack;
import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CheckForLJEInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.runtime.IRReturnJump;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.opto.ConstantCache;

/**
 * This interpreter is meant to interpret the instructions generated directly from IRBuild.
 */
public class StartupInterpreterEngine extends InterpreterEngine {
    public IRubyObject interpret(ThreadContext context, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, IRubyObject[] args, Block block, Block.Type blockType) {
        Instr[]   instrs    = interpreterContext.getInstructions();
        Object[]  temp      = interpreterContext.allocateTemporaryVariables();
        int       n         = instrs.length;
        int       ipc       = 0;
        Object    exception = null;

        if (interpreterContext.receivesKeywordArguments()) IRRuntimeHelpers.frobnicateKwargsArgument(context, interpreterContext.getRequiredArgsCount(), args);

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();
        IRScope scope = currScope.getIRScope();
        boolean      acceptsKeywordArgument = interpreterContext.receivesKeywordArguments();

        Stack<Integer> rescuePCs = new Stack<>();

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? Profiler.initProfiling(scope) : 0;

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {" + ipc + "} ", instr);
                Interpreter.interpInstrsCount++;
            } else if (profile) {
                Profiler.instrTick(operation);
                Interpreter.interpInstrsCount++;
            }

            ipc++;

            try {
                switch (operation.opClass) {
                    case ARG_OP:
                        receiveArg(context, instr, operation, args, acceptsKeywordArgument, currDynScope, temp, exception, block);
                        break;
                    case CALL_OP:
                        if (profile) Profiler.updateCallSite(instr, scope, scopeVersion);
                        processCall(context, instr, operation, currDynScope, currScope, temp, self);
                        break;
                    case RET_OP:
                        return processReturnOp(context, instr, operation, currDynScope, temp, self, blockType, currScope);
                    case BRANCH_OP:
                        switch (operation) {
                            case JUMP: ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC(); break;
                            default: ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc); break;
                        }
                        break;
                    case BOOK_KEEPING_OP:
                        if (operation == Operation.PUSH_BINDING) {
                            // IMPORTANT: Preserve this update of currDynScope.
                            // This affects execution of all instructions in this scope
                            // which will now use the updated value of currDynScope.
                            currDynScope = interpreterContext.newDynamicScope(context);
                            context.pushScope(currDynScope);
                        } else {
                            processBookKeepingOp(context, instr, operation, name, args, self, block, blockType, implClass, rescuePCs);
                        }
                        break;
                    case OTHER_OP:
                        processOtherOp(context, instr, operation, currDynScope, currScope, temp, self, blockType);
                        break;
                }
            } catch (Throwable t) {
                if (debug) extractToMethodToAvoidC2Crash(instr, t);

                if (rescuePCs.empty() || (t instanceof IRBreakJump && instr instanceof BreakInstr) ||
                        (t instanceof IRReturnJump && instr instanceof NonlocalReturnInstr)) {
                    ipc = -1;
                } else {
                    ipc = rescuePCs.pop();
                }

                if (debug) {
                    Interpreter.LOG.info("in : " + interpreterContext.getStaticScope().getIRScope() + ", caught Java throwable: " + t + "; excepting instr: " + instr);
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

    protected static void processOtherOp(ThreadContext context, Instr instr, Operation operation, DynamicScope currDynScope,
                                         StaticScope currScope, Object[] temp, IRubyObject self, Block.Type blockType) {
        switch(operation) {
            case RECV_SELF:
                break;
            case COPY: {
                CopyInstr c = (CopyInstr)instr;
                setResult(temp, currDynScope, c.getResult(), retrieveOp(c.getSource(), context, self, currDynScope, currScope, temp));
                break;
            }
            case GET_FIELD: {
                GetFieldInstr gfi = (GetFieldInstr)instr;
                IRubyObject object = (IRubyObject)gfi.getSource().retrieve(context, self, currScope, currDynScope, temp);
                VariableAccessor a = gfi.getAccessor(object);
                Object result = a == null ? null : (IRubyObject)a.get(object);
                if (result == null) {
                    if (context.runtime.isVerbose()) {
                        context.runtime.getWarnings().warning(IRubyWarnings.ID.IVAR_NOT_INITIALIZED, "instance variable " + gfi.getRef() + " not initialized");
                    }
                    result = context.nil;
                }
                setResult(temp, currDynScope, gfi.getResult(), result);
                break;
            }
            case SEARCH_CONST: {
                SearchConstInstr sci = (SearchConstInstr)instr;
                ConstantCache cache = sci.getConstantCache();
                Object result = !ConstantCache.isCached(cache) ?
                    sci.cache(context, currScope, currDynScope, self, temp) : cache.value;
                setResult(temp, currDynScope, sci.getResult(), result);
                break;
            }
            case RUNTIME_HELPER: {
                RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                setResult(temp, currDynScope, rhc.getResult(),
                        rhc.callHelper(context, currScope, currDynScope, self, temp, blockType));
                break;
            }
            case CHECK_FOR_LJE:
                ((CheckForLJEInstr) instr).check(context, currDynScope, blockType);
                break;
            case LOAD_FRAME_CLOSURE:
                setResult(temp, currDynScope, instr, context.getFrameBlock());
                return;
            // ---------- All the rest ---------
            default:
                setResult(temp, currDynScope, instr, instr.interpret(context, currScope, currDynScope, self, temp));
                break;
        }
    }

}
