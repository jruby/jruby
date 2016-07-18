package org.jruby.ir.interpreter;

import java.util.HashMap;
import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.ir.OpClass;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;

/**
 * An attempt at a minimal subset of instrs for small simple methods.
 */
public class SimpleMethodInterpreterEngine extends InterpreterEngine {
    public static Map<Operation, Boolean> OPERATIONS = new HashMap() {{
        put(Operation.RECV_PRE_REQD_ARG, true);
        put(Operation.CHECK_ARITY, true);
        put(Operation.RETURN, true);
        put(Operation.LINE_NUM, true);
        put(Operation.RECV_SELF, true);
        put(Operation.RECV_JRUBY_EXC, true);
        put(Operation.THROW, true);
        put(Operation.PUSH_METHOD_FRAME, true);
        put(Operation.POP_METHOD_FRAME, true);
        put(Operation.PUSH_METHOD_BINDING, true);
        put(Operation.POP_BINDING, true);
        put(Operation.NORESULT_CALL_1O, true);
        put(Operation.SEARCH_CONST, true);
        put(Operation.INHERITANCE_SEARCH_CONST, true);
        put(Operation.NORESULT_CALL, true);
        put(Operation.CALL, true);
        put(Operation.CALL_0O, true);
        put(Operation.CALL_1O, true);
        put(Operation.B_FALSE, true);
        put(Operation.B_NIL, true);
        put(Operation.BNE, true);
        put(Operation.LOAD_IMPLICIT_CLOSURE, true);
        put(Operation.COPY, true);
        put(Operation.JUMP, true);
        put(Operation.RUNTIME_HELPER, true);
        put(Operation.GET_FIELD, true);
        put(Operation.PUT_FIELD, true);
        put(Operation.BUILD_COMPOUND_STRING, true);
        put(Operation.CONST_MISSING, true);
    }};
    @Override
    public IRubyObject interpret(ThreadContext context, Compilable compilable, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, String name, Block blockArg) {
        // Just use any interp since it will contain no recvs
        return interpret(context, compilable, block, self, interpreterContext, name, (IRubyObject) null, blockArg);
    }

