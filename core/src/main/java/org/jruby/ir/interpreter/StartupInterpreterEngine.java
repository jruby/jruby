package org.jruby.ir.interpreter;

import org.jruby.RubyModule;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CheckForLJEInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Error.runtimeError;

/**
 * This interpreter is meant to interpret the instructions generated directly from IRBuild.
 */
public class StartupInterpreterEngine extends InterpreterEngine {
    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, IRubyObject[] args, Block blockArg) {
        Instr[]   instrs    = interpreterContext.getInstructions();
        Object[]  temp      = interpreterContext.allocateTemporaryVariables();
        int       n         = instrs.length;
        int       ipc       = 0;
        Object    exception = null;

        boolean   ruby2Keywords = interpreterContext.isRuby2Keywords();

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        int[] rescuePCs = interpreterContext.getRescueIPCs();

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? Profiler.initProfiling(interpreterContext.getScope()) : 0;

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: " + ipc + ", R: "  + rescuePCs[ipc] + " - " + instr + ">");
                Interpreter.interpInstrsCount++;
            } else if (profile) {
                Profiler.instrTick(operation);
                Interpreter.interpInstrsCount++;
            }

            try {
                switch (operation.opClass) {
                    case ARG_OP:
                        receiveArg(context, instr, operation, self, args, ruby2Keywords,
                                currScope, currDynScope, temp, exception, blockArg);
                        break;
                    case CALL_OP:
                        if (profile) Profiler.updateCallSite(instr, interpreterContext.getScope(), scopeVersion);
                        processCall(context, instr, operation, currDynScope, currScope, temp, self, name);
                        break;
                    case RET_OP:
                        return processReturnOp(context, block, instr, operation, currDynScope, temp, self, currScope);
                    case BRANCH_OP:
                        switch (operation) {
                            case JUMP:
                                JumpInstr jump = ((JumpInstr)instr);
                                ipc = jump.getJumpTarget().getTargetPC();
                                break;
                            default:
                                ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc + 1);
                                break;

                        }
                        continue;
                    case BOOK_KEEPING_OP:
                        switch (operation) {
                            case PUSH_METHOD_BINDING:
                                // IMPORTANT: Preserve this update of currDynScope.
                                // This affects execution of all instructions in this scope
                                // which will now use the updated value of currDynScope.
                                currDynScope = interpreterContext.newDynamicScope(context);
                                context.pushScope(currDynScope);
                            case EXC_REGION_START:
                            case EXC_REGION_END:
                                break;
                            default:
                                processBookKeepingOp(context, block, instr, operation, name, args, self, blockArg, implClass, currDynScope, temp, currScope);
                        }
                        break;
                    case OTHER_OP:
                        processOtherOp(context, block, instr, operation, currDynScope, currScope, temp, self);
                        break;
                }

                ipc++;
            } catch (Throwable t) {
                if (debug) extractToMethodToAvoidC2Crash(instr, t);

                ipc = rescuePCs == null ? -1 : rescuePCs[ipc];

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
        throw runtimeError(context, "BUG: interpreter fell through to end unexpectedly");
    }

    protected static void processOtherOp(ThreadContext context, Block block, Instr instr, Operation operation, DynamicScope currDynScope,
                                         StaticScope currScope, Object[] temp, IRubyObject self) {
        switch(operation) {
            case RECV_SELF:
                break;
            case COPY: {
                CopyInstr c = (CopyInstr)instr;
                setResult(temp, currDynScope, c.getResult(), retrieveOp(c.getSource(), context, self, currDynScope, currScope, temp));
                break;
            }
            case RUNTIME_HELPER: {
                RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                setResult(temp, currDynScope, rhc.getResult(),
                        rhc.callHelper(context, currScope, currDynScope, self, temp, block));
                break;
            }
            case CHECK_FOR_LJE:
                ((CheckForLJEInstr) instr).check(context, currDynScope, block);
                break;

            case LOAD_FRAME_CLOSURE:
                setResult(temp, currDynScope, instr, context.getFrameBlock());
                break;

            case LOAD_BLOCK_IMPLICIT_CLOSURE:
                setResult(temp, currDynScope, instr, Helpers.getImplicitBlockFromBlockBinding(block));
                return;

            // ---------- All the rest ---------
            default:
                setResult(temp, currDynScope, instr, instr.interpret(context, currScope, currDynScope, self, temp));
                break;
        }
    }

}
