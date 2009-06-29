package org.jruby.compiler.ir;

// SSS FIXME: Should I have a reference to the IR_loop whose iteration is being redone?
public class REDO_Instr extends OneOperandInstr
{
    Label _jumpLabel;

    public REDO_Instr(Label iterStart)
    {
        super(Operation.REDO, null, null);
		  _jumpLabel = iterStart;
    }
}
