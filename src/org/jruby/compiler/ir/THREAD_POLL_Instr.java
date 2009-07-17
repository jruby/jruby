package org.jruby.compiler.ir;

public class THREAD_POLL_Instr extends NoOperandInstr
{
    public THREAD_POLL_Instr()
    {
        super(Operation.THREAD_POLL, null);
    }
}
