package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// This instruction encodes the receive of an argument into a closure
//   Ex:  .. { |a| .. }
// The closure receives 'a' via this instruction
public class ReceiveClosureArgInstr extends NoOperandInstr {
    public final int     argIndex;
    public final boolean restOfArgArray;

    public ReceiveClosureArgInstr(Variable dest, int argIndex, boolean restOfArgArray) {
        super(Operation.RECV_CLOSURE_ARG, dest);
        
        this.argIndex = argIndex;
        this.restOfArgArray = restOfArgArray;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + (restOfArgArray ? ", ALL" : "") + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, interp.getParameter(argIndex));
        return null;
    }
}
