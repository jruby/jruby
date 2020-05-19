package org.jruby.compiler;

/**
 * Represents an object that can produce a JIT-optimizable "constant" version of itself. Currently this is only used
 * by invokedynamic support to avoid creating duplicate MethodHandles.constant handles for the same value.
 */
public interface Constantizable {
    public Object constant();
}
