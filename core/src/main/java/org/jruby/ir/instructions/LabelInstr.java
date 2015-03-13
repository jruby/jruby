package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class LabelInstr extends Instr implements FixedArityInstr {
    public LabelInstr(Label label) {
        super(Operation.LABEL, new Operand[] { label });
    }

    public Label getLabel() {
        return (Label) operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new LabelInstr(ii.getRenamedLabel(getLabel()));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getLabel());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LabelInstr(this);
    }
}
