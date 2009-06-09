package org.jruby.buildr.ir;

public class RECV_ARG_Instr extends OneOperandInstr
{
    public RECV_ARG_Instr(Variable dest, Constant index)
    {
        super(Operation.RECV_ARG, dest, index);
    }
}
