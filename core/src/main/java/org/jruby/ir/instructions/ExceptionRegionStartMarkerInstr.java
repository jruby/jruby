package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

public class ExceptionRegionStartMarkerInstr extends Instr implements FixedArityInstr {
    final public Label begin;
    final public Label end;
    final public Label firstRescueBlockLabel;

    public ExceptionRegionStartMarkerInstr(Label begin, Label end, Label firstRescueBlockLabel) {
        super(Operation.EXC_REGION_START);

        this.begin = begin;
        this.end = end;
        this.firstRescueBlockLabel = firstRescueBlockLabel;
    }
    
    @Override
    public Operand[] getOperands() {
        return new Operand[] { begin, end, firstRescueBlockLabel };
    }    

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());

        buf.append("(").append(begin).append(", ").append(end).append(", rescue[").append(firstRescueBlockLabel).append("]");
        buf.append(")");

        return buf.toString();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ExceptionRegionStartMarkerInstr(this);
    }
}
