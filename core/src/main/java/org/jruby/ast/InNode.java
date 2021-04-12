package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;

import java.util.List;

public class InNode extends Node {
    private final Node expr;
    private final Node body;
    private final Node nextCase; // InNode or whatever is an else branch.

    public InNode(int line, Node expr, Node body, Node nextCase) {
        super(line, expr.containsVariableAssignment());

        this.expr = expr;
        this.body = body;
        this.nextCase = nextCase;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return createList(expr, body, nextCase);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.INNODE;
    }
}
