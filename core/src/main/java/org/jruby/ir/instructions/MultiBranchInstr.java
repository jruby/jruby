package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;

public abstract class MultiBranchInstr extends Instr {
    public MultiBranchInstr(Operation op) {
        super(op);
    }

    public abstract Label[] getJumpTargets();
}
