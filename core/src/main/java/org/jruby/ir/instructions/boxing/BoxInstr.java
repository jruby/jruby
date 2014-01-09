package org.jruby.ir.instructions.boxing;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

public abstract class BoxInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand val;

    public BoxInstr(Operation op, Variable result, Operand val) {
        super(op);
        this.result = result;
        this.val = val;
    }

    public Operand[] getOperands() {
        return new Operand[]{val};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand getValue() {
        return val;
    }

    @Override
    public String toString() {
        return getResult() + " = " + getOperation() + "(" + val + ")";
    }
}
