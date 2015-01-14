package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PopFrameInstr extends Instr implements FixedArityInstr {
    public PopFrameInstr() {
        super(Operation.POP_FRAME, EMPTY_OPERANDS);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopFrameInstr(this);
    }
}
