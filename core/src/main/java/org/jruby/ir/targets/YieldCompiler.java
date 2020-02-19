package org.jruby.ir.targets;

public interface YieldCompiler {
    /**
     * Yield argument list to a block.
     * <p>
     * Stack required: context, block, argument
     */
    public abstract void yield(boolean unwrap);

    /**
     * Yield to a block.
     * <p>
     * Stack required: context, block
     */
    public abstract void yieldSpecific();

    /**
     * Yield a number of flat arguments to a block.
     * <p>
     * Stack required: context, block
     */
    public abstract void yieldValues(int arity);
}
