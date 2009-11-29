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
        // SSS FIXME: Looks like _arg2 can be null for NOT alu instructions -- fix this IR bug.
        if (_arg2 == null)
            return new Operand[] {_arg1};
        else 
            return new Operand[] {_arg1, _arg2};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap)
    {
        _arg1 = _arg1.getSimplifiedOperand(valueMap);
        // SSS FIXME: Looks like _arg2 can be null for NOT alu instructions -- fix this IR bug.
        if (_arg2 == null)
           ; //System.out.println("Got null arg2 for a 2-operand instruction: " + this.toString());
        else
           _arg2 = _arg2.getSimplifiedOperand(valueMap);
    }
}
