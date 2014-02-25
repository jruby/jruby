package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.InlinerInfo;

/* Receive the closure argument (either implicit or explicit in Ruby source code) */
public class ReceiveClosureInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;

    public ReceiveClosureInstr(Variable result) {
        super(Operation.RECV_CLOSURE);

        assert result != null: "ReceiveClosureInstr result is null";

        this.result = result;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.RECEIVES_CLOSURE_ARG);
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new ReceiveClosureInstr(ii.getRenamedVariable(result));
            case METHOD_INLINE:
            case CLOSURE_INLINE:
                // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
                // operand type that converts the static code block to a proc which is a closure.
                if (ii.getCallClosure() instanceof WrappedIRClosure) return NopInstr.NOP;
                else return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
            default:
                // Should not get here
                return super.cloneForInlining(ii);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveClosureInstr(this);
    }
}
