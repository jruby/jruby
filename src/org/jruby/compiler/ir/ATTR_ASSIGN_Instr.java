package org.jruby.compiler.ir;

public class ATTR_ASSIGN_Instr extends MultiOperandInstr
{
    public ATTR_ASSIGN_Instr(Operand obj, Operand attr, Operand value)
    {
        super(Operation.ATTR_ASSIGN, null, new Operand[] { obj, attr, value });
    }
}
