package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;

/*
 * Assign Argument passed into scope/method to destination Variable
 */
public class ReceiveArgumentInstruction extends NoOperandInstr {
    int argIndex;
    boolean restOfArgArray;

    public ReceiveArgumentInstruction(Variable destination, int argIndex,
            boolean restOfArgArray) {
        super(Operation.RECV_ARG, destination);
        
        this.argIndex = argIndex;
        this.restOfArgArray = restOfArgArray;
    }

    public ReceiveArgumentInstruction(Variable destination, int index) {
        this(destination, index, false);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + (restOfArgArray ? ", ALL" : "") + ")";
    }
}
