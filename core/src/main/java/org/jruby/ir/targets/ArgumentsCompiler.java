package org.jruby.ir.targets;

public interface ArgumentsCompiler {
    /**
     * Construct a Hash based on keyword arguments pasesd to this method, for use in zsuper
     * <p>
     * Stack required: context, kwargs hash to dup, remaining elements of hash
     *
     * @param length number of element pairs
     */
    public abstract void kwargsHash(int length);
}
