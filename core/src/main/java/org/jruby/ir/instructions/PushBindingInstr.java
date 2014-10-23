package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class PushBindingInstr extends Instr implements FixedArityInstr {
    public PushBindingInstr() {
        super(Operation.PUSH_BINDING);
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this; // FIXME: This has to be wrong if pop_binding is conditionally noop'ing on inline
    }

    @Override
    public String toString() {
        return "" + getOperation();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBindingInstr(this);
    }
}
