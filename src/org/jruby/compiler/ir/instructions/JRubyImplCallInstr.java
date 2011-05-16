package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyRegexp;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.MetaClass;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

public class JRubyImplCallInstr extends CallInstr {
    public enum JRubyImplementationMethod {
       // SSS FIXME: Note that compiler/impl/BaseBodyCompiler is using op_match2 for match() and and op_match for match2,
       // and we are replicating it here ... Is this a bug there?
       MATCH("op_match2"), 
       MATCH2("op_match"), 
       MATCH3("match3"),
       // SSS FIXME: This method (at least in the context of multiple assignment) is a little weird.
       // It calls regular to_ary on the object.  But, if it encounters a method_missing, the value
       // is inserted into an 1-element array!
       // try "a,b,c = 1" first; then define Fixnum.to_ary method and try it again.
       // Ex: http://gist.github.com/163551
       TO_ARY("to_ary"),
       UNDEF_METHOD("undefMethod"),
       BLOCK_GIVEN("block_isGiven"),
       RT_IS_GLOBAL_DEFINED("runtime_isGlobalDefined"),
       RT_GET_OBJECT("runtime_getObject"),
       RT_GET_BACKREF("runtime_getBackref"),
       RTH_GET_DEFINED_CONSTANT_OR_BOUND_METHOD("getDefinedConstantOrBoundMethod"),
       SELF_METACLASS("self_metaClass"),
       SELF_HAS_INSTANCE_VARIABLE("self_hasInstanceVariable"),
       SELF_IS_METHOD_BOUND("self_isMethodBound"),
       TC_SAVE_ERR_INFO("threadContext_saveErrInfo"),
       TC_RESTORE_ERR_INFO("threadContext_restoreErrInfo"),
       TC_GET_CONSTANT_DEFINED("threadContext_getConstantDefined"),
       TC_GET_CURRENT_MODULE("threadContext_getCurrentModule"),
       BACKREF_IS_RUBY_MATCH_DATA("backref_isRubyMatchData"),
       METHOD_PUBLIC_ACCESSIBLE("methodIsPublicAccessible"),
       CLASS_VAR_DEFINED("isClassVarDefined"),
       FRAME_SUPER_METHOD_BOUND("frame_superMethodBound"),
       SET_WITHIN_DEFINED("setWithinDefined");

       public MethAddr methAddr;
       JRubyImplementationMethod(String methodName) {
           this.methAddr = new MethAddr(methodName);
       }

