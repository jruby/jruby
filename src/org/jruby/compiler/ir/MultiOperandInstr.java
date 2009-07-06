package org.jruby.compiler.ir;

import java.util.Arrays;

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
}
