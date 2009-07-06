package org.jruby.compiler.ir;

public class LABEL_Instr extends IR_Instr
{
	 public final Label _lbl;

    public LABEL_Instr(Label l)
    {
        super(Operation.LABEL);
		  _lbl = l;
    }

	 public String toString() { return _lbl + ":"; }
}
