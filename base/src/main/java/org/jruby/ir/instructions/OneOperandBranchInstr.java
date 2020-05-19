package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

import org.jruby.ir.persistence.IRWriterEncoder;

public abstract class OneOperandBranchInstr extends BranchInstr {
    private Label jumpTarget;
    private Operand value;

    public OneOperandBranchInstr(Operation op, Label jumpTarget, Operand value) {
        super(op);

        this.jumpTarget = jumpTarget;
        this.value = value;
    }

    public Operand[] getOperands() {
        return new Operand[] { jumpTarget, value };
    }

    public void setOperand(int i, Operand operand) {
        switch (i) {
            case 0:
                jumpTarget = (Label) operand;
                break;
            case 1:
                value = operand;
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
        return value;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getJumpTarget());
        e.encode(getArg1());
    }
}
