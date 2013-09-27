package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ReceiveSelfInstr extends Instr implements ResultInstr {
    private Variable result;

    // SSS FIXME: destination always has to be a local variable '%self'.  So, is this a redundant arg?
    public ReceiveSelfInstr(Variable result) {
        super(Operation.RECV_SELF);

        assert result != null: "ReceiveSelfInstr result is null";

        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // receive-self will disappear after inlining
        // all uses of %self will be replaced by the call receiver
        return null;
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return this;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveSelfInstr(this);
    }
}
