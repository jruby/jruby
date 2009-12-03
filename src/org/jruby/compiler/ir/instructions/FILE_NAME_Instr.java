package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;

public class FILE_NAME_Instr extends NoOperandInstr
{
    public final String _fname;

    public FILE_NAME_Instr(String f)
    {
        super(Operation.FILE_NAME);
        _fname = f;
    }

    public String toString() { return super.toString() + ":" + _fname; }
}
