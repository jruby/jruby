package org.jruby.compiler.ir.instructions;

import java.util.Map;
import java.util.List;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

public class RESCUED_BODY_END_MARKER_Instr extends IR_Instr
{
    private static Operand[] _empty = new Operand[] {};

    public final RESCUED_BODY_START_MARKER_Instr _rbStartInstr;

    public RESCUED_BODY_END_MARKER_Instr(RESCUED_BODY_START_MARKER_Instr start)
    {
        super(Operation.RESCUE_BODY_END);
        _rbStartInstr = start;
    }

    public Operand[] getOperands() { return _empty; }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }
}
