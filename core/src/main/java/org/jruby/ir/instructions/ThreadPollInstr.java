package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.Profiler;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(onBackEdge);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        if (IRRuntimeHelpers.inProfileMode()) Profiler.clockTick();
        context.callThreadPoll();
        return null;
    }
}
