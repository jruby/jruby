package org.jruby.ir.targets.simple;

import org.jruby.ir.targets.CheckpointCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.runtime.ThreadContext;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class NormalCheckpointCompiler implements CheckpointCompiler {
    private final IRBytecodeAdapter compiler;

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
