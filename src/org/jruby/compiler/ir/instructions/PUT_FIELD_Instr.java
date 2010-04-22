package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class PUT_FIELD_Instr extends PUT_Instr
{
    public PUT_FIELD_Instr(Operand obj, String fieldName, Operand value) {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new PUT_FIELD_Instr(_target.cloneForInlining(ii), _ref, _value.cloneForInlining(ii));
    }
}
