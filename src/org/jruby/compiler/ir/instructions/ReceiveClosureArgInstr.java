package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
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
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object o;
        int numArgs = interp.getParameterCount();
        if (restOfArgArray) {
            IRubyObject[] restOfArgs = new IRubyObject[numArgs-argIndex];
            int j = 0;
            for (int i = argIndex; i < numArgs; i++) {
                restOfArgs[j] = (IRubyObject)interp.getParameter(i);
                j++;
            }
            o =  org.jruby.RubyArray.newArray(interp.getRuntime(), restOfArgs);
        } else {
            if (numArgs <= argIndex) {
                o = interp.getRuntime().getNil();
            } else {
                o = interp.getParameter(argIndex);
            }
        }
        getResult().store(interp, o);
        return null;
    }
}
