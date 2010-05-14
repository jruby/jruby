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

    public Operand[] getOperands() { return new Operand[] {_arg}; }

    public String toString() { return super.toString() + "(" + _arg + ")"; }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
		  return (_arg instanceof UnboxedValue) ? ((UnboxedValue)_arg)._value : new BoxedValue(_arg);
	}

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new BOX_Instr(ii.getRenamedVariable(_result), _arg.cloneForInlining(ii));
    }
}
