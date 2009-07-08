package org.jruby.compiler.ir;

public class RECV_CLOSURE_ARG_Instr extends OneOperandInstr
{
    public RECV_CLOSURE_ARG_Instr(Variable dest, int index)
    {
        super(Operation.RECV_CLOSURE_ARG, dest, new ArgIndex(index));
    }
}
