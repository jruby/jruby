package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;

public abstract class TwoOperandBranchInstr extends BranchInstr {
    private Label jumpTarget;
    private Operand arg1;
    private Operand arg2;

    public TwoOperandBranchInstr(Operation op, Label jumpTarget, Operand arg1, Operand arg2) {
        super(op);

        this.jumpTarget = jumpTarget;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public Operand[] getOperands() {
        return new Operand[] { jumpTarget, arg1, arg2 };
    }

    public void setOperand(int i, Operand operand) {
        switch (i) {
            case 0:
                jumpTarget = (Label) operand;
                break;
            case 1:
                arg1 = operand;
                break;
            case 2:
                arg2 = operand;
                break;
            default:
                throw new IllegalArgumentException("No such operand to set at index: " + i);
        }
    }

    public void setJumpTarget(Label target) {
        this.jumpTarget = target;
    }

    public Label getJumpTarget() {
        return jumpTarget;
    }

    public Operand getArg1() {
        return arg1;
    }

    public Operand getArg2() {
        return arg2;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getJumpTarget());
        e.encode(getArg1());
        e.encode(getArg2());
    }

}
