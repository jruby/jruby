package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PushBlockFrameInstr extends NoOperandResultBaseInstr implements FixedArityInstr {
    private final RubySymbol frameName;

    public PushBlockFrameInstr(Variable result, RubySymbol frameName) {
        super(Operation.PUSH_BLOCK_FRAME, result);
        this.frameName = frameName;
    }

    public RubySymbol getFrameName() {
        return frameName;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct?
    }

    public static PushBlockFrameInstr decode(IRReaderDecoder d) {
        return new PushBlockFrameInstr(d.decodeVariable(), d.decodeSymbol());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getFrameName());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBlockFrameInstr(this);
    }
}
