package org.jruby.compiler.ir;

// SSS FIXME: Should I have a reference to the IR_loop that is being retried?
public class RETRY_Instr extends OneOperandInstr
{
    Label _jumpLabel;

    public RETRY_Instr(Label loopStart)
    {
        super(Operation.RETRY, null, null);
		  _jumpLabel = loopStart;
    }
}
