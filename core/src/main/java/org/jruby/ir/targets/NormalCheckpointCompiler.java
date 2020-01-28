package org.jruby.ir.targets;

import org.jruby.runtime.ThreadContext;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

class NormalCheckpointCompiler implements CheckpointCompiler {
    private IRBytecodeAdapter compiler;

    public NormalCheckpointCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void checkpoint() {
        compiler.loadContext();
        compiler.adapter.invokevirtual(
                p(ThreadContext.class),
                "callThreadPoll",
                sig(void.class));
    }
}
