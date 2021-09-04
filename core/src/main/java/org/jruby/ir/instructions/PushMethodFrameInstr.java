package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.runtime.Visibility;

public class PushMethodFrameInstr extends NoOperandInstr implements FixedArityInstr {
    private final RubySymbol frameName;
    private final Visibility visibility;
    
    public PushMethodFrameInstr(RubySymbol frameName, Visibility visibility) {
        super(Operation.PUSH_METHOD_FRAME);

        this.frameName = frameName;
        this.visibility = visibility;
    }

    public RubySymbol getFrameName() {
        return frameName;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct?
    }

    public static PushMethodFrameInstr decode(IRReaderDecoder d) {
        return new PushMethodFrameInstr(d.decodeSymbol(), Visibility.values()[d.decodeByte()]);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushMethodFrameInstr(this);
    }
}
