package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

/*
 * Assign Argument passed into scope/method to destination Variable
 */
public class ReceiveArgumentInstruction extends NoOperandInstr {
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

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new COPY_Instr(ii.getRenamedVariable(_result), ii.getCallArg(argIndex, restOfArgArray));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + (restOfArgArray ? ", ALL" : "") + ")";
    }
}
