package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class ThreadPollInstr extends Instr implements FixedArityInstr {
    public final boolean onBackEdge;

    public ThreadPollInstr(boolean onBackEdge) {
        super(Operation.THREAD_POLL, EMPTY_OPERANDS);
        this.onBackEdge = onBackEdge;
    }

    public ThreadPollInstr() {
        this(false);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        if (ii instanceof SimpleCloneInfo) return new ThreadPollInstr(onBackEdge);

        // Get rid of non-back-edge thread-poll instructions when scopes are inlined
        return onBackEdge ? new ThreadPollInstr(onBackEdge) : null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ThreadPollInstr(this);
    }
}
