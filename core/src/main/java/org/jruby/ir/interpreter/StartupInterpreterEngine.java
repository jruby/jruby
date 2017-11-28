package org.jruby.ir.interpreter;

import java.util.EmptyStackException;
import java.util.Stack;
import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CheckForLJEInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
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
    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, IRubyObject[] args, Block blockArg) {
        Instr[]   instrs    = interpreterContext.getInstructions();
        Object[]  temp      = interpreterContext.allocateTemporaryVariables();
        int       n         = instrs.length;
        int       ipc       = 0;
        Object    exception = null;

        if (interpreterContext.receivesKeywordArguments()) args = IRRuntimeHelpers.frobnicateKwargsArgument(context, args, interpreterContext.getRequiredArgsCount());

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();
        boolean      acceptsKeywordArgument = interpreterContext.receivesKeywordArguments();

        Stack<ExceptionRegionStartMarkerInstr> rescuePCs = null;

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? Profiler.initProfiling(interpreterContext.getScope()) : 0;

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {" + ipc + "} " + instr + "; <#RPCs=" + (rescuePCs == null ? 0 : rescuePCs.size()) + ">");
                Interpreter.interpInstrsCount++;
            } else if (profile) {
                Profiler.instrTick(operation);
                Interpreter.interpInstrsCount++;
            }

            ipc++;

            try {
                switch (operation.opClass) {
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
                            case JUMP:
                                JumpInstr jump = ((JumpInstr)instr);
                                ipc = jump.getJumpTarget().getTargetPC();
                                break;
                            default:
                                ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                                break;
                        }
                        break;
                    case BOOK_KEEPING_OP:
                        switch (operation) {
                            case PUSH_METHOD_BINDING:
                                // IMPORTANT: Preserve this update of currDynScope.
                                // This affects execution of all instructions in this scope
                                // which will now use the updated value of currDynScope.
                                currDynScope = interpreterContext.newDynamicScope(context);
                                context.pushScope(currDynScope);
                            case EXC_REGION_START: {
                                if (rescuePCs == null) {
                                    rescuePCs = new Stack<>();
                                    rescuePCs.push((ExceptionRegionStartMarkerInstr) instr);
                                } else {
                                    // We use EXC_REGION_{START,END} as actual instructions instead of markers
                                    // in this particular interpreter.  Unfortunately, these can never be guaranteed to
                                    // execute in matched pairs since other instrs (like from a jump representing a Ruby
                                    // next) may happen before hitting the END instr.  Because of this we will look to
                                    // see if the stack is dirty and prune back to a proper clean point.  Otherwise it is
                                    // clean and we push a new entry.  This mechanism works because these exc. regions
                                    // represent lexical boundaries and you cannot see the same boundary nested in itself.
                                    // If we try to push something already there then the space-time continuum is blown
                                    // and we have to clean the universe up.
                                    pushOrPrune((ExceptionRegionStartMarkerInstr) instr, rescuePCs);
                                }
                            }
                            break;
                            case EXC_REGION_END:
                                try {
                                    rescuePCs.pop();
                                } catch (EmptyStackException e) {
                                    System.out.println("WHHOOOPS: " + interpreterContext.toStringInstrs() + ", IP: " + ipc);
                                }
                                break;
                            default:
                                processBookKeepingOp(context, block, instr, operation, name, args, self, blockArg, implClass, currDynScope, temp, currScope);
                        }
                        break;
                    case OTHER_OP:
                        processOtherOp(context, block, instr, operation, currDynScope, currScope, temp, self);
                        break;
                }
            } catch (Throwable t) {
                if (debug) extractToMethodToAvoidC2Crash(instr, t);

                if (rescuePCs == null || rescuePCs.empty()) {
                    ipc = -1;
                } else {
                    ipc = rescuePCs.pop().getFirstRescueBlockLabel().getTargetPC();
                }

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

    private void pushOrPrune(ExceptionRegionStartMarkerInstr element, Stack<ExceptionRegionStartMarkerInstr> stack) {
        int firstOccurrence = stack.indexOf(element);

        if (firstOccurrence != -1) {
            int elementsToPrune = stack.size() - (firstOccurrence + 1);

            for (int i = 0; i < elementsToPrune; i++) {
                stack.pop();
            }
        } else {
            stack.push(element);
        }
    }

    protected static void processOtherOp(ThreadContext context, Block block, Instr instr, Operation operation, DynamicScope currDynScope,
                                         StaticScope currScope, Object[] temp, IRubyObject self) {
        Block.Type blockType = block == null ? null : block.type;
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
