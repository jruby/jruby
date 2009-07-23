package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

public class PUT_FIELD_Instr extends PUT_Instr
{
    public PUT_FIELD_Instr(Operand obj, String fieldName, Operand value)
    {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }
}
