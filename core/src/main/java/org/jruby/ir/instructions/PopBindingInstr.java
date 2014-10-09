package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class PopBindingInstr extends Instr implements FixedArityInstr {
    public PopBindingInstr() {
        super(Operation.POP_BINDING);
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PopBindingInstr() : NopInstr.NOP;  // FIXME: Is this correct
    }

    @Override
    public String toString() {
        return "" + getOperation();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopBindingInstr(this);
    }
}
