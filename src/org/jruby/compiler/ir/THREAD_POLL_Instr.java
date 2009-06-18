package org.jruby.compiler.ir;

public class THREAD_POLL_Instr extends IR_Instr
{
    public final Operation _op;

    public THREAD_POLL_Instr()
    {
        super(Operation.THREAD_POLL, null);
    }
}
