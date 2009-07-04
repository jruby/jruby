package org.jruby.compiler.ir;

public class RECV_OPT_ARG_Instr extends TwoOperandInstr
{
    public RECV_OPT_ARG_Instr(Variable dest, int index, Label nullLabel)
    {
        super(Operation.RECV_OPT_ARG, dest, new ArgIndex(index), nullLabel);
    }
}