       public MethAddr getMethAddr() { 
           return this.methAddr; 
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
        return new JRubyImplCallInstr(ii.getRenamedVariable(result), this.implMethod,
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii),
                closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        String   name;
        Object   receiver;
        Ruby     rt   = interp.getRuntime();
        Object   rVal = null;

        switch (this.implMethod) {
            // SSS FIXME: Note that compiler/impl/BaseBodyCompiler is using op_match2 for match() and and op_match for match2,
            // and we are replicating it here ... Is this a bug there?
            case MATCH:
                receiver = getReceiver().retrieve(interp);
                rVal = ((RubyRegexp) receiver).op_match2(interp.getContext());
                break;
            case MATCH2:
                receiver = getReceiver().retrieve(interp);
                rVal = ((RubyRegexp) receiver).op_match(interp.getContext(), (IRubyObject) getCallArgs()[0].retrieve(interp));
                break;
            case MATCH3: // ENEBO: Only for rubystring?
                receiver = getReceiver().retrieve(interp);
                rVal = ((RubyRegexp) receiver).op_match(interp.getContext(), (IRubyObject) getCallArgs()[0].retrieve(interp));
                break;
            case UNDEF_METHOD:
                rVal = RuntimeHelpers.undefMethod(interp.getContext(), getReceiver().retrieve(interp));
                break;
            case TO_ARY:
                receiver = getReceiver().retrieve(interp);
                rVal = RuntimeHelpers.aryToAry((IRubyObject) receiver);
                break;
            case SET_WITHIN_DEFINED:
                interp.getContext().setWithinDefined(((BooleanLiteral)getCallArgs()[0]).isTrue());
                break;
            case RTH_GET_DEFINED_CONSTANT_OR_BOUND_METHOD:
            {
                IRubyObject v = (IRubyObject)getCallArgs()[0].retrieve(interp);
                name = ((StringLiteral)getCallArgs()[1])._str_value;
					 ByteList definedType = RuntimeHelpers.getDefinedConstantOrBoundMethod(v, name);
                rVal = (definedType == null ? Nil.NIL : (new StringLiteral(definedType))).retrieve(interp);
                break;
            }
            case BLOCK_GIVEN:
                rVal = rt.newBoolean(interp.getBlock().isGiven());
                break;
            case RT_IS_GLOBAL_DEFINED:
                //name = getCallArgs()[0].retrieve(interp).toString();
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                rVal = rt.newBoolean(rt.getGlobalVariables().isDefined(name));
                break;
            case RT_GET_OBJECT:
                rVal = rt.getObject();
                break;
            case RT_GET_BACKREF:
                // SSS: FIXME: Or use this directly? "context.getCurrentScope().getBackRef(rt)" What is the diff??
                rVal = RuntimeHelpers.getBackref(rt, interp.getContext());
                break;
            case SELF_METACLASS:
                // SSS FIXME: Should we pass self in as a receiver and let this go to super.interpret?
                rVal = self.getMetaClass();
                break;
            case SELF_HAS_INSTANCE_VARIABLE:
                // SSS FIXME: Should we pass self in as a receiver and let this go to super.interpret?
                //name = getCallArgs()[0].retrieve(interp).toString();
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                rVal = rt.newBoolean(self.getInstanceVariables().fastHasInstanceVariable(name));
                break;
            case SELF_IS_METHOD_BOUND:
            {
                receiver = getReceiver().retrieve(interp); // SSS: This should be identical to self. Add an assert?
                boolean bound = ((IRubyObject)receiver).getMetaClass().isMethodBound(((StringLiteral)getCallArgs()[0])._str_value, false); 
                rVal = rt.newBoolean(bound);
                break;
            }
            case TC_SAVE_ERR_INFO:
                rVal = interp.getContext().getErrorInfo();
                break;
            case TC_RESTORE_ERR_INFO:
                interp.getContext().setErrorInfo((IRubyObject)getCallArgs()[0].retrieve(interp));
                break;
            case TC_GET_CONSTANT_DEFINED:
                //name = getCallArgs()[0].retrieve(interp).toString();
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                rVal = rt.newBoolean(interp.getContext().getConstantDefined(name));
                break;
            case TC_GET_CURRENT_MODULE:
                rVal = interp.getContext().getCurrentScope().getStaticScope().getModule();
                break;
            case BACKREF_IS_RUBY_MATCH_DATA:
                // bRef = getBackref()
                // flag = bRef instanceof RubyMatchData
                try {
                    // SSS: FIXME: Or use this directly? "context.getCurrentScope().getBackRef(rt)" What is the diff??
                    IRubyObject bRef = RuntimeHelpers.getBackref(rt, interp.getContext());
                    rVal = rt.newBoolean(Class.forName("RubyMatchData").isInstance(bRef)); // SSS FIXME: Is this correct?
                } catch (ClassNotFoundException e) {
                    // Should never get here!
                    throw new RuntimeException(e);
                }
                break;
            case METHOD_PUBLIC_ACCESSIBLE:
            {
                /* ------------------------------------------------------------
                 * mc = r.metaClass
                 * v  = mc.getVisibility(methodName)
                 * v.isPrivate? || (v.isProtected? && receiver/self? instanceof mc.getRealClass)
                 * ------------------------------------------------------------ */
                IRubyObject r   = (IRubyObject)getReceiver().retrieve(interp);
                RubyClass   mc  = r.getMetaClass();
                String      arg = ((StringLiteral)getCallArgs()[0])._str_value;
                Visibility  v   = mc.searchMethod(arg).getVisibility();
                rVal = rt.newBoolean((v != null) && !v.isPrivate() && !(v.isProtected() && mc.getRealClass().isInstance(r)));
                break;
            }
            case CLASS_VAR_DEFINED:
            {
                // cm.classVarDefined(name) || (cm.isSingleton && !(cm.attached instanceof RubyModule) && cm.attached.classVarDefined(name))
                boolean flag;
                RubyModule cm = (RubyModule)getReceiver().retrieve(interp);
                name = ((StringLiteral)getCallArgs()[0])._str_value;
                flag = cm.fastIsClassVarDefined(name);
                if (!flag) {
                    if (cm.isSingleton()) {
                        IRubyObject ao = ((MetaClass)cm).getAttached();
                        if (ao instanceof RubyModule) flag = ((RubyModule)ao).fastIsClassVarDefined(name);
                    }
                }
                rVal = rt.newBoolean(flag);
                break;
            }
            case FRAME_SUPER_METHOD_BOUND:
            {
                boolean flag = false;
                ThreadContext tc = interp.getContext();
                String        fn = tc.getFrameName();
                if (fn != null) {
                    RubyModule fc = tc.getFrameKlazz();
                    if (fc != null) {
                        flag = RuntimeHelpers.findImplementerIfNecessary(self.getMetaClass(), fc).getSuperClass().isMethodBound(fn, false);
                    }
                }
                rVal = rt.newBoolean(flag);
                break;
            }
        }

        // Store the result
        if (rVal != null) getResult().store(interp, rVal);

        return null;
    }
}
