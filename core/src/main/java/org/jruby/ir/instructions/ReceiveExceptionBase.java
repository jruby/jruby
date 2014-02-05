package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

public class ReceiveExceptionBase extends Instr implements ResultInstr, FixedArityInstr {
    protected Variable result;

    public ReceiveExceptionBase(Operation op, Variable result) {
        super(op);

        assert result != null : "ResultExceptionInstr result is null";

        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[] {};
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation();
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }
}
