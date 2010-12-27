package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YieldInstr extends MultiOperandInstr {
    // SSS FIXME: Correct?  Where does closure arg come from?
    public YieldInstr(Variable result, Operand[] args) {
        super(Operation.YIELD, result, args);
    }
   
    public boolean isRubyInternalsCall() {
        return false;
    }

    public boolean isStaticCallTarget() {
        return false;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return this;  // This is just a placeholder during inlining.
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Object resultValue = interp.getBlock().call(interp.getContext(), prepareArguments(getOperands(), interp));
        getResult().store(interp, resultValue);
        return null;
    }
}
