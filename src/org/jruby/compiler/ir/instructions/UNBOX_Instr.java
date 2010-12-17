package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.BoxedValue;
import org.jruby.compiler.ir.operands.UnboxedValue;

public class UNBOX_Instr extends OneOperandInstr
{
    public UNBOX_Instr(Variable dst, Operand v) {
        super(Operation.UNBOX_VALUE, dst, v);
    }

    public String toString() { return super.toString() + "(" + argument + ")"; }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        return (argument instanceof BoxedValue) ? ((BoxedValue)argument)._value : new UnboxedValue(argument);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new UNBOX_Instr(ii.getRenamedVariable(result), argument.cloneForInlining(ii));
    }
}
