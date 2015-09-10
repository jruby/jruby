package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class ExceptionRegionEndMarkerInstr extends NoOperandInstr implements FixedArityInstr {
    public ExceptionRegionEndMarkerInstr() {
        super(Operation.EXC_REGION_END);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ExceptionRegionEndMarkerInstr(this);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this;
    }
}
