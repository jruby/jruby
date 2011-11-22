package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

/*
 * Assign Argument passed into scope/method to a result variable
 */
public class ReceiveArgumentInstruction extends ReceiveArgBase {
    public ReceiveArgumentInstruction(Variable result, int argIndex) {
        super(Operation.RECV_ARG, result, argIndex);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, false));
    }
}
