package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

public class LabelInstr extends Instr {
    public final Label label;

    public LabelInstr(Label label) {
        super(Operation.LABEL);

        this.label = label;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public String toString() {
        return label + ":";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LabelInstr(this);
    }

    public Label getLabel() {
        return label;
    }
}
