package org.jruby.compiler.ir.util;

/**
 * Thrown when asking for a vertex which does not exist.
 */
public class NoSuchVertexException extends Exception {
    private Object data;
    
    public NoSuchVertexException(Object data) {
        super();
    }
    
    public Object getData() {
        return data;
    }
}