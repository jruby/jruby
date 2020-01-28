package org.jruby.ir.targets;

import org.jruby.runtime.ThreadContext;

import static org.jruby.util.CodegenUtils.sig;

class IndyCheckpointCompiler implements CheckpointCompiler {
    private IRBytecodeAdapter compiler;

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
