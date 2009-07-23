package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class ALU_Instr extends TwoOperandInstr
{
    public ALU_Instr(Operation op, Variable dst, Operand arg1, Operand arg2)
    {
        super(op, dst, arg1, arg2);
    }

    public ALU_Instr(Operation op, Variable dst, Operand arg)
    {
        super(op, dst, arg, null);
    }
}
