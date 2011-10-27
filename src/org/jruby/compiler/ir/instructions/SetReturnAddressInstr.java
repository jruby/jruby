package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is of the form:
//    v = LBL_..
// Used in rescue blocks to tell the ensure block where to return to after it is done doing its thing.
public class SetReturnAddressInstr extends OneOperandInstr {
    public SetReturnAddressInstr(Variable d, Label l) {
        super(Operation.SET_RETADDR, d, l);
    }

    public Label getReturnAddr() {
        return (Label) argument;
    }

    @Override
    public String toString() {
        return "" + getResult() + " = " + argument;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SetReturnAddressInstr(ii.getRenamedVariable(getResult()), 
                ii.getRenamedLabel(getReturnAddr()));
    }

    // Can this instruction raise exceptions?
    @Override
    public boolean canRaiseException() {
        return false;
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        getResult().store(interp, context, self, ((Label)getArg()));
        
        return null;
    }
}
