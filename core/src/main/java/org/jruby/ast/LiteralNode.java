package org.jruby.ast;

import java.util.List;


import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * This is not a node in the classic sense in that it has no defined or
 * interpret method which can be called.  It just stores the position of
 * the literal and the name/value of the literal.  We made it a node so that
 * the parser needs to work less hard in its productions.  dynamic literals
 * are nodes and by having literals also be nodes means they have a common
 * subtype which is not Object.
 */
public class LiteralNode extends Node implements InvisibleNode {
    private ByteList name;

    public LiteralNode(ISourcePosition position, ByteList name) {
        super(position, false);
        this.name = name;
    }

    @Deprecated
    public LiteralNode(ISourcePosition position, String name) {
        this(position, StringSupport.stringAsByteList(name));
    }

    public String getName() {
        return StringSupport.byteListAsString(name);
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
