package org.jruby.ir.instructions;

import org.jruby.ir.IRManager;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;

public abstract class BranchInstr extends Instr implements JumpTargetInstr {
    public BranchInstr(Operation op) {
        super(op);
    }

    public abstract Label getJumpTarget();
}
