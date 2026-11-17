package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;

import java.util.List;

import static org.jruby.ast.NodeType.NILRESTARG;

public class NilRestArgNode extends Node {
    public NilRestArgNode(int line) {
        super(line, false);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitNilRestArgNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return EMPTY_LIST;
    }

    @Override
    public NodeType getNodeType() {
        return NILRESTARG;
    }
}
