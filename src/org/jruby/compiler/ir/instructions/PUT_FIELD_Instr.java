package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class PUT_FIELD_Instr extends PutInstr
{
    public PUT_FIELD_Instr(Operand obj, String fieldName, Operand value) {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PUT_FIELD_Instr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }
}
