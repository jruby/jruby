package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

public abstract class BranchInstr extends NOperandInstr {
    public BranchInstr(Operation op, Operand[] operands) {
        super(op, operands);
    }

    public Label getJumpTarget() {
        return (Label) operands[0];
    }
}
