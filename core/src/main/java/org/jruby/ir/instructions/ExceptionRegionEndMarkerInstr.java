package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;

public class ExceptionRegionEndMarkerInstr extends Instr implements FixedArityInstr {
    public ExceptionRegionEndMarkerInstr() {
        super(Operation.EXC_REGION_END);
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ExceptionRegionEndMarkerInstr(this);
    }
}
