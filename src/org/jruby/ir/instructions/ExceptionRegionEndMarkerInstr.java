package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.representations.InlinerInfo;

public class ExceptionRegionEndMarkerInstr extends Instr {
    public ExceptionRegionEndMarkerInstr() {
        super(Operation.EXC_REGION_END);
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
}
