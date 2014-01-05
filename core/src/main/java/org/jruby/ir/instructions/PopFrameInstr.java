package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class PopFrameInstr extends Instr implements FixedArityInstr {
    public PopFrameInstr() {
        super(Operation.POP_FRAME);
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
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
        visitor.PopFrameInstr(this);
    }
}
