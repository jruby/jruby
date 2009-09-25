package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

public class CLOSURE_RETURN_Instr extends OneOperandInstr
{
    public CLOSURE_RETURN_Instr(Operand rv)
    {
        super(Operation.CLOSURE_RETURN, null, rv);
    }

        // By default, return instructions cannot be deleted
    public boolean hasSideEffects() { return true; }
}
