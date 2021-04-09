package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;

import java.util.List;

public class HashPatternNode extends Node {
    private final Node restArg;
    private final HashNode keywordArgs;

    private Node constant;

    public HashPatternNode(int line, Node restArg, HashNode keywordArgs) {
        super(line, false);

        this.restArg = restArg;
        this.keywordArgs = keywordArgs;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitHashPatternNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return createList(restArg, keywordArgs, constant);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.HASHPATTERNNODE;
    }

    public void setConstant(Node constant) {
        this.constant = constant;
    }
}
