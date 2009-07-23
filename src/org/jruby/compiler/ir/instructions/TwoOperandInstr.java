package org.jruby.compiler.ir.instructions;

// This is of the form:

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

//   v = OP(arg1, arg2, attribute_array); Ex: v = ADD(v1, v2)

public class TwoOperandInstr extends IR_Instr
{
    public final Operand _arg1;
    public final Operand _arg2;

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
}
