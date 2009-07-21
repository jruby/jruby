package org.jruby.compiler.ir;

public class GET_FIELD_Instr extends GET_Instr
{
    public GET_FIELD_Instr(Variable dest, Operand obj, String fieldName)
    {
        super(Operation.GET_FIELD, dest, obj, fieldName);
    }
}
