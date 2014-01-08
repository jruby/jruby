package org.jruby.ir.instructions.boxing;

import java.util.Map;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class UnboxInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand val;

    public UnboxInstr(Operation op, Variable dst, Operand val) {
        super(op);
        this.result = dst;
        this.val = val;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{ val };
    }

    public Operand getValue() {
        return val;
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        val = val.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return getResult() + " = " + getOperation() + "(" + val + ")";
    }
}
