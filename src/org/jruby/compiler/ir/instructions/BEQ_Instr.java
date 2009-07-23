package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;

public class BEQ_Instr extends BRANCH_Instr
{
    public BEQ_Instr(Operand v1, Operand v2, Label jmpTarget)
    {
        super(Operation.BEQ, v1, v2, jmpTarget);
    }
}
