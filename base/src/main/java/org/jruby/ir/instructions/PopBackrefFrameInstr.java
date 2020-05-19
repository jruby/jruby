package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PopBackrefFrameInstr extends NoOperandInstr implements FixedArityInstr {
    public PopBackrefFrameInstr() {
        super(Operation.POP_BACKREF_FRAME);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PopBackrefFrameInstr decode(IRReaderDecoder d) {
        return new PopBackrefFrameInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopBackrefFrameInstr(this);
    }
}
