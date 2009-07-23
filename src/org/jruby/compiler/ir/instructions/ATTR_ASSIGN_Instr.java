package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

public class ATTR_ASSIGN_Instr extends MultiOperandInstr
{
    public ATTR_ASSIGN_Instr(Operand obj, Operand attr, Operand value)
    {
        super(Operation.ATTR_ASSIGN, null, new Operand[] { obj, attr, value });
    }
}
