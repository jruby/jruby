package org.jruby.ir.interpreter;

import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.Unrescuable;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CheckForLJEInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.PopBlockFrameInstr;
import org.jruby.ir.instructions.PushBlockFrameInstr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.instructions.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.RestoreBindingVisibilityInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.ReturnOrRethrowSavedExcInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SaveBindingVisibilityInstr;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.ToggleBacktraceInstr;
import org.jruby.ir.instructions.TraceInstr;
import org.jruby.ir.instructions.boxing.AluInstr;
import org.jruby.ir.instructions.boxing.BoxBooleanInstr;
import org.jruby.ir.instructions.boxing.BoxFixnumInstr;
import org.jruby.ir.instructions.boxing.BoxFloatInstr;
import org.jruby.ir.instructions.boxing.BoxInstr;
import org.jruby.ir.instructions.boxing.UnboxInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneFloatArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.TemporaryFixnumVariable;
import org.jruby.ir.operands.TemporaryFloatVariable;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UnboxedBoolean;
import org.jruby.ir.operands.UnboxedFixnum;
import org.jruby.ir.operands.UnboxedFloat;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.opto.ConstantCache;

/**
 * Base full interpreter.  Subclasses can use utility methods here and override what they want.  This method requires
 * that it has fully built and has had a CFG made, etc...
 */
