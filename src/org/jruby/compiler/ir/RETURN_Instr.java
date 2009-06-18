package org.jruby.compiler.ir;

public class RETURN_Instr extends OneOperandInstr
{
    Operand _retval;

    public RETURN_Instr(Operand rv)
    {
        super(Operation.RETURN, null, _retval);
    }
}
