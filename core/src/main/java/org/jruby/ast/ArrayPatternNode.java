package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;

import java.util.List;

public class ArrayPatternNode extends Node {
    private final ListNode preArgs;
    private final Node restArg;
    private final ListNode postArgs;

    private Node constant;

    public ArrayPatternNode(int line, ListNode preArgs, Node restArg, ListNode postArgs) {
        super(line, false);

        this.preArgs = preArgs;
        this.restArg = restArg;
        this.postArgs = postArgs;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitArrayPatternNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return createList(preArgs, restArg, postArgs, constant);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ARRAYPATTERNNODE;
    }

    public void setConstant(Node constant) {
        this.constant = constant;
    }
}
