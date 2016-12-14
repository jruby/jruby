package org.jruby.truffle.parser.ast;

/**
 * A marker interface for scoped variables (which have an offset and depth).
 */
public interface IScopedNode {
    /**
     * How many scopes down we should look for this variable
     */
    public int getDepth();

    /**
     * Which index (or slot) this variable is located at in the scope it is stored in.
     */
    public int getIndex();
}
