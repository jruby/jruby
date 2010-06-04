package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// This instruction encodes the receive of an argument into a closure
//   Ex:  .. { |a| .. }
// The closure receives 'a' via this instruction
public class RECV_CLOSURE_ARG_Instr extends NoOperandInstr
{
    public final int     _argIndex;
    public final boolean _restOfArgArray;

    public RECV_CLOSURE_ARG_Instr(Variable dest, int argIndex, boolean restOfArgArray)
    {
        super(Operation.RECV_CLOSURE_ARG, dest);
        _argIndex = argIndex;
        _restOfArgArray = restOfArgArray;
    }

    public String toString() { return super.toString() + "(" + _argIndex + (_restOfArgArray ? ", ALL" : "") + ")"; }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }
}
