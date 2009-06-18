package org.jruby.compiler.ir;

public class LABEL_Instr
{
    public final Operation _op;
    public final Operand   _result; 

    public LABEL_Instr(Label l)
    {
        super(Operation.LABEL, l);
    }
}
