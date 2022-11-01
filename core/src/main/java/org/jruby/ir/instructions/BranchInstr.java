package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.Label;

public abstract class BranchInstr extends Instr implements JumpTargetInstr {
    public BranchInstr(Operation op) {
        super(op);
    }

    public abstract Label getJumpTarget();

    public Instr simplifyBranch(FullInterpreterContext fic) {
        return this;
    }
}
