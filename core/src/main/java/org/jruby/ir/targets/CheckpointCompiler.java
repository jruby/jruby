package org.jruby.ir.targets;

public interface CheckpointCompiler {
    /**
     * Perform a thread event checkpoint.
     * <p>
     * Stack required: none
     */
    public abstract void checkpoint();
}
