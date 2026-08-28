package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.parser.ProductionState;

import java.util.List;

public class ErrorNode extends Node {
    ProductionState loc;
    // FIXME: Perhaps we can store real location object and not just leak production state.
    public ErrorNode(ProductionState loc) {
        super(loc.start(), false);

        this.loc = loc;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return null;
    }

    @Override
    public List<Node> childNodes() {
        return null;
    }

    @Override
    public NodeType getNodeType() {
        return null;
    }
}
