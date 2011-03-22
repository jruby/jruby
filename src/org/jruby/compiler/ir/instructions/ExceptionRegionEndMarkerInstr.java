package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class ExceptionRegionEndMarkerInstr extends Instr
{
    private static Operand[] _empty = new Operand[] {};

    public ExceptionRegionEndMarkerInstr() {
        super(Operation.EXC_REGION_END);
    }

    public Operand[] getOperands() { return _empty; }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }

    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }
}
