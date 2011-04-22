package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyRegexp;
import org.jruby.RubyClass;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Visibility;

public class JRubyImplCallInstr extends CallInstr {
    public JRubyImplCallInstr(Variable result, MethAddr methAddr, Operand receiver, Operand[] args) {
        super(Operation.JRUBY_IMPL, result, methAddr, receiver, args, null);
    }

    public JRubyImplCallInstr(Variable result, MethAddr methAddr, Operand receiver, Operand[] args,
            Operand closure) {
        super(result, methAddr, receiver, args, closure);
    }

    @Override
    public boolean isStaticCallTarget() {
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new JRubyImplCallInstr(ii.getRenamedVariable(result), (MethAddr) methAddr.cloneForInlining(ii),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii),
                closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Object   receiver;
        MethAddr ma    = getMethodAddr();
        String   mName = ma.getName();
        Ruby     rt    = interp.getRuntime();
        Object   rVal  = null;

        if (ma == MethAddr.MATCH2) {
            receiver = getReceiver().retrieve(interp);
            rVal = ((RubyRegexp) receiver).op_match(interp.getContext(), (IRubyObject) getCallArgs()[0].retrieve(interp));
        } else if (ma == MethAddr.MATCH3) { // ENEBO: Only for rubystring?
            receiver = getReceiver().retrieve(interp);
            rVal = ((RubyRegexp) receiver).op_match(interp.getContext(), (IRubyObject) getCallArgs()[0].retrieve(interp));
        } else if (ma == MethAddr.TO_ARY) {
            receiver = getReceiver().retrieve(interp);
            rVal = RuntimeHelpers.aryToAry((IRubyObject) receiver);
        } else if (mName.equals("threadContext_saveErrInfo")) {
            rVal = interp.getContext().getErrorInfo();
        } else if (mName.equals("threadContext_restoreErrInfo")) {
            interp.getContext().setErrorInfo((IRubyObject)getCallArgs()[0].retrieve(interp));
        } else if (mName.equals("threadContext_getConstantDefined")) {
            //String name = getCallArgs()[0].retrieve(interp).toString();
            String name = ((StringLiteral)getCallArgs()[0])._str_value;
            rVal = rt.newBoolean(interp.getContext().getConstantDefined(name));
        } else if (mName.equals("self_hasInstanceVariable")) {
            receiver = getReceiver().retrieve(interp); // SSS: This should be identical to self. Add an assert?
            //String name = getCallArgs()[0].retrieve(interp).toString();
            String name = ((StringLiteral)getCallArgs()[0])._str_value;
            rVal = rt.newBoolean(((IRubyObject)receiver).getInstanceVariables().fastHasInstanceVariable(name));
        } else if (mName.equals("runtime_isGlobalDefined")) {
            //String name = getCallArgs()[0].retrieve(interp).toString();
            String name = ((StringLiteral)getCallArgs()[0])._str_value;
            rVal = rt.newBoolean(rt.getGlobalVariables().isDefined(name));
        } else if (mName.equals("runtime_getObject")) {
            rVal = rt.getObject();
        } else if (mName.equals("block_isGiven")) {
            rVal = rt.newBoolean(interp.getBlock().isGiven());
        } else if (mName.equals("self_isMethodBound")) {
            receiver = getReceiver().retrieve(interp); // SSS: This should be identical to self. Add an assert?
            boolean bound = ((IRubyObject)receiver).getMetaClass().isMethodBound(((StringLiteral)getCallArgs()[0])._str_value, false); // No visibility check
            rVal = rt.newBoolean(bound);
        } else if (mName.equals("runtime_getBackref")) {
            // SSS: FIXME: Or use this directly? "context.getCurrentScope().getBackRef(rt)" What is the diff??
            rVal = RuntimeHelpers.getBackref(rt, interp.getContext());
        } else if (mName.equals("backref_isRubyMatchData")) {
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
        } else if (mName.equals("verifyMethodIsPublicAccessible")) {
            /* ------------------------------------------------------------
             * mc = r.metaClass
             * v  = mc.getVisibility(methodName)
             * v.isPrivate? || (v.isProtected? && receiver/self? instanceof mc.getRealClass)
             * ------------------------------------------------------------ */
            IRubyObject r   = (IRubyObject)getReceiver().retrieve(interp);
            RubyClass   mc  = r.getMetaClass();
            String      arg = ((StringLiteral)getCallArgs()[0])._str_value;
            Visibility  v   = mc.searchMethod(arg).getVisibility();
            rVal = rt.newBoolean(v.isPrivate() || (v.isProtected() && mc.getRealClass().isInstance(r)));
        } else {
            super.interpret(interp, self);
        }

        // Store the result
        if (rVal != null) getResult().store(interp, rVal);

        return null;
    }
}
