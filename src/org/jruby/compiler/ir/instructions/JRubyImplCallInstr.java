package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyMatchData;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.jruby.BlockGivenInstr;
import org.jruby.compiler.ir.instructions.jruby.CheckArityInstr;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

public class JRubyImplCallInstr extends CallInstr {
    // SSS FIXME: This is a rather arbitrary set of methods 
    //
    // 1. Most of these are there to support defined?  I just did a dumb translation of the
    //    bytecode instrs from the existing AST compiler.  This code needs cleanup!  Most of
    //    the defined? inlined IR instructions in IRBuilder should be cleanly tucked away into a
    //    defined? support runtime library with a relatively clean API.
    //
    // 2. Some of the other methods are a little arbitrary as well and come from the first pass
    //    of trying to mimic behavior of the previous AST compiler.  This set of code can be
    //    cleaned up in a later pass.
    public enum JRubyImplementationMethod {
       // SSS FIXME: Note that compiler/impl/BaseBodyCompiler is using op_match2 for match() and and op_match for match2,
       // and we are replicating it here ... Is this a bug there?
       MATCH("op_match2"),
       MATCH2("op_match"),
       MATCH3("match3"),
       RT_IS_GLOBAL_DEFINED("runtime_isGlobalDefined"),
       RT_GET_OBJECT("runtime_getObject"),
       RT_GET_BACKREF("runtime_getBackref"),
       RTH_GET_DEFINED_CONSTANT_OR_BOUND_METHOD("getDefinedConstantOrBoundMethod"),
       BLOCK_GIVEN("block_isGiven"), // SSS FIXME: Should this be a Ruby internals call rather than a JRUBY internals call?
       SELF_METACLASS("self_metaClass"), // SSS FIXME: Should this be a Ruby internals call rather than a JRUBY internals call?
       SELF_HAS_INSTANCE_VARIABLE("self_hasInstanceVariable"), // SSS FIXME: Should this be a Ruby internals call rather than a JRUBY internals call?
       SELF_IS_METHOD_BOUND("self_isMethodBound"), // SSS FIXME: Should this be a Ruby internals call rather than a JRUBY internals call?
       TC_SAVE_ERR_INFO("threadContext_saveErrInfo"),
       TC_RESTORE_ERR_INFO("threadContext_restoreErrInfo"),
       TC_GET_CONSTANT_DEFINED("threadContext_getConstantDefined"),
       TC_GET_CURRENT_MODULE("threadContext_getCurrentModule"),
       BACKREF_IS_RUBY_MATCH_DATA("backref_isRubyMatchData"),
       METHOD_PUBLIC_ACCESSIBLE("methodIsPublicAccessible"),
       CLASS_VAR_DEFINED("isClassVarDefined"),
       FRAME_SUPER_METHOD_BOUND("frame_superMethodBound"),
       SET_WITHIN_DEFINED("setWithinDefined"),
       CHECK_ARITY("checkArity"),
       RAISE_ARGUMENT_ERROR("raiseArgumentError");

       public MethAddr methAddr;
       JRubyImplementationMethod(String methodName) {
           this.methAddr = new MethAddr(methodName);
       }

       public MethAddr getMethAddr() { 
           return this.methAddr; 
       }
    }
    
    public static Instr createJRubyImplementationMethod(Variable result, 
            JRubyImplementationMethod methAddr, Operand receiver, Operand[] args) {
        switch (methAddr) {
            case BLOCK_GIVEN:
                return new BlockGivenInstr(result);
            case CHECK_ARITY:
                return new CheckArityInstr(result, receiver, args);
            default:
                return new JRubyImplCallInstr(result, methAddr, receiver, args);
        }
    }

    JRubyImplementationMethod implMethod;

    public JRubyImplCallInstr(Variable result, JRubyImplementationMethod methAddr, Operand receiver, Operand[] args) {
        super(Operation.JRUBY_IMPL, result, methAddr.getMethAddr(), receiver, args, null);
        this.implMethod = methAddr;
    }

