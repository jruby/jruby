package org.jruby.compiler.ir;

public class PUT_FIELD_Instr extends MultiOperandInstr
{
    public PUT_FIELD_Instr(Operand obj, String fieldName, Object value)
    {
        super(Operation.PUT_FIELD, null, new Operand[] { dest, obj, new FieldRef(fieldName) });
    }
}
