package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.ReqdArgMultipleAsgnInstr;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;

/*
 * Assign Argument passed into scope/method to a result variable
 */
public class ReceivePreReqdArgInstr extends ReceiveArgBase {
    public ReceivePreReqdArgInstr(Variable result, int argIndex) {
        super(Operation.RECV_PRE_REQD_ARG, result, argIndex);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        if (ii.canMapArgsStatically()) {
            return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, false));
        } else {
            return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgsArray(), -1, -1, argIndex);
        }
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ReceivePreReqdArgInstr(ii.getRenamedVariable(result), argIndex);
    }

    @Override
    public Instr cloneForInlinedClosure(InlinerInfo ii) {
        return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getYieldArg(), -1, -1, argIndex);
    }

    public void compile(JVM jvm) {
        int index = jvm.methodData().local(getResult());
    }
}
