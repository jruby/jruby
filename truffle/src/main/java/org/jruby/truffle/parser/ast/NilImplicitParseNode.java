package org.jruby.truffle.parser.ast;

/**
 * A node which behaves like a nil node, but is not actually present in the AST as a syntactical
 * element (e.g. IDE's should ignore occurences of this node.  We have this as seperate subclass
 * so that IDE consumers can more easily ignore these.
 */
public class NilImplicitParseNode extends NilParseNode implements InvisibleNode {
    public static final NilImplicitParseNode NIL = new NilImplicitParseNode();
    
    public NilImplicitParseNode() {
        super(null);
    }

    public boolean isNil() {
        return true;
    }
}
