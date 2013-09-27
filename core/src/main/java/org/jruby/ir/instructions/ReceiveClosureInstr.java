package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.InlinerInfo;

/* Receive the closure argument (either implicit or explicit in Ruby source code) */
public class ReceiveClosureInstr extends Instr implements ResultInstr {
    private Variable result;

    public ReceiveClosureInstr(Variable result) {
        super(Operation.RECV_CLOSURE);

        assert result != null: "ReceiveClosureInstr result is null";

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
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
        // operand type that converts the static code block to a proc which is a closure.
        if (ii.getCallClosure() instanceof WrappedIRClosure) return NopInstr.NOP;
        else return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ReceiveClosureInstr(ii.getRenamedVariable(result));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveClosureInstr(this);
    }
}
