package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

import java.util.Map;

public abstract class BranchInstr extends Instr {
    private Label target;
    private Operand arg1;
    private Operand arg2;

    public BranchInstr(Operation op, Operand v1, Operand v2, Label jumpTarget) {
        super(op);
        this.target = jumpTarget;
        this.arg1 = v1;
        this.arg2 = v2;
    }

    public Operand[] getOperands() {
        return arg2 == null ? new Operand[]{arg1} : new Operand[]{arg1, arg2};
    }

    public Operand getArg1() {
        return arg1;
    }

    public Operand getArg2() {
        return arg2;
    }

    public Label getJumpTarget() {
        return target;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        arg1 = arg1.getSimplifiedOperand(valueMap, force);
        if (arg2 != null) arg2 = arg2.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return "" + getOperation() + "(" + arg1 + ", " + (arg2 != null ? arg2 + ", " : "") + target + ")";
    }
}
