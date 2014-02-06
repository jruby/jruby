package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class PushFrameInstr extends Instr implements FixedArityInstr {
    private final MethAddr frameName;
    public PushFrameInstr(MethAddr frameName) {
        super(Operation.PUSH_FRAME);

        this.frameName = frameName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{frameName};
    }

    public MethAddr getFrameName() {
        return frameName;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // FIXME: Is this correct?
        switch (ii.getCloneMode()) {
            case CLOSURE_INLINE:
            case METHOD_INLINE:
                return NopInstr.NOP;
            default:
                return this;
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushFrameInstr(this);
    }
}