public class InterpreterEngine {

    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, Block blockArg) {
        return interpret(context, block, self, interpreterContext, implClass, name, IRubyObject.NULL_ARRAY, blockArg);
    }

    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, IRubyObject arg1, Block blockArg) {
        return interpret(context, block, self, interpreterContext, implClass, name, new IRubyObject[] {arg1}, blockArg);
    }

    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, IRubyObject arg1, IRubyObject arg2, Block blockArg) {
        return interpret(context, block, self, interpreterContext, implClass, name, new IRubyObject[] {arg1, arg2}, blockArg);
    }

    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block blockArg) {
        return interpret(context, block, self, interpreterContext, implClass, name, new IRubyObject[] {arg1, arg2, arg3}, blockArg);
    }

    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, RubyModule implClass,
                                 String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, Block blockArg) {
        return interpret(context, block, self, interpreterContext, implClass, name, new IRubyObject[] {arg1, arg2, arg3, arg4}, blockArg);
    }

    public IRubyObject interpret(ThreadContext context, Block block, IRubyObject self,
                                         InterpreterContext interpreterContext, RubyModule implClass,
                                         String name, IRubyObject[] args, Block blockArg) {
        Instr[]   instrs    = interpreterContext.getInstructions();
        Object[]  temp      = interpreterContext.allocateTemporaryVariables();
        double[]  floats    = interpreterContext.allocateTemporaryFloatVariables();
        long[]    fixnums   = interpreterContext.allocateTemporaryFixnumVariables();
        boolean[] booleans  = interpreterContext.allocateTemporaryBooleanVariables();
        int       n         = instrs.length;
        int       ipc       = 0;
        Object    exception = null;
        boolean   acceptsKeywordArgument = interpreterContext.receivesKeywordArguments();

        // Blocks with explicit call protocol shouldn't do this before args are prepared
        if (acceptsKeywordArgument && (block == null || !interpreterContext.hasExplicitCallProtocol())) {
            IRRuntimeHelpers.frobnicateKwargsArgument(context, args, interpreterContext.getRequiredArgsCount());
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
                Interpreter.LOG.info("I: {" + ipc + "} ", instr);
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

                ipc = instr.getRPC();

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

    protected static void interpretIntOp(AluInstr instr, Operation op, long[] fixnums, boolean[] booleans) {
        TemporaryLocalVariable dst = (TemporaryLocalVariable)instr.getResult();
        long i1 = getFixnumArg(fixnums, instr.getArg1());
        long i2 = getFixnumArg(fixnums, instr.getArg2());
        switch (op) {
            case IADD: setFixnumVar(fixnums, dst, i1 + i2); break;
            case ISUB: setFixnumVar(fixnums, dst, i1 - i2); break;
            case IMUL: setFixnumVar(fixnums, dst, i1 * i2); break;
            case IDIV: setFixnumVar(fixnums, dst, i1 / i2); break;
            case IOR : setFixnumVar(fixnums, dst, i1 | i2); break;
            case IAND: setFixnumVar(fixnums, dst, i1 & i2); break;
            case IXOR: setFixnumVar(fixnums, dst, i1 ^ i2); break;
            case ISHL: setFixnumVar(fixnums, dst, i1 << i2); break;
            case ISHR: setFixnumVar(fixnums, dst, i1 >> i2); break;
            case ILT : setBooleanVar(booleans, dst, i1 < i2); break;
            case IGT : setBooleanVar(booleans, dst, i1 > i2); break;
            case IEQ : setBooleanVar(booleans, dst, i1 == i2); break;
            default: throw new RuntimeException("Unhandled int op: " + op + " for instr " + instr);
        }
    }

    protected static void interpretFloatOp(AluInstr instr, Operation op, double[] floats, boolean[] booleans) {
        TemporaryLocalVariable dst = (TemporaryLocalVariable)instr.getResult();
        double a1 = getFloatArg(floats, instr.getArg1());
        double a2 = getFloatArg(floats, instr.getArg2());
        switch (op) {
            case FADD: setFloatVar(floats, dst, a1 + a2); break;
            case FSUB: setFloatVar(floats, dst, a1 - a2); break;
            case FMUL: setFloatVar(floats, dst, a1 * a2); break;
            case FDIV: setFloatVar(floats, dst, a1 / a2); break;
            case FLT : setBooleanVar(booleans, dst, a1 < a2); break;
            case FGT : setBooleanVar(booleans, dst, a1 > a2); break;
            case FEQ : setBooleanVar(booleans, dst, a1 == a2); break;
            default: throw new RuntimeException("Unhandled float op: " + op + " for instr " + instr);
        }
    }

    protected static void receiveArg(ThreadContext context, Instr i, Operation operation, IRubyObject[] args, boolean acceptsKeywordArgument, DynamicScope currDynScope, Object[] temp, Object exception, Block blockArg) {
        Object result;
        ResultInstr instr = (ResultInstr)i;

        switch(operation) {
            case RECV_PRE_REQD_ARG:
                int argIndex = ((ReceivePreReqdArgInstr)instr).getArgIndex();
                result = IRRuntimeHelpers.getPreArgSafe(context, args, argIndex);
                setResult(temp, currDynScope, instr.getResult(), result);
                return;
            case RECV_POST_REQD_ARG:
                result = ((ReceivePostReqdArgInstr)instr).receivePostReqdArg(context, args, acceptsKeywordArgument);
                setResult(temp, currDynScope, instr.getResult(), result);
                return;
            case RECV_RUBY_EXC:
                setResult(temp, currDynScope, instr.getResult(), IRRuntimeHelpers.unwrapRubyException(exception));
                return;
            case RECV_JRUBY_EXC:
                setResult(temp, currDynScope, instr.getResult(), exception);
                return;
            case LOAD_IMPLICIT_CLOSURE:
                setResult(temp, currDynScope, instr.getResult(), blockArg);
                return;
            default:
                result = ((ReceiveArgBase)instr).receiveArg(context, args, acceptsKeywordArgument);
                setResult(temp, currDynScope, instr.getResult(), result);
        }
    }

    protected static void processCall(ThreadContext context, Instr instr, Operation operation, DynamicScope currDynScope, StaticScope currScope, Object[] temp, IRubyObject self) {
        Object result;

        switch(operation) {
            case CALL_1F: {
                OneFixnumArgNoBlockCallInstr call = (OneFixnumArgNoBlockCallInstr)instr;
                IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                result = call.getCallSite().call(context, self, r, call.getFixnumArg());
                setResult(temp, currDynScope, call.getResult(), result);
                break;
            }
            case CALL_1D: {
                OneFloatArgNoBlockCallInstr call = (OneFloatArgNoBlockCallInstr)instr;
                IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                result = call.getCallSite().call(context, self, r, call.getFloatArg());
                setResult(temp, currDynScope, call.getResult(), result);
                break;
            }
            case CALL_1O: {
                OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr)instr;
                IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                result = call.getCallSite().call(context, self, r, o);
                setResult(temp, currDynScope, call.getResult(), result);
                break;
            }
            case CALL_1OB: {
                OneOperandArgBlockCallInstr call = (OneOperandArgBlockCallInstr)instr;
                IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                Block preparedBlock = call.prepareBlock(context, self, currScope, currDynScope, temp);
                result = call.getCallSite().call(context, self, r, o, preparedBlock);
                setResult(temp, currDynScope, call.getResult(), result);
                break;
            }
            case CALL_0O: {
                ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
                IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                result = call.getCallSite().call(context, self, r);
                setResult(temp, currDynScope, call.getResult(), result);
                break;
            }
            case NORESULT_CALL_1O: {
                OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr)instr;
                IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                call.getCallSite().call(context, self, r, o);
                break;
            }
            case NORESULT_CALL:
                instr.interpret(context, currScope, currDynScope, self, temp);
                break;
            case CALL:
            default:
                result = instr.interpret(context, currScope, currDynScope, self, temp);
                setResult(temp, currDynScope, instr, result);
                break;
        }
    }

    protected static void processBookKeepingOp(ThreadContext context, Block block, Instr instr, Operation operation,
                                             String name, IRubyObject[] args, IRubyObject self, Block blockArg, RubyModule implClass,
                                             DynamicScope currDynScope, Object[] temp, StaticScope currScope) {
        switch(operation) {
            case LABEL:
                break;
            case SAVE_BINDING_VIZ:
                setResult(temp, currDynScope, ((SaveBindingVisibilityInstr) instr).getResult(), block.getBinding().getFrame().getVisibility());
                break;
            case RESTORE_BINDING_VIZ:
                block.getBinding().getFrame().setVisibility((Visibility) retrieveOp(((RestoreBindingVisibilityInstr) instr).getVisibility(), context, self, currDynScope, currScope, temp));
                break;
            case PUSH_BLOCK_FRAME:
                setResult(temp, currDynScope, ((PushBlockFrameInstr) instr).getResult(), context.preYieldNoScope(block.getBinding()));
                break;
            case POP_BLOCK_FRAME:
                context.postYieldNoScope((Frame) retrieveOp(((PopBlockFrameInstr)instr).getFrame(), context, self, currDynScope, currScope, temp));
                break;
            case PUSH_METHOD_FRAME:
                context.preMethodFrameOnly(implClass, name, self, blockArg);
                // Only the top-level script scope has PRIVATE visibility.
                // This is already handled as part of Interpreter.execute above.
                // Everything else is PUBLIC by default.
                context.setCurrentVisibility(Visibility.PUBLIC);
                break;
            case POP_METHOD_FRAME:
                context.popFrame();
                break;
            case POP_BINDING:
                context.popScope();
                break;
            case THREAD_POLL:
                if (IRRuntimeHelpers.inProfileMode()) Profiler.clockTick();
                context.callThreadPoll();
                break;
            case CHECK_ARITY:
                ((CheckArityInstr) instr).checkArity(context, args, block == null ? null : block.type);
                break;
            case LINE_NUM:
                context.setLine(((LineNumberInstr)instr).lineNumber);
                break;
            case TOGGLE_BACKTRACE:
                context.setExceptionRequiresBacktrace(((ToggleBacktraceInstr) instr).requiresBacktrace());
                break;
            case TRACE: {
                if (context.runtime.hasEventHooks()) {
                    TraceInstr trace = (TraceInstr) instr;
                    // FIXME: Try and statically generate END linenumber instead of hacking it.
                    int linenumber = trace.getLinenumber() == -1 ? context.getLine()+1 : trace.getLinenumber();

                    context.trace(trace.getEvent(), trace.getName(), context.getFrameKlazz(),
                            trace.getFilename(), linenumber);
                }
                break;
            }
        }
    }

    protected static IRubyObject processReturnOp(ThreadContext context, Block block, Instr instr, Operation operation,
                                                 DynamicScope currDynScope, Object[] temp, IRubyObject self,
                                                 StaticScope currScope) {
        Block.Type blockType = block == null ? null : block.type;
        switch(operation) {
            // --------- Return flavored instructions --------
            case RETURN: {
                return (IRubyObject)retrieveOp(((ReturnBase)instr).getReturnValue(), context, self, currDynScope, currScope, temp);
            }
            case BREAK: {
                BreakInstr bi = (BreakInstr)instr;
                IRubyObject rv = (IRubyObject)bi.getReturnValue().retrieve(context, self, currScope, currDynScope, temp);
                // This also handles breaks in lambdas -- by converting them to a return
                //
                // This assumes that scopes with break instr. have a frame / dynamic scope
                // pushed so that we can get to its static scope. For-loops now always have
                // a dyn-scope pushed onto stack which makes this work in all scenarios.
                return IRRuntimeHelpers.initiateBreak(context, currDynScope, rv, blockType);
            }
            case NONLOCAL_RETURN: {
                NonlocalReturnInstr ri = (NonlocalReturnInstr)instr;
                IRubyObject rv = (IRubyObject)retrieveOp(ri.getReturnValue(), context, self, currDynScope, currScope, temp);
                return IRRuntimeHelpers.initiateNonLocalReturn(context, currDynScope, blockType, rv);
            }
            case RETURN_OR_RETHROW_SAVED_EXC: {
                IRubyObject retVal = (IRubyObject) retrieveOp(((ReturnBase) instr).getReturnValue(), context, self, currDynScope, currScope, temp);
                return IRRuntimeHelpers.returnOrRethrowSavedException(context, retVal);
            }
        }
        return null;
    }

    protected static void processOtherOp(ThreadContext context, Block block, Instr instr, Operation operation, DynamicScope currDynScope,
                                         StaticScope currScope, Object[] temp, IRubyObject self,
                                         double[] floats, long[] fixnums, boolean[] booleans) {
        Block.Type blockType = block == null ? null : block.type;
        Object result;
        switch(operation) {
            case RECV_SELF:
                break;
            case COPY: {
                CopyInstr c = (CopyInstr)instr;
                Operand src = c.getSource();
                Variable res = c.getResult();
                if (res instanceof TemporaryFloatVariable) {
                    setFloatVar(floats, (TemporaryFloatVariable)res, getFloatArg(floats, src));
                } else if (res instanceof TemporaryFixnumVariable) {
                    setFixnumVar(fixnums, (TemporaryFixnumVariable)res, getFixnumArg(fixnums, src));
                } else {
                    setResult(temp, currDynScope, res, retrieveOp(src, context, self, currDynScope, currScope, temp));
                }
                break;
            }

            case GET_FIELD: {
                GetFieldInstr gfi = (GetFieldInstr)instr;
                IRubyObject object = (IRubyObject)gfi.getSource().retrieve(context, self, currScope, currDynScope, temp);
                VariableAccessor a = gfi.getAccessor(object);
                result = a == null ? null : (IRubyObject)a.get(object);
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
                if (!ConstantCache.isCached(cache)) {
                    result = sci.cache(context, currScope, currDynScope, self, temp);
                } else {
                    result = cache.value;
                }
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

            case BOX_FLOAT: {
                RubyFloat f = context.runtime.newFloat(getFloatArg(floats, ((BoxFloatInstr)instr).getValue()));
                setResult(temp, currDynScope, ((BoxInstr)instr).getResult(), f);
                break;
            }

            case BOX_FIXNUM: {
                RubyFixnum f = context.runtime.newFixnum(getFixnumArg(fixnums, ((BoxFixnumInstr) instr).getValue()));
                setResult(temp, currDynScope, ((BoxInstr)instr).getResult(), f);
                break;
            }

            case BOX_BOOLEAN: {
                RubyBoolean f = context.runtime.newBoolean(getBooleanArg(booleans, ((BoxBooleanInstr) instr).getValue()));
                setResult(temp, currDynScope, ((BoxInstr)instr).getResult(), f);
                break;
            }

            case UNBOX_FLOAT: {
                UnboxInstr ui = (UnboxInstr)instr;
                Object val = retrieveOp(ui.getValue(), context, self, currDynScope, currScope, temp);
                if (val instanceof RubyFloat) {
                    floats[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFloat)val).getValue();
                } else {
                    floats[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFixnum)val).getDoubleValue();
                }
                break;
            }

            case UNBOX_FIXNUM: {
                UnboxInstr ui = (UnboxInstr)instr;
                Object val = retrieveOp(ui.getValue(), context, self, currDynScope, currScope, temp);
                if (val instanceof RubyFloat) {
                    fixnums[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFloat)val).getLongValue();
                } else {
                    fixnums[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFixnum)val).getLongValue();
                }
                break;
            }

            case LOAD_FRAME_CLOSURE:
                setResult(temp, currDynScope, instr, context.getFrameBlock());
                return;

            // ---------- All the rest ---------
            default:
                result = instr.interpret(context, currScope, currDynScope, self, temp);
                setResult(temp, currDynScope, instr, result);
                break;
        }
    }

    /*
     * If you put this code into the method above it will hard crash some production builds of C2 in Java 8. We aren't
     * sure exactly which builds, but it seems to appear more often in Linux builds than Mac. - Chris Seaton
     */
    protected static void extractToMethodToAvoidC2Crash(Instr instr, Throwable t) {
        if (!(t instanceof Unrescuable) && !instr.canRaiseException()) {
            System.err.println("BUG: Got exception " + t + " but instr " + instr + " is not supposed to be raising exceptions!");
        }
    }

    protected static void setResult(Object[] temp, DynamicScope currDynScope, Variable resultVar, Object result) {
        if (resultVar instanceof TemporaryVariable) {
            // Unboxed Java primitives (float/double/int/long) don't come here because result is an Object
            // So, it is safe to use offset directly without any correction as long as IRScope uses
            // three different allocators (each with its own 'offset' counter)
            // * one for LOCAL, BOOLEAN, CURRENT_SCOPE, CURRENT_MODULE, CLOSURE tmpvars
            // * one for FIXNUM
            // * one for FLOAT
            temp[((TemporaryLocalVariable)resultVar).offset] = result;
        } else {
            LocalVariable lv = (LocalVariable)resultVar;
            currDynScope.setValue((IRubyObject) result, lv.getLocation(), lv.getScopeDepth());
        }
    }

    protected static void setResult(Object[] temp, DynamicScope currDynScope, Instr instr, Object result) {
        if (instr instanceof ResultInstr) {
            setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), result);
        }
    }

    protected static Object retrieveOp(Operand r, ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope, Object[] temp) {
        Object res;
        if (r instanceof Self) {
            return self;
        } else if (r instanceof TemporaryLocalVariable) {
            res = temp[((TemporaryLocalVariable)r).offset];
            return res == null ? context.nil : res;
        } else if (r instanceof LocalVariable) {
            LocalVariable lv = (LocalVariable)r;
            res = currDynScope.getValue(lv.getLocation(), lv.getScopeDepth());
            return res == null ? context.nil : res;
        } else {
            return r.retrieve(context, self, currScope, currDynScope, temp);
        }

    }

    private static double getFloatArg(double[] floats, Operand arg) {
        if (arg instanceof Float) {
            return ((Float)arg).value;
        } else if (arg instanceof UnboxedFloat) {
            return ((UnboxedFloat)arg).value;
        } else if (arg instanceof Fixnum) {
            return (double)((Fixnum)arg).value;
        } else if (arg instanceof UnboxedFixnum) {
            return (double)((UnboxedFixnum)arg).value;
        } else if (arg instanceof Bignum) {
            return ((Bignum)arg).value.doubleValue();
        } else if (arg instanceof TemporaryLocalVariable) {
            return floats[((TemporaryLocalVariable)arg).offset];
        } else {
            throw new RuntimeException("invalid float operand: " + arg);
        }
    }

    private static long getFixnumArg(long[] fixnums, Operand arg) {
        if (arg instanceof Float) {
            return (long)((Float)arg).value;
        } else if (arg instanceof UnboxedFixnum) {
            return ((UnboxedFixnum)arg).value;
        } else if (arg instanceof Fixnum) {
            return ((Fixnum)arg).value;
        } else if (arg instanceof Bignum) {
            return ((Bignum)arg).value.longValue();
        } else if (arg instanceof TemporaryLocalVariable) {
            return fixnums[((TemporaryLocalVariable)arg).offset];
        } else {
            throw new RuntimeException("invalid fixnum operand: " + arg);
        }
    }

    private static boolean getBooleanArg(boolean[] booleans, Operand arg) {
        if (arg instanceof UnboxedBoolean) {
            return ((UnboxedBoolean)arg).isTrue();
        } else if (arg instanceof TemporaryLocalVariable) {
            return booleans[((TemporaryLocalVariable)arg).offset];
        } else {
            throw new RuntimeException("invalid fixnum operand: " + arg);
        }
    }

    private static void setFloatVar(double[] floats, TemporaryLocalVariable var, double val) {
        floats[var.offset] = val;
    }

    private static void setFixnumVar(long[] fixnums, TemporaryLocalVariable var, long val) {
        fixnums[var.offset] = val;
    }

    private static void setBooleanVar(boolean[] booleans, TemporaryLocalVariable var, boolean val) {
        booleans[var.offset] = val;
    }
}
