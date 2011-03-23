package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is of the form:
//    v = LBL_..
// Used in rescue blocks to tell the ensure block where to return to after it is done doing its thing.
public class SET_RETADDR_Instr extends OneOperandInstr 
{
    public SET_RETADDR_Instr(Variable d, Label l) {
        super(Operation.SET_RETADDR, d, l);
    }

    public Label getReturnAddr() { return (Label)argument; }

    public String toString() { return "\t" + result + " = " + argument; }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SET_RETADDR_Instr(ii.getRenamedVariable(result), ii.getRenamedLabel((Label)argument));
    }

    // Can this instruction raise exceptions?
    @Override
    public boolean canRaiseException() { return false; }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, ((Label)getArg()));
        return null;
    }
}
