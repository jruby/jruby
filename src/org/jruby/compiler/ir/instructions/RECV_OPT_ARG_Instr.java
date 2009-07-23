package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.ArgIndex;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;

public class RECV_OPT_ARG_Instr extends TwoOperandInstr
{
    public RECV_OPT_ARG_Instr(Variable dest, int index, Label nullLabel)
    {
        super(Operation.RECV_OPT_ARG, dest, new ArgIndex(index), nullLabel);
    }
}
