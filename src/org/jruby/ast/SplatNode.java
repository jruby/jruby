package org.jruby.ast;

import org.ablaf.ast.INode;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;


public class SplatNode extends AbstractNode {
    static final long serialVersionUID = -1649004231006940340L;
    private INode node;

    public SplatNode(ISourcePosition position, INode node) {
        super(position);
        this.node = node;
    }

    public void accept(INodeVisitor visitor) {
        ((NodeVisitor)visitor).visitSplatNode(this);
    }
    
    public INode getValue() {
        return node;
    }
}
