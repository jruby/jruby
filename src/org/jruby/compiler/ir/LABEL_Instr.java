package org.jruby.compiler.ir;

public class LABEL_Instr extends NoOperandInstr
{
	 public final Label _lbl;

    public LABEL_Instr(Label l)
    {
        super(Operation.LABEL);
		  _lbl = l;
    }

	 public String toString() { return _lbl + ":"; }
}
