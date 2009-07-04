package org.jruby.compiler.ir;

public class RECV_ARG_Instr extends OneOperandInstr
{
    public RECV_ARG_Instr(Variable dest, int index)
    {
        super(Operation.RECV_ARG, dest, new ArgIndex(index));
    }
}
