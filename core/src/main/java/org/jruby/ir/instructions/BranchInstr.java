package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;

public abstract class BranchInstr extends Instr {
    private final Label jumpTarget; // Destination if branch succeeds.

    public BranchInstr(Operation op, Label jumpTarget) {
        super(op);
        this.jumpTarget = jumpTarget;
    }

    public Label getJumpTarget() {
        return jumpTarget;
    }
}
