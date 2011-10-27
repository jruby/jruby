package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class ExceptionRegionEndMarkerInstr extends Instr {
    public ExceptionRegionEndMarkerInstr() {
        super(Operation.EXC_REGION_END);
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }
}
