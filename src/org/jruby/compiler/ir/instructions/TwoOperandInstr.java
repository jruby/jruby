package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// This is of the form:
//   v = OP(arg1, arg2, attribute_array); Ex: v = ADD(v1, v2)

public class TwoOperandInstr extends IR_Instr
{
    Operand _arg1;
    Operand _arg2;

    public TwoOperandInstr(Operation op, Variable dest, Operand a1, Operand a2)
    {
        super(op, dest);
        _arg1 = a1;
        _arg2 = a2;
    }

    public String toString() { return super.toString() + "(" + _arg1 + ", " + _arg2 + ")"; }

    public Operand[] getOperands() {
        return new Operand[] {_arg1, _arg2};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap)
    {
        Operand a1 = valueMap.get(_arg1);
        if (a1 != null)
            _arg1 = a1;
        Operand a2 = valueMap.get(_arg2);
        if (a2 != null)
            _arg2 = a2;
    }
}
