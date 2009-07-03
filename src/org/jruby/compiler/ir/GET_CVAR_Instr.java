package org.jruby.compiler.ir;

public class GET_CVAR_Instr extends TwoOperandInstr
{
    public GET_CVAR_Instr(Variable dest, IR_Class c, String varName)
    {
        super(Operation.GET_CVAR, dest, new MetaObject(c), new Reference(varName));
    }
}
