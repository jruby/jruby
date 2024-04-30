package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;

import java.util.List;

public class InNode extends Node {
    private final Node expression;
    private final Node body;
    private final Node nextCase; // InNode or whatever is an else branch.

    public InNode(int line, Node expr, Node body, Node nextCase) {
        super(line, expr.containsVariableAssignment());

        this.expression = expr;
        this.body = body;
        this.nextCase = nextCase;
    }

    public Node getExpression() {
        return expression;
    }

    public Node getBody() {
        return body;
    }

    public Node getNextCase() {
        return nextCase;
    }

    // {a: 1} => {a: b} has no body other cases will (arg in will have false and p_case_body is non-null).
    public boolean isSinglePattern() {
        return getBody() == null;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return createList(expression, body, nextCase);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.INNODE;
    }
}
