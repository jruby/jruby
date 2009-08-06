package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// This is of the form:
//   v = OP(arg, attribute_array); Ex: v = NOT(v1)

public abstract class NoOperandInstr extends IR_Instr
{
    public NoOperandInstr(Operation op, Variable dest)
    {
        super(op, dest);
    }

    public NoOperandInstr(Operation op)
    {
        super(op);
    }

    public Operand[] getOperands() {
        return Operand.EMPTY_ARRAY;
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }
}
