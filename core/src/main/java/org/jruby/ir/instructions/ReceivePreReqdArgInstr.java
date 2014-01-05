package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

/*
 * Assign Argument passed into scope/method to a result variable
 */
public class ReceivePreReqdArgInstr extends ReceiveArgBase implements FixedArityInstr {
    public ReceivePreReqdArgInstr(Variable result, int argIndex) {
        super(Operation.RECV_PRE_REQD_ARG, result, argIndex);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case NORMAL_CLONE:
                return new ReceivePreReqdArgInstr(ii.getRenamedVariable(result), argIndex);
            default:
                if (ii.canMapArgsStatically()) {
                    return new CopyInstr(ii.getRenamedVariable(result), ii.getArg(argIndex));
                } else {
                    return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), -1, -1, argIndex);
                }
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceivePreReqdArgInstr(this);
    }
}
