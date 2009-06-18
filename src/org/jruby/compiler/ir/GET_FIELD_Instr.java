package org.jruby.compiler.ir;

public class GET_FIELD_Instr extends TwoOperandInstr
{
    public GET_FIELD_Instr(Variable dest, String fieldName)
    {
        super(Operation.GET_FIELD, dest, new Variable("self"), new FieldRef(fieldName));
    }
}
