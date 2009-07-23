package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;

public class THREAD_POLL_Instr extends NoOperandInstr
{
    public THREAD_POLL_Instr()
    {
        super(Operation.THREAD_POLL, null);
    }
}
