package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PopBlockFrameInstr extends OneOperandInstr implements FixedArityInstr {
    public PopBlockFrameInstr(Operand frame) {
        super(Operation.POP_BLOCK_FRAME, frame);
    }

    public Operand getFrame() {
        return getOperand1();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getFrame());
    }

    public static PopBlockFrameInstr decode(IRReaderDecoder d) {
        return new PopBlockFrameInstr(d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopBlockFrameInstr(this);
    }
}
