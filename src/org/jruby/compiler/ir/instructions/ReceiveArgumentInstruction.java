package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;

/*
 * Assign Argument passed into scope/method to a result variable
 */
public class ReceiveArgumentInstruction extends ReceiveArgBase {
    public ReceiveArgumentInstruction(Variable result, int argIndex) {
        super(Operation.RECV_ARG, result, argIndex);
    }

	 @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, false));
    }

	 @Override
    public Instr cloneForInlinedClosure(InlinerInfo ii) {
		  // SSS FIXME: Temporary, This should become a GetArrayInstr
        return new CopyInstr(ii.getRenamedVariable(result), ii.getBlockArg(argIndex));
    }

    public void compile(JVM jvm) {
        int index = jvm.methodData().local(getResult());
    }
}
