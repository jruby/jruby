package org.jruby.ast;

import java.util.List;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * A::B ||= 1
 */
public class OpAsgnConstDeclNode extends Node implements BinaryOperatorNode {
    private Node lhs;
    private String operator;
    private Node rhs;

    public OpAsgnConstDeclNode(ISourcePosition position, Node lhs, String operator, Node rhs) {
        super(position, lhs.containsVariableAssignment() || rhs.containsVariableAssignment());

        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
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
        return operator;
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
