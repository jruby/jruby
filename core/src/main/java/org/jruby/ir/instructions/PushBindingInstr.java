package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class PushBindingInstr extends Instr implements FixedArityInstr {
    public PushBindingInstr() {
        super(Operation.PUSH_BINDING);
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public String toString() {
        return "" + getOperation() + "()";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBindingInstr(this);
    }
}
