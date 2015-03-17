package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class PushBindingInstr extends Instr implements FixedArityInstr {
    public PushBindingInstr() {
        super(Operation.PUSH_BINDING, EMPTY_OPERANDS);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this; // FIXME: This has to be wrong if pop_binding is conditionally noop'ing on inline
    }

    public static PushBindingInstr decode(IRReaderDecoder d) {
        return new PushBindingInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBindingInstr(this);
    }
}
