package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ExceptionRegionStartMarkerInstr extends Instr implements FixedArityInstr {
    final public Label firstRescueBlockLabel;

    public ExceptionRegionStartMarkerInstr(Label firstRescueBlockLabel) {
        super(Operation.EXC_REGION_START);

        this.firstRescueBlockLabel = firstRescueBlockLabel;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { firstRescueBlockLabel };
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());

        buf.append("(").append(firstRescueBlockLabel).append(")");

        return buf.toString();
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ExceptionRegionStartMarkerInstr(ii.getRenamedLabel(firstRescueBlockLabel));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ExceptionRegionStartMarkerInstr(this);
    }
}
