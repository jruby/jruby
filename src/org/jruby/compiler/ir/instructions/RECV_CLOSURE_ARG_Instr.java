package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;

public class RECV_CLOSURE_ARG_Instr extends NoOperandInstr
{
    int     _argIndex;
    boolean _restOfArgArray;

    public RECV_CLOSURE_ARG_Instr(Variable dest, int argIndex, boolean restOfArgArray)
    {
        super(Operation.RECV_CLOSURE_ARG, dest);
        _argIndex = argIndex;
        _restOfArgArray = restOfArgArray;
    }

    public String toString() { return super.toString() + "(" + _argIndex + (_restOfArgArray ? ", ALL" : "") + ")"; }
}
