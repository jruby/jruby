package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PushFrameInstr extends Instr implements FixedArityInstr {
    private final String frameName;
    public PushFrameInstr(String frameName) {
        super(Operation.PUSH_FRAME, EMPTY_OPERANDS);

        this.frameName = frameName;
    }

    public String getFrameName() {
        return frameName;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct?
    }

    public static PushFrameInstr decode(IRReaderDecoder d) {
        return new PushFrameInstr(d.decodeString());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushFrameInstr(this);
    }
}
