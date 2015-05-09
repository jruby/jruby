package org.jruby.ir.interpreter;

import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings;
import org.jruby.ir.IRScope;
import org.jruby.ir.OpClass;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.TraceInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryFixnumVariable;
import org.jruby.ir.operands.TemporaryFloatVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.opto.ConstantCache;

/**
 * Created by enebo on 2/5/15.
 */
public class BodyInterpreterEngine extends InterpreterEngine {
    @Override
    public IRubyObject interpret(ThreadContext context, IRubyObject self, InterpreterContext interpreterContext, RubyModule implClass, String name, Block block, Block.Type blockType) {
        Instr[] instrs = interpreterContext.getInstructions();
        Object[] temp = interpreterContext.allocateTemporaryVariables();
        int n = instrs.length;
        int ipc = 0;
        Object exception = null;

        StaticScope currScope = interpreterContext.getStaticScope();
        DynamicScope currDynScope = context.getCurrentScope();
        IRScope scope = currScope.getIRScope();

        // Init profiling this scope
        boolean debug = IRRuntimeHelpers.isDebug();

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];

            Operation operation = instr.getOperation();
            if (debug) {
                Interpreter.LOG.info("I: {" + ipc + "} ", instr);
                Interpreter.interpInstrsCount++;
            }

            ipc++;

            try {
                switch (operation) {
                    case RETURN:
                        return (IRubyObject) retrieveOp(((ReturnBase) instr).getReturnValue(), context, self, currDynScope, currScope, temp);
                    case NONLOCAL_RETURN: {
                        NonlocalReturnInstr ri = (NonlocalReturnInstr)instr;
                        IRubyObject rv = (IRubyObject)retrieveOp(ri.getReturnValue(), context, self, currDynScope, currScope, temp);
                        return IRRuntimeHelpers.initiateNonLocalReturn(context, currDynScope, blockType, rv);
                    }
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
                    case PUSH_FRAME:
                        context.preMethodFrameOnly(implClass, name, self, block);
                        // Only the top-level script scope has PRIVATE visibility.
                        // This is already handled as part of Interpreter.execute above.
                        // Everything else is PUBLIC by default.
                        context.setCurrentVisibility(Visibility.PUBLIC);
                        break;
                    case POP_FRAME:
                        context.popFrame();
                        break;
                    case PUSH_BINDING:
                        // IMPORTANT: Preserve this update of currDynScope.
                        // This affects execution of all instructions in this scope
                        // which will now use the updated value of currDynScope.
                        currDynScope = interpreterContext.newDynamicScope(context);
                        context.pushScope(currDynScope);
                        break;
                    case POP_BINDING:
                        context.popScope();
                        break;
                    case LOAD_FRAME_CLOSURE:
                        setResult(temp, currDynScope, instr, context.getFrameBlock());
                        break;
                    case DEF_INST_METH:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case PUT_CONST:
                        instr.interpret(context, currScope, currDynScope, self, temp);
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
                    case PROCESS_MODULE_BODY:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case DEF_CLASS:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case INHERITANCE_SEARCH_CONST:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case DEF_MODULE:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(),
                                instr.interpret(context, currScope, currDynScope, self, temp));
                        break;
                    case CALL_1O: {
                        OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr) instr;
                        IRubyObject r = (IRubyObject) retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
                        IRubyObject o = (IRubyObject) call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
                        setResult(temp, currDynScope, call.getResult(), call.getCallSite().call(context, self, r, o));
                        break;
                    }
                    case BNE:
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        break;
                    case DEF_CLASS_METH:
                        instr.interpret(context, currScope, currDynScope, self, temp);
                        break;
                    case LOAD_IMPLICIT_CLOSURE:
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), block);
                        break;
                    case RECV_RUBY_EXC: // NO INTERP
                        setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), IRRuntimeHelpers.unwrapRubyException(exception));
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
                    case GET_FIELD: { // NO INTERP
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
                    default:
                        if (instr.getOperation().opClass == OpClass.BRANCH_OP) {
                            ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc);
                        } else {
                            Object result = instr.interpret(context, currScope, currDynScope, self, temp);
                            setResult(temp, currDynScope, instr, result);
                        }
                }
            } catch (Throwable t) {
                ipc = instr.getRPC();
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
        throw context.runtime.newRuntimeError("BUG: interpreter fell through to end unexpectedly");
    }

    @Override
    public IRubyObject interpret(ThreadContext context, IRubyObject self, InterpreterContext interpreterContext, RubyModule implClass, String name, IRubyObject[] args, Block block, Block.Type blockType) {
        return interpret(context, self, interpreterContext, implClass, name, block, blockType);
    }
}
