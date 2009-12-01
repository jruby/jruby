package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;

public class RESCUE_BLOCK_Instr extends IR_Instr
{
    private static Operand[] _empty = new Operand[] {};

    Label _begin;
    Label _firstBlock;
    Label _elseBlock;
    Label _end;

    public RESCUE_BLOCK_Instr(Label rBegin, Label firstBlock, Label elseBlock, Label rEnd)
    {
        super(Operation.RESCUE);
        _begin = rBegin;
        _end = rEnd;
        _firstBlock = firstBlock;
        _elseBlock = elseBlock;
    }

    public String toString() { return super.toString() + "(" + _begin + "," + _firstBlock + "," + _elseBlock + "," + _end + ")"; }

    public Operand[] getOperands() { return _empty; }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }
}
