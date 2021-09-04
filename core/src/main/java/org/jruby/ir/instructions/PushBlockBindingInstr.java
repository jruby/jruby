package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class PushBlockBindingInstr extends NoOperandInstr implements FixedArityInstr {
    public PushBlockBindingInstr() {
        super(Operation.PUSH_BLOCK_BINDING);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this; // FIXME: This has to be wrong if pop_binding is conditionally noop'ing on inline
    }

    public static PushBlockBindingInstr decode(IRReaderDecoder d) {
        return new PushBlockBindingInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBlockBindingInstr(this);
    }
}
