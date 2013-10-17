package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class PopFrameInstr extends Instr {
    public PopFrameInstr() {
        super(Operation.POP_FRAME);
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopFrameInstr(this);
    }
}
