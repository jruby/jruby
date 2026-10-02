package org.jruby.ast;

/**
 * A node which behaves like a nil node, but is not actually present in the AST as a syntactical
 * element (e.g. IDE's should ignore occurrences of this node. We have this as separate subclass
 * so that IDE consumers can more easily ignore these.
 */
public class NilImplicitNode extends NilNode implements InvisibleNode {
    public static final NilImplicitNode NIL = new NilImplicitNode();

    @Override
    public void setAutoSourceSpan(long start, long end) {
        // shared singleton: it has no position of its own
    }
    
    public NilImplicitNode() {
        super(-1);
    }

    public boolean isNil() {
        return true;
    }
}
