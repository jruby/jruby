package org.jruby.buildr.ir;

public class RECV_OPT_ARG_Instr extends TwoOperandInstr
{
    public RECV_OPT_ARG_Instr(Variable dest, Constant index, Label nullLabel)
    {
        super(Operation.RECV_OPT_ARG, dest, index, nullLabel);
    }
}
