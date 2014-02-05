package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.UnboxedBoolean;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ThreadPollInstr extends Instr implements FixedArityInstr {
    public final boolean onBackEdge;

    public ThreadPollInstr(boolean onBackEdge) {
        super(Operation.THREAD_POLL);
        this.onBackEdge = onBackEdge;
    }

    public ThreadPollInstr() {
        this(false);
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new UnboxedBoolean(onBackEdge) };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return this;
            default:
                // Get rid of non-back-edge thread-poll instructions when scopes are inlined
                return onBackEdge ? this : null;
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ThreadPollInstr(this);
    }
}
