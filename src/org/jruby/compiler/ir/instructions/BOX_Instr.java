package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.BoxedValue;
import org.jruby.compiler.ir.operands.UnboxedValue;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class BOX_Instr extends OneOperandInstr
{
    public BOX_Instr(Variable dst, Operand v) {
        super(Operation.BOX_VALUE, dst, v);
    }

    public Operand[] getOperands() { return new Operand[] {argument}; }

    public String toString() { return super.toString() + "(" + argument + ")"; }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
		  return (argument instanceof UnboxedValue) ? ((UnboxedValue)argument)._value : new BoxedValue(argument);
	}

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BOX_Instr(ii.getRenamedVariable(result), argument.cloneForInlining(ii));
    }
}
