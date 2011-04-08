package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyRegexp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;

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
        Object receiver;
        Ruby   rt = interp.getRuntime();

        if (getMethodAddr() == MethAddr.MATCH2) {
            receiver = getReceiver().retrieve(interp);
            getResult().store(interp, ((RubyRegexp) receiver).op_match(interp.getContext(),
                    (IRubyObject) getCallArgs()[0].retrieve(interp)));
        } else if (getMethodAddr() == MethAddr.MATCH3) { // ENEBO: Only for rubystring?
            receiver = getReceiver().retrieve(interp);
            getResult().store(interp, ((RubyRegexp) receiver).op_match(interp.getContext(),
                    (IRubyObject) getCallArgs()[0].retrieve(interp)));
        } else if (getMethodAddr() == MethAddr.TO_ARY) {
            receiver = getReceiver().retrieve(interp);
            getResult().store(interp, RuntimeHelpers.aryToAry((IRubyObject) receiver));
        } else if (getMethodAddr().getName().equals("threadContext_saveErrInfo")) {
            getResult().store(interp, interp.getContext().getErrorInfo());
        } else if (getMethodAddr().getName().equals("threadContext_restoreErrInfo")) {
            interp.getContext().setErrorInfo((IRubyObject)getCallArgs()[0].retrieve(interp));
        } else if (getMethodAddr().getName().equals("threadContext_getConstantDefined")) {
            String name = getCallArgs()[0].retrieve(interp).toString();
            getResult().store(interp, rt.newBoolean(interp.getContext().getConstantDefined(name)));
        } else if (getMethodAddr().getName().equals("self_hasInstanceVariable")) {
            receiver = getReceiver().retrieve(interp); // SSS: This should be identical to self. Add an assert?
            String name = getCallArgs()[0].retrieve(interp).toString();
            getResult().store(interp, rt.newBoolean(((IRubyObject)receiver).getInstanceVariables().fastHasInstanceVariable(name)));
        } else if (getMethodAddr().getName().equals("runtime_isGlobalDefined")) {
            String name = getCallArgs()[0].retrieve(interp).toString();
            getResult().store(interp, rt.newBoolean(rt.getGlobalVariables().isDefined(name)));
        } else if (getMethodAddr().getName().equals("runtime_getObject")) {
            getResult().store(interp, rt.getObject());
        } else if (getMethodAddr().getName().equals("block_isGiven")) {
            getResult().store(interp, rt.newBoolean(interp.getBlock().isGiven()));
        } else {
            super.interpret(interp, self);
        }
        return null;
    }
}
