package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

public class ExceptionRegionStartMarkerInstr extends Instr {
    final public Label begin;
    final public Label end;
    final public Label firstRescueBlockLabel;

    public ExceptionRegionStartMarkerInstr(Label begin, Label end, Label firstRescueBlockLabel) {
        super(Operation.EXC_REGION_START);

        this.begin = begin;
        this.end = end;
        this.firstRescueBlockLabel = firstRescueBlockLabel;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ExceptionRegionStartMarkerInstr(this);
    }
}