    @Override
    public IRubyObject interpret(ThreadContext context, Compilable compilable, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, String name, IRubyObject arg1, Block blockArg) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp = interpreterContext.allocateTemporaryVariables();
        int n = instrs.length;
        int ipc = 0;
        Object exception = null;
        Block.Type blockType = block == null ? null : block.type;

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        // Init profiling this scope
        boolean debug = IRRuntimeHelpers.isDebug();

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            ipc++;

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {}", instr);
                Interpreter.interpInstrsCount++;
            }

            try {
                switch (operation) {
                    case RECV_PRE_REQD_ARG:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), arg1);
                        break;
                    case CHECK_ARITY:
                        break;
                    case RETURN:
                        return (IRubyObject) retrieveOp(((ReturnBase) instr).getReturnValue(), context, self, currDynScope, currScope, temp);
                    case LINE_NUM:
                        context.setLine(((LineNumberInstr) instr).lineNumber);
                        break;
                    case RECV_SELF:
                        break;
                    case RECV_JRUBY_EXC:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), exception);
                        break;
                    case THROW:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case PUSH_METHOD_FRAME:
                        context.preMethodFrameOnly(compilable.getImplementationClass().getMethodLocation(), name, self, blockArg);
                        // Only the top-level script scope has PRIVATE visibility.
                        // This is already handled as part of Interpreter.execute above.
                        // Everything else is PUBLIC by default.
                        context.setCurrentVisibility(Visibility.PUBLIC);
                        break;
                    case POP_METHOD_FRAME:
                        context.popFrame();
                        break;
                    case PUSH_METHOD_BINDING:
                        // IMPORTANT: Preserve this update of currDynScope.
                        // This affects execution of all instructions in this scope
                        // which will now use the updated value of currDynScope.
                        currDynScope = interpreterContext.newDynamicScope(context);
                        context.pushScope(currDynScope);
                        break;
                    case POP_BINDING:
                        context.popScope();
                        break;
                    case NORESULT_CALL_1O: {
                        OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        call.getCallSite().call(context, self, r, o);
                        break;
                    }
                    case SEARCH_CONST: {
                        SearchConstInstr sci = (SearchConstInstr) instr;
                        ConstantCache cache = sci.getConstantCache();
                        Object result;
                        if (!ConstantCache.isCached(cache)) {
                            result = sci.cache(context, currScope, currDynScope, self, temp);
                        } else {
                            result = cache.value;
                        }
                        setResult(temp, currDynScope, sci.getResult(), result);
                        break;
                    }
                    case INHERITANCE_SEARCH_CONST:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case NORESULT_CALL:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case CALL:
                        setResult(temp, currDynScope, instr,
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case CALL_0O: {
                        ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
                        IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r));
                        break;
                    }
                    case CALL_1O: {
                        OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r, o));
                        break;
                    }
                    case B_NIL:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_FALSE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_TRUE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case BNE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case LOAD_IMPLICIT_CLOSURE:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), blockArg);
                        break;
                    case COPY: // NO INTERP
                        setResult(temp, currDynScope, ((CopyInstr) instr).getResult(),
                                retrieveOp(((CopyInstr) instr).getSource(), context, self, currDynScope, currScope, temp));
                        break;
                    case JUMP: // NO INTERP
                        ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC();
                        break;
                    case RUNTIME_HELPER: { // NO INTERP
                        RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                        setResult(temp, currDynScope, rhc.getResult(),
                                rhc.callHelper(context, currScope, currDynScope, self, temp, blockType));
                        break;
                    }

                    case BUILD_COMPOUND_STRING: case CONST_MISSING:
                    default:
                        if (instr.getOperation().opClass == OpClass.BRANCH_OP) {
                            ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        } else {
                            Object result = instr.interpret(context, currScope, currDynScope, self, temp);

                            if (instr instanceof ResultInstr) {
                                setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), result);
                            }
                        }
                }
            } catch (Throwable t) {
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
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }

    @Override
    public IRubyObject interpret(ThreadContext context, Compilable compilable, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, String name, IRubyObject arg1, IRubyObject arg2, Block blockArg) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp = interpreterContext.allocateTemporaryVariables();
        int n = instrs.length;
        int ipc = 0;
        Object exception = null;
        Block.Type blockType = block == null ? null : block.type;

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        // Init profiling this scope
        boolean debug = IRRuntimeHelpers.isDebug();

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            ipc++;

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {}", instr);
                Interpreter.interpInstrsCount++;
            }

            try {
                switch (operation) {
                    case RECV_PRE_REQD_ARG:{
                        int argIndex = ((ReceivePreReqdArgInstr) instr).getArgIndex();
                        IRubyObject arg = null;
                        switch (argIndex) {
                            case 0: arg = arg1; break;
                            case 1: arg = arg2; break;
                        }
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), arg);
                        break;
                    }
                    case CHECK_ARITY:
                        break;
                    case RETURN:
                        return (IRubyObject) retrieveOp(((ReturnBase) instr).getReturnValue(), context, self, currDynScope, currScope, temp);
                    case LINE_NUM:
                        context.setLine(((LineNumberInstr) instr).lineNumber);
                        break;
                    case RECV_SELF:
                        break;
                    case RECV_JRUBY_EXC:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), exception);
                        break;
                    case THROW:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case PUSH_METHOD_FRAME:
                        context.preMethodFrameOnly(compilable.getImplementationClass().getMethodLocation(), name, self, blockArg);
                        // Only the top-level script scope has PRIVATE visibility.
                        // This is already handled as part of Interpreter.execute above.
                        // Everything else is PUBLIC by default.
                        context.setCurrentVisibility(Visibility.PUBLIC);
                        break;
                    case POP_METHOD_FRAME:
                        context.popFrame();
                        break;
                    case PUSH_METHOD_BINDING:
                        // IMPORTANT: Preserve this update of currDynScope.
                        // This affects execution of all instructions in this scope
                        // which will now use the updated value of currDynScope.
                        currDynScope = interpreterContext.newDynamicScope(context);
                        context.pushScope(currDynScope);
                        break;
                    case POP_BINDING:
                        context.popScope();
                        break;
                    case NORESULT_CALL_1O: {
                        OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        call.getCallSite().call(context, self, r, o);
                        break;
                    }
                    case SEARCH_CONST: {
                        SearchConstInstr sci = (SearchConstInstr) instr;
                        ConstantCache cache = sci.getConstantCache();
                        Object result;
                        if (!ConstantCache.isCached(cache)) {
                            result = sci.cache(context, currScope, currDynScope, self, temp);
                        } else {
                            result = cache.value;
                        }
                        setResult(temp, currDynScope, sci.getResult(), result);
                        break;
                    }
                    case INHERITANCE_SEARCH_CONST:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case NORESULT_CALL:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case CALL:
                        setResult(temp, currDynScope, instr,
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case CALL_0O: {
                        ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
                        IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r));
                        break;
                    }
                    case CALL_1O: {
                        OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r, o));
                        break;
                    }
                    case B_NIL:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_FALSE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_TRUE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case BNE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case LOAD_IMPLICIT_CLOSURE:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), blockArg);
                        break;
                    case COPY: // NO INTERP
                        setResult(temp, currDynScope, ((CopyInstr) instr).getResult(),
                                retrieveOp(((CopyInstr) instr).getSource(), context, self, currDynScope, currScope, temp));
                        break;
                    case JUMP: // NO INTERP
                        ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC();
                        break;
                    case RUNTIME_HELPER: { // NO INTERP
                        RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                        setResult(temp, currDynScope, rhc.getResult(),
                                rhc.callHelper(context, currScope, currDynScope, self, temp, blockType));
                        break;
                    }

                    case BUILD_COMPOUND_STRING: case CONST_MISSING:
                    default:
                        if (instr.getOperation().opClass == OpClass.BRANCH_OP) {
                            ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        } else {
                            Object result = instr.interpret(context, currScope, currDynScope, self, temp);

                            if (instr instanceof ResultInstr) {
                                setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), result);
                            }
                        }
                }
            } catch (Throwable t) {
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
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }

    @Override
    public IRubyObject interpret(ThreadContext context, Compilable compilable, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block blockArg) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp = interpreterContext.allocateTemporaryVariables();
        int n = instrs.length;
        int ipc = 0;
        Object exception = null;
        Block.Type blockType = block == null ? null : block.type;

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        // Init profiling this scope
        boolean debug = IRRuntimeHelpers.isDebug();

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            ipc++;

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {}", instr);
                Interpreter.interpInstrsCount++;
            }

            try {
                switch (operation) {
                    case RECV_PRE_REQD_ARG:{
                        int argIndex = ((ReceivePreReqdArgInstr) instr).getArgIndex();
                        IRubyObject arg = null;
                        switch (argIndex) {
                            case 0: arg = arg1; break;
                            case 1: arg = arg2; break;
                            case 2: arg = arg3; break;
                        }
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), arg);
                        break;
                    }
                    case CHECK_ARITY:
                        break;
                    case RETURN:
                        return (IRubyObject) retrieveOp(((ReturnBase) instr).getReturnValue(), context, self, currDynScope, currScope, temp);
                    case LINE_NUM:
                        context.setLine(((LineNumberInstr) instr).lineNumber);
                        break;
                    case RECV_SELF:
                        break;
                    case RECV_JRUBY_EXC:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), exception);
                        break;
                    case THROW:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case PUSH_METHOD_FRAME:
                        context.preMethodFrameOnly(compilable.getImplementationClass().getMethodLocation(), name, self, blockArg);
                        // Only the top-level script scope has PRIVATE visibility.
                        // This is already handled as part of Interpreter.execute above.
                        // Everything else is PUBLIC by default.
                        context.setCurrentVisibility(Visibility.PUBLIC);
                        break;
                    case POP_METHOD_FRAME:
                        context.popFrame();
                        break;
                    case PUSH_METHOD_BINDING:
                        // IMPORTANT: Preserve this update of currDynScope.
                        // This affects execution of all instructions in this scope
                        // which will now use the updated value of currDynScope.
                        currDynScope = interpreterContext.newDynamicScope(context);
                        context.pushScope(currDynScope);
                        break;
                    case POP_BINDING:
                        context.popScope();
                        break;
                    case NORESULT_CALL_1O: {
                        OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        call.getCallSite().call(context, self, r, o);
                        break;
                    }
                    case SEARCH_CONST: {
                        SearchConstInstr sci = (SearchConstInstr) instr;
                        ConstantCache cache = sci.getConstantCache();
                        Object result;
                        if (!ConstantCache.isCached(cache)) {
                            result = sci.cache(context, currScope, currDynScope, self, temp);
                        } else {
                            result = cache.value;
                        }
                        setResult(temp, currDynScope, sci.getResult(), result);
                        break;
                    }
                    case INHERITANCE_SEARCH_CONST:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case NORESULT_CALL:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case CALL:
                        setResult(temp, currDynScope, instr,
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case CALL_0O: {
                        ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
                        IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r));
                        break;
                    }
                    case CALL_1O: {
                        OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r, o));
                        break;
                    }
                    case B_NIL:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_FALSE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_TRUE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case BNE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case LOAD_IMPLICIT_CLOSURE:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), blockArg);
                        break;
                    case COPY: // NO INTERP
                        setResult(temp, currDynScope, ((CopyInstr) instr).getResult(),
                                retrieveOp(((CopyInstr) instr).getSource(), context, self, currDynScope, currScope, temp));
                        break;
                    case JUMP: // NO INTERP
                        ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC();
                        break;
                    case RUNTIME_HELPER: { // NO INTERP
                        RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                        setResult(temp, currDynScope, rhc.getResult(),
                                rhc.callHelper(context, currScope, currDynScope, self, temp, blockType));
                        break;
                    }

                    case BUILD_COMPOUND_STRING: case CONST_MISSING:
                    default:
                        if (instr.getOperation().opClass == OpClass.BRANCH_OP) {
                            ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        } else {
                            Object result = instr.interpret(context, currScope, currDynScope, self, temp);

                            if (instr instanceof ResultInstr) {
                                setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), result);
                            }
                        }
                }
            } catch (Throwable t) {
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
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }

    @Override
    public IRubyObject interpret(ThreadContext context, Compilable compilable, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, Block blockArg) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp = interpreterContext.allocateTemporaryVariables();
        int n = instrs.length;
        int ipc = 0;
        Object exception = null;
        Block.Type blockType = block == null ? null : block.type;

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        // Init profiling this scope
        boolean debug = IRRuntimeHelpers.isDebug();

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            ipc++;

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {}", instr);
                Interpreter.interpInstrsCount++;
            }

            try {
                switch (operation) {
                    case RECV_PRE_REQD_ARG: {
                        int argIndex = ((ReceivePreReqdArgInstr) instr).getArgIndex();
                        IRubyObject arg = null;
                        switch (argIndex) {
                            case 0: arg = arg1; break;
                            case 1: arg = arg2; break;
                            case 2: arg = arg3; break;
                            case 3: arg = arg4; break;
                        }
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), arg);
                        break;
                    }
                    case CHECK_ARITY:
                        break;
                    case RETURN:
                        return (IRubyObject) retrieveOp(((ReturnBase) instr).getReturnValue(), context, self, currDynScope, currScope, temp);
                    case LINE_NUM:
                        context.setLine(((LineNumberInstr) instr).lineNumber);
                        break;
                    case RECV_SELF:
                        break;
                    case RECV_JRUBY_EXC:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), exception);
                        break;
                    case THROW:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case PUSH_METHOD_FRAME:
                        context.preMethodFrameOnly(compilable.getImplementationClass().getMethodLocation(), name, self, blockArg);
                        // Only the top-level script scope has PRIVATE visibility.
                        // This is already handled as part of Interpreter.execute above.
                        // Everything else is PUBLIC by default.
                        context.setCurrentVisibility(Visibility.PUBLIC);
                        break;
                    case POP_METHOD_FRAME:
                        context.popFrame();
                        break;
                    case PUSH_METHOD_BINDING:
                        // IMPORTANT: Preserve this update of currDynScope.
                        // This affects execution of all instructions in this scope
                        // which will now use the updated value of currDynScope.
                        currDynScope = interpreterContext.newDynamicScope(context);
                        context.pushScope(currDynScope);
                        break;
                    case POP_BINDING:
                        context.popScope();
                        break;
                    case NORESULT_CALL_1O: {
                        OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        call.getCallSite().call(context, self, r, o);
                        break;
                    }
                    case SEARCH_CONST: {
                        SearchConstInstr sci = (SearchConstInstr) instr;
                        ConstantCache cache = sci.getConstantCache();
                        Object result;
                        if (!ConstantCache.isCached(cache)) {
                            result = sci.cache(context, currScope, currDynScope, self, temp);
                        } else {
                            result = cache.value;
                        }
                        setResult(temp, currDynScope, sci.getResult(), result);
                        break;
                    }
                    case INHERITANCE_SEARCH_CONST:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case NORESULT_CALL:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case CALL:
                        setResult(temp, currDynScope, instr,
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case CALL_0O: {
                        ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
                        IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r));
                        break;
                    }
                    case CALL_1O: {
                        OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r, o));
                        break;
                    }
                    case B_NIL:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_FALSE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_TRUE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case BNE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case LOAD_IMPLICIT_CLOSURE:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), blockArg);
                        break;
                    case COPY: // NO INTERP
                        setResult(temp, currDynScope, ((CopyInstr) instr).getResult(),
                                retrieveOp(((CopyInstr) instr).getSource(), context, self, currDynScope, currScope, temp));
                        break;
                    case JUMP: // NO INTERP
                        ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC();
                        break;
                    case RUNTIME_HELPER: { // NO INTERP
                        RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                        setResult(temp, currDynScope, rhc.getResult(),
                                rhc.callHelper(context, currScope, currDynScope, self, temp, blockType));
                        break;
                    }

                    case BUILD_COMPOUND_STRING: case CONST_MISSING:
                    default:
                        if (instr.getOperation().opClass == OpClass.BRANCH_OP) {
                            ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        } else {
                            Object result = instr.interpret(context, currScope, currDynScope, self, temp);

                            if (instr instanceof ResultInstr) {
                                setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), result);
                            }
                        }
                }
            } catch (Throwable t) {
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
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }

    @Override
    public IRubyObject interpret(ThreadContext context, Compilable compilable, Block block, IRubyObject self,
                                 InterpreterContext interpreterContext, String name, IRubyObject[] args, Block blockArg) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp = interpreterContext.allocateTemporaryVariables();
        int n = instrs.length;
        int ipc = 0;
        Object exception = null;
        Block.Type blockType = block == null ? null : block.type;

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();

        // Init profiling this scope
        boolean debug = IRRuntimeHelpers.isDebug();

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            ipc++;

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {}", instr);
                Interpreter.interpInstrsCount++;
            }

            try {
                switch (operation) {
                    case RECV_PRE_REQD_ARG:
                        int argIndex = ((ReceivePreReqdArgInstr)instr).getArgIndex();
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), IRRuntimeHelpers.getPreArgSafe(context, args, argIndex));
                        break;
                    case CHECK_ARITY:
                        break;
                    case RETURN:
                        return (IRubyObject) retrieveOp(((ReturnBase) instr).getReturnValue(), context, self, currDynScope, currScope, temp);
                    case LINE_NUM:
                        context.setLine(((LineNumberInstr) instr).lineNumber);
                        break;
                    case RECV_SELF:
                        break;
                    case RECV_JRUBY_EXC:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), exception);
                        break;
                    case THROW:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case PUSH_METHOD_FRAME:
                        context.preMethodFrameOnly(compilable.getImplementationClass().getMethodLocation(), name, self, blockArg);
                        // Only the top-level script scope has PRIVATE visibility.
                        // This is already handled as part of Interpreter.execute above.
                        // Everything else is PUBLIC by default.
                        context.setCurrentVisibility(Visibility.PUBLIC);
                        break;
                    case POP_METHOD_FRAME:
                        context.popFrame();
                        break;
                    case PUSH_METHOD_BINDING:
                        // IMPORTANT: Preserve this update of currDynScope.
                        // This affects execution of all instructions in this scope
                        // which will now use the updated value of currDynScope.
                        currDynScope = interpreterContext.newDynamicScope(context);
                        context.pushScope(currDynScope);
                        break;
                    case POP_BINDING:
                        context.popScope();
                        break;
                    case NORESULT_CALL_1O: {
                        OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        call.getCallSite().call(context, self, r, o);
                        break;
                    }
                    case SEARCH_CONST: {
                        SearchConstInstr sci = (SearchConstInstr) instr;
                        ConstantCache cache = sci.getConstantCache();
                        Object result;
                        if (!ConstantCache.isCached(cache)) {
                            result = sci.cache(context, currScope, currDynScope, self, temp);
                        } else {
                            result = cache.value;
                        }
                        setResult(temp, currDynScope, sci.getResult(), result);
                        break;
                    }
                    case INHERITANCE_SEARCH_CONST:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case NORESULT_CALL:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case CALL:
                        setResult(temp, currDynScope, instr,
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case CALL_0O: {
                        ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
                        IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r));
                        break;
                    }
                    case CALL_1O: {
                        OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r, o));
                        break;
                    }
                    case B_NIL:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_FALSE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case B_TRUE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case BNE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case LOAD_IMPLICIT_CLOSURE:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), blockArg);
                        break;
                    case COPY: // NO INTERP
                        setResult(temp, currDynScope, ((CopyInstr) instr).getResult(),
                                retrieveOp(((CopyInstr) instr).getSource(), context, self, currDynScope, currScope, temp));
                        break;
                    case JUMP: // NO INTERP
                        ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC();
                        break;
                    case RUNTIME_HELPER: { // NO INTERP
                        RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
                        setResult(temp, currDynScope, rhc.getResult(),
                                rhc.callHelper(context, currScope, currDynScope, self, temp, blockType));
                        break;
                    }

                    case BUILD_COMPOUND_STRING: case CONST_MISSING:
                    default:
                        if (instr.getOperation().opClass == OpClass.BRANCH_OP) {
                            ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        } else {
                            Object result = instr.interpret(context, currScope, currDynScope, self, temp);

                            if (instr instanceof ResultInstr) {
                                setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), result);
                            }
                        }
                }
            } catch (Throwable t) {
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
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }
}
