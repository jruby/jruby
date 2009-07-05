package org.jruby.compiler.ir;

public class PUT_CVAR_Instr extends IR_Instr
{
    final public IR_Class _class;
    final public String   _varName;
    final public Operand  _val;

    public PUT_CVAR_Instr(IR_Class c, String varName, Operand value)
    {
        super(Operation.PUT_CVAR);
		  _class = c;
		  _varName = varName;
		  _val = value;
    }
}
