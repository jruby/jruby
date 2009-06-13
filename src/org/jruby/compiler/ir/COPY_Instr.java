package org.jruby.compiler.ir;

// This is of the form:
//   d = s
public class COPY_Instr extends OneOperandInstr 
{
    public COPY_Instr(Variable d, Operand s)
    {
        super(Operation.COPY, d, s);
    }
}
