package org.jruby.compiler.ir;

public class JUMP_Instr extends IR_Instr
{
    public final Label _target; 

    public JUMP_Instr(Label l)
    {
        super(Operation.JUMP);
        _target = l;
    }
}
