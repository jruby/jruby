package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

public class RETURN_Instr extends OneOperandInstr
{
    Operand _retval;

    public RETURN_Instr(Operand rv)
    {
        super(Operation.RETURN, null, rv);
		  _retval = rv;
    }
}
