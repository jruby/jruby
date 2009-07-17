package org.jruby.compiler.ir;

public class JUMP_Instr extends NoOperandInstr
{
    public final Label _target; 

    public JUMP_Instr(Label l)
    {
        super(Operation.JUMP);
        _target = l;
    }

    public String toString() {
        return super.toString() + " " + _target;
    }
}
