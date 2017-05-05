package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * A::B ||= 1
 */
public class OpAsgnConstDeclNode extends Node implements BinaryOperatorNode {
    private Node lhs;
    private ByteList operator;
    private Node rhs;

    public OpAsgnConstDeclNode(ISourcePosition position, Node lhs, ByteList operator, Node rhs) {
        super(position, lhs.containsVariableAssignment() || rhs.containsVariableAssignment());

        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    @Deprecated
    public OpAsgnConstDeclNode(ISourcePosition position, Node lhs, String operator, Node rhs) {
        this(position, lhs, StringSupport.stringAsByteList(operator), rhs);
    }

    @Override
    public Node getFirstNode() {
        return lhs;
    }

    @Override
    public Node getSecondNode() {
        return rhs;
    }

    public String getOperator() {
        return StringSupport.byteListAsString(operator);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitOpAsgnConstDeclNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return createList(lhs, new LiteralNode(getPosition(), operator), rhs);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.OPASGNCONSTDECLNODE;
    }
}
