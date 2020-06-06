package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class PushMethodBindingInstr extends NoOperandInstr implements FixedArityInstr {
    public PushMethodBindingInstr() {
        super(Operation.PUSH_METHOD_BINDING);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this; // FIXME: This has to be wrong if pop_binding is conditionally noop'ing on inline
    }

    public static PushMethodBindingInstr decode(IRReaderDecoder d) {
        return new PushMethodBindingInstr();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushMethodBindingInstr(this);
    }
}
