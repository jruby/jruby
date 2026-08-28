package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PushBackrefFrameInstr extends NoOperandInstr implements FixedArityInstr {
    public PushBackrefFrameInstr() {
        super(Operation.PUSH_BACKREF_FRAME);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct?
    }

    public static PushBackrefFrameInstr decode(IRReaderDecoder d) {
        return new PushBackrefFrameInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBackrefFrameInstr(this);
    }
}
