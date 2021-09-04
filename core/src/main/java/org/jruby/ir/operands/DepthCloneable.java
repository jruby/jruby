package org.jruby.ir.operands;

/**
 * For Operands which can be accessed from nested scopes we sometimes need to adjust them
 * for different depths.
 */
public interface DepthCloneable {
    /**
     * How deep is this operand from where it is defined?
     */
    public Operand cloneForDepth(int n);
}
