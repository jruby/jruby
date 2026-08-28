package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class RestoreBindingVisibilityInstr extends OneOperandInstr implements FixedArityInstr {
    public RestoreBindingVisibilityInstr(Operand viz) {
        super(Operation.RESTORE_BINDING_VIZ, viz);
    }

    public Operand getVisibility() {
        return getOperand1();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? this : NopInstr.NOP;  // FIXME: Is this correct?
    }

    public static RestoreBindingVisibilityInstr decode(IRReaderDecoder d) {
        return new RestoreBindingVisibilityInstr(d.decodeVariable());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getVisibility());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RestoreBindingVisibilityInstr(this);
    }
}
