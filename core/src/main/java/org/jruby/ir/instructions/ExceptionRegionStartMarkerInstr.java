package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class ExceptionRegionStartMarkerInstr extends OneOperandInstr implements FixedArityInstr {
    public ExceptionRegionStartMarkerInstr(Label firstRescueBlockLabel) {
        super(Operation.EXC_REGION_START, firstRescueBlockLabel);
    }

    public Label getFirstRescueBlockLabel() {
        return (Label) getOperand1();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ExceptionRegionStartMarkerInstr(ii.getRenamedLabel(getFirstRescueBlockLabel()));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getFirstRescueBlockLabel());
    }

    public static ExceptionRegionStartMarkerInstr decode(IRReaderDecoder d) {
        return new ExceptionRegionStartMarkerInstr(d.decodeLabel());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ExceptionRegionStartMarkerInstr(this);
    }
}
