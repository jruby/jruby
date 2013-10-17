package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class PopBindingInstr extends Instr {
    public PopBindingInstr() {
        super(Operation.POP_BINDING);
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new PopBindingInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopBindingInstr(this);
    }
}
