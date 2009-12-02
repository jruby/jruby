package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;

// This is of the form:
//    v = LBL_..
// Used in rescue blocks to tell the ensure block where to return to after it is done doing its thing.
public class SET_RETADDR_Instr extends OneOperandInstr 
{
    public SET_RETADDR_Instr(Variable d, Label l)
    {
        super(Operation.SET_RETADDR, d, l);
    }

    public Label getReturnAddr() { return (Label)_arg; }

    public String toString() { return "\t" + _result + " = " + _arg; }
}
