package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PopMethodFrameInstr extends NoOperandInstr implements FixedArityInstr {
    public PopMethodFrameInstr() {
        super(Operation.POP_METHOD_FRAME);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PopMethodFrameInstr decode(IRReaderDecoder d) {
        return new PopMethodFrameInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopMethodFrameInstr(this);
    }
}
