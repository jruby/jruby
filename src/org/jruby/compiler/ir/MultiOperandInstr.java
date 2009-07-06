package org.jruby.compiler.ir;

// This is of the form:

import java.util.Arrays;

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
        return super.toString() +
                ", args: " + Arrays.toString(_args);
    }
}
