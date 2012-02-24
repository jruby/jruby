package org.jruby.compiler.ir.util;

/**
 * Directed graph toString output and just general identification should
 * also implement DataInfo.
 */
public interface ExplicitVertexID {
    public int getID();
}
