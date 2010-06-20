package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class NOT_Instr extends OneOperandInstr
{
    public NOT_Instr(Variable dst, Operand arg) {
        super(Operation.NOT, dst, arg);
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new NOT_Instr(ii.getRenamedVariable(_result), _arg.cloneForInlining(ii));
    }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        return (_arg instanceof BooleanLiteral) ? ((BooleanLiteral)_arg).logicalNot() : null;
    }
}
