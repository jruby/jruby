package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

public class RETURN_Instr extends OneOperandInstr
{
    public RETURN_Instr(Operand rv)
    {
        super(Operation.RETURN, null, rv);
    }
}
