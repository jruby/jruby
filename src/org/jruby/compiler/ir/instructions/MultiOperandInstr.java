package org.jruby.compiler.ir.instructions;

import java.util.Arrays;
import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// This is of the form:
//   v = OP(args, attribute_array); Ex: v = CALL(args, v2)

public class MultiOperandInstr extends IR_Instr
{
    public Operand[] _args;

    public MultiOperandInstr(Operation opType, Variable result, Operand[] args)
    {
       super(opType, result);
       _args = args;
    }

    public String toString() {
        return super.toString() + Arrays.toString(_args);
    }

    public Operand[] getOperands() {
        return _args;
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap)
    {
        for (int i = 0; i < _args.length; i++)
            _args[i] = _args[i].getSimplifiedOperand(valueMap);
    }
}