    public JRubyImplCallInstr(Variable result, JRubyImplementationMethod methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(result, methAddr.getMethAddr(), receiver, args, closure);
        this.implMethod = methAddr;
    }

    @Override
    public boolean isStaticCallTarget() {
        return true;
    }

    @Override
    public Operand[] getOperands() {
        int       offset  = (receiver != null) ? 2 : 1;
        Operand[] allArgs = new Operand[arguments.length + offset];

        allArgs[0] = methAddr;
        if (receiver != null) allArgs[1] = receiver;
        for (int i = 0; i < arguments.length; i++) {
            assert arguments[i] != null : "ARG " + i + " is null";
            allArgs[i + offset] = arguments[i];
        }

        return allArgs;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        if (receiver != null) receiver = receiver.getSimplifiedOperand(valueMap);
        methAddr = (MethAddr)methAddr.getSimplifiedOperand(valueMap);
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].getSimplifiedOperand(valueMap);
        }
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        Operand receiver = getReceiver();

        return new JRubyImplCallInstr(result == null ? null : ii.getRenamedVariable(result), this.implMethod,
                receiver == null ? null : receiver.cloneForInlining(ii), cloneCallArgs(ii),
                closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Ruby runtime = context.getRuntime();        
        String name;
        Object receiver;
        Object rVal = null;

        switch (this.implMethod) {
            // SSS FIXME: Note that compiler/impl/BaseBodyCompiler is using op_match2 for match() and and op_match for match2,
            // and we are replicating it here ... Is this a bug there?
            case MATCH:
                receiver = getReceiver().retrieve(interp, context, self);
                rVal = ((RubyRegexp) receiver).op_match2(context);
                break;
            case MATCH2:
                receiver = getReceiver().retrieve(interp, context, self);
                rVal = ((RubyRegexp) receiver).op_match(context, (IRubyObject) getCallArgs()[0].retrieve(interp, context, self));
                break;
            case MATCH3: {// ENEBO: Only for rubystring?
                receiver = getReceiver().retrieve(interp, context, self);
                IRubyObject value = (IRubyObject) getCallArgs()[0].retrieve(interp, context, self);
                        
                if (value instanceof RubyString) {
                    rVal = ((RubyRegexp) receiver).op_match(context, value);
                } else {
                    rVal = value.callMethod(context, "=~", (IRubyObject) receiver);
                }
                break;
            }
            case SET_WITHIN_DEFINED:
                context.setWithinDefined(((BooleanLiteral)getCallArgs()[0]).isTrue());
                break;
            case RTH_GET_DEFINED_CONSTANT_OR_BOUND_METHOD:
            {
                IRubyObject v = (IRubyObject)getCallArgs()[0].retrieve(interp, context, self);
                name = ((StringLiteral)getCallArgs()[1])._str_value;
                ByteList definedType = RuntimeHelpers.getDefinedConstantOrBoundMethod(v, name);
                rVal = (definedType == null ? Nil.NIL : (new StringLiteral(definedType))).retrieve(interp, context, self);
                break;
            }
            case RT_IS_GLOBAL_DEFINED:
                //name = getCallArgs()[0].retrieve(interp).toString();
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                rVal = runtime.newBoolean(runtime.getGlobalVariables().isDefined(name));
                break;
            case RT_GET_OBJECT:
                rVal = runtime.getObject();
                break;
            case RT_GET_BACKREF:
                // SSS: FIXME: Or use this directly? "context.getCurrentScope().getBackRef(rt)" What is the diff??
                rVal = RuntimeHelpers.getBackref(runtime, context);
                break;
            case SELF_METACLASS:
                rVal = ((IRubyObject)getReceiver().retrieve(interp, context, self)).getMetaClass();
                break;
            case RAISE_ARGUMENT_ERROR:
            {
                Operand[] args = getCallArgs();
                int required = ((Fixnum)args[0]).value.intValue();
                int opt      = ((Fixnum)args[1]).value.intValue();
                int rest     = ((Fixnum)args[2]).value.intValue();
                int numArgs  = ((Fixnum)args[3]).value.intValue();
                Arity.raiseArgumentError(context.getRuntime(), numArgs, required, required+opt);
                break;
            }
            case SELF_HAS_INSTANCE_VARIABLE:
            {
                receiver = getReceiver().retrieve(interp, context, self);
                //name = getCallArgs()[0].retrieve(interp).toString();
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                rVal = runtime.newBoolean(((IRubyObject)receiver).getInstanceVariables().hasInstanceVariable(name));
                break;
            }
            case SELF_IS_METHOD_BOUND:
            {
                receiver = getReceiver().retrieve(interp, context, self);
                boolean bound = ((IRubyObject)receiver).getMetaClass().isMethodBound(((StringLiteral)getCallArgs()[0])._str_value, false); 
                rVal = runtime.newBoolean(bound);
                break;
            }
            case TC_SAVE_ERR_INFO:
                rVal = context.getErrorInfo();
                break;
            case TC_RESTORE_ERR_INFO:
                context.setErrorInfo((IRubyObject)getCallArgs()[0].retrieve(interp, context, self));
                break;
            case TC_GET_CONSTANT_DEFINED:
                //name = getCallArgs()[0].retrieve(interp).toString();
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                rVal = runtime.newBoolean(context.getConstantDefined(name));
                break;
            case TC_GET_CURRENT_MODULE:
                rVal = context.getCurrentScope().getStaticScope().getModule();
                break;
            case BACKREF_IS_RUBY_MATCH_DATA:
                // bRef = getBackref()
                // flag = bRef instanceof RubyMatchData
                // SSS: FIXME: Or use this directly? "context.getCurrentScope().getBackRef(rt)" What is the diff??
                IRubyObject bRef = RuntimeHelpers.getBackref(runtime, context);
                rVal = runtime.newBoolean(RubyMatchData.class.isInstance(bRef));
                break;
            case METHOD_PUBLIC_ACCESSIBLE:
            {
                /* ------------------------------------------------------------
                 * mc = r.metaClass
                 * v  = mc.getVisibility(methodName)
                 * v.isPrivate? || (v.isProtected? && receiver/self? instanceof mc.getRealClass)
                 * ------------------------------------------------------------ */
                IRubyObject r   = (IRubyObject)getReceiver().retrieve(interp, context, self);
                RubyClass   mc  = r.getMetaClass();
                String      arg = ((StringLiteral)getCallArgs()[0])._str_value;
                Visibility  v   = mc.searchMethod(arg).getVisibility();
                rVal = runtime.newBoolean((v != null) && !v.isPrivate() && !(v.isProtected() && mc.getRealClass().isInstance(r)));
                break;
            }
            case CLASS_VAR_DEFINED:
            {
                // cm.classVarDefined(name) || (cm.isSingleton && !(cm.attached instanceof RubyModule) && cm.attached.classVarDefined(name))
                boolean flag;
                RubyModule cm = (RubyModule)getReceiver().retrieve(interp, context, self);
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                flag = cm.isClassVarDefined(name);
                if (!flag) {
                    if (cm.isSingleton()) {
                        IRubyObject ao = ((MetaClass)cm).getAttached();
                        if (ao instanceof RubyModule) flag = ((RubyModule)ao).isClassVarDefined(name);
                    }
                }
                rVal = runtime.newBoolean(flag);
                break;
            }
            case FRAME_SUPER_METHOD_BOUND:
            {
                receiver = getReceiver().retrieve(interp, context, self);
                boolean flag = false;
                String        fn = context.getFrameName();
                if (fn != null) {
                    RubyModule fc = context.getFrameKlazz();
                    if (fc != null) {
                        flag = RuntimeHelpers.findImplementerIfNecessary(((IRubyObject)receiver).getMetaClass(), fc).getSuperClass().isMethodBound(fn, false);
                    }
                }
                rVal = runtime.newBoolean(flag);
                break;
            }
        }

        // Store the result
        if (rVal != null) getResult().store(interp, context, self, rVal);

        return null;
    }
}
