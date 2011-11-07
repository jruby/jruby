package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;

public abstract class BranchInstr extends Instr {
    private Label target;
    private Operand arg1;
    private Operand arg2;

    public BranchInstr(Operation op, Operand v1, Operand v2, Label jumpTarget) {
        super(op, null);
        this.target = jumpTarget;
        this.arg1 = v1;
        this.arg2 = v2;
    }

    public Operand[] getOperands() {
        return new Operand[]{arg1, arg2};
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
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        arg1 = arg1.getSimplifiedOperand(valueMap);
        arg2 = arg2.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return "" + getOperation() + "(" + arg1 + ", " + arg2 + ", " + target + ")";
    }
}
