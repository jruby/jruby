package org.jruby.ast;

import org.jruby.lexer.yacc.ISourcePosition;

/**
 * A node which behaves like a nil node, but is not actually present in the AST as a syntactical
 * element (e.g. IDE's should ignore occurences of this node.  We have this as seperate subclass
 * so that IDE consumers can more easily ignore these.
 */
public class NilImplicitNode extends NilNode implements InvisibleNode {
    public static final NilImplicitNode NIL = new NilImplicitNode();
    
    public NilImplicitNode() {
        super(ISourcePosition.INVALID_POSITION);
    }
}
