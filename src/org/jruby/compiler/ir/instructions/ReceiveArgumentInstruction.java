package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign Argument passed into scope/method to destination Variable
 */
public class ReceiveArgumentInstruction extends NoOperandInstr {
	 // SSS FIXME: Fix IR to start offsets from 0
    int argIndex;
    boolean restOfArgArray; // If true, the argument sub-array starting at this index is passed in via this instruction.

    public ReceiveArgumentInstruction(Variable destination, int argIndex,
            boolean restOfArgArray) {
        super(Operation.RECV_ARG, destination);
        
        this.argIndex = argIndex;
        this.restOfArgArray = restOfArgArray;
    }

    public ReceiveArgumentInstruction(Variable destination, int index) {
        this(destination, index, false);
    }
    
    public boolean isRestOfArgArray() {
        return restOfArgArray;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, restOfArgArray));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + (restOfArgArray ? ", ALL" : "") + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Operand destination = getResult(); // result is a confusing name

        if (restOfArgArray) {
            interpretAsRestArg(interp, context, self, destination);
        } else {
            destination.store(interp, context, self, interp.getParameter(argIndex));
        }
        return null;
    }

    @Interp
    private void  interpretAsRestArg(InterpreterContext interp, ThreadContext context, IRubyObject self, Operand destination) {
        destination.store(interp, context, self, context.getRuntime().newArray(interp.getParametersFrom(argIndex)));
    }
}
