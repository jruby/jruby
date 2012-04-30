package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ThreadPollInstr extends Instr {
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
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        // Get rid of non-back-edge thread-poll instructions when scopes are inlined
        return onBackEdge ? this : null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ThreadPollInstr(this);
    }
}
