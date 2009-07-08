package org.jruby.compiler.ir;

public class CLOSURE_RETURN_Instr extends OneOperandInstr
{
    Operand _retval;

    public CLOSURE_RETURN_Instr(Operand rv)
    {
        super(Operation.CLOSURE_RETURN, null, rv);
		  _retval = rv;
    }
}
