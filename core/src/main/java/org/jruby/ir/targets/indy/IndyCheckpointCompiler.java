package org.jruby.ir.targets.indy;

import org.jruby.ir.targets.CheckpointCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.runtime.ThreadContext;

import static org.jruby.util.CodegenUtils.sig;

public class IndyCheckpointCompiler implements CheckpointCompiler {
    private final IRBytecodeAdapter compiler;

    public IndyCheckpointCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void checkpoint() {
        compiler.loadContext();
        compiler.adapter.invokedynamic(
                "checkpoint",
                sig(void.class, ThreadContext.class),
                Bootstrap.checkpointHandle());
    }
}
