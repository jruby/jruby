package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class LabelInstr extends OneOperandInstr implements FixedArityInstr {
    public LabelInstr(Label label) {
        super(Operation.LABEL, label);
    }

    public Label getLabel() {
        return (Label) getOperand1();
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

    public static LabelInstr decode(IRReaderDecoder d) {
        return new LabelInstr(d.decodeLabel());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LabelInstr(this);
    }
}
