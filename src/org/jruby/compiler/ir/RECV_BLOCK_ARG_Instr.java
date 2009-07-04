package org.jruby.compiler.ir;

public class RECV_BLOCK_ARG_Instr extends OneOperandInstr
{
    public RECV_BLOCK_ARG_Instr(Variable dest, int index)
    {
        super(Operation.RECV_BLOCK_ARG, dest, new ArgIndex(index));
    }
}
