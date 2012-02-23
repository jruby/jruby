package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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

    public void compile(JVM jvm) {
        jvm.method().poll();
    }
}
