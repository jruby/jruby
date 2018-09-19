package org.jruby.ast;

import java.util.List;


import org.jruby.RubySymbol;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.util.ByteList;

/**
 * This is not a node in the classic sense in that it has no defined or
 * interpret method which can be called.  It just stores the position of
 * the literal and the name/value of the literal.  We made it a node so that
 * the parser needs to work less hard in its productions.  dynamic literals
 * are nodes and by having literals also be nodes means they have a common
 * subtype which is not Object.
 *
 * Used by alias and undef.
 */
public class LiteralNode extends Node implements InvisibleNode {
    private RubySymbol name;

    public LiteralNode(ISourcePosition position, RubySymbol name) {
        super(position, false);
        this.name = name;
    }

    public String getName() {
        return name.asJavaString();
    }

    public ByteList getByteName() {
        return name.getBytes();
    }

    public RubySymbol getSymbolName() {
        return name;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitLiteralNode(this);
    }

    public List<Node> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.LITERALNODE;
    }

}
