package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;

public class LINE_NUM_Instr extends NoOperandInstr
{
    public final int _lnum;

    public LINE_NUM_Instr(int n)
    {
        super(Operation.LINE_NUM);
        _lnum = n;
    }

    public String toString() { return super.toString() + ":" + _lnum; }
}
