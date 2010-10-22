package org.jruby.compiler.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JRubyImplCallInstr extends CallInstr {
    public JRubyImplCallInstr(Variable result, Operand methAddr, Operand[] args) {
        super(Operation.JRUBY_IMPL, result, methAddr, args, null);
    }

    public JRubyImplCallInstr(Variable result, Operand methAddr, Operand[] args, Operand closure) {
        super(result, methAddr, args, closure);
    }

    @Override
    public boolean isStaticCallTarget() {
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new JRubyImplCallInstr(ii.getRenamedVariable(result), _methAddr.cloneForInlining(ii), super.cloneCallArgs(ii), _closure == null ? null : _closure.cloneForInlining(ii));
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        Object receiver = getReceiver().retrieve(interp);

        if (getMethodAddr() == MethAddr.MATCH3) {
            getResult().store(interp, ((RubyRegexp) receiver).op_match(interp.getContext(),
                    (IRubyObject) getCallArgs()[1].retrieve(interp)));
        } else {
            super.interpret(interp, self);
        }
    }
}
