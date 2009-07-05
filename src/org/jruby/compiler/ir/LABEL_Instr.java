package org.jruby.compiler.ir;

public class LABEL_Instr extends OneOperandInstr
{
    public LABEL_Instr(Label l)
    {
        super(Operation.LABEL, null, l);
    }
}
