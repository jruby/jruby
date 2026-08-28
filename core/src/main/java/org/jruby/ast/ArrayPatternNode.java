package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;

import java.util.List;

public class ArrayPatternNode extends Node {
    private ListNode preArgs;
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

    public boolean hasConstant() {
        return constant != null;
    }

    public Node getConstant() {
        return constant;
    }

    public Node[] getPre() {
        return preArgs == null ? null : preArgs.children();
    }

    public ListNode getPreArgs() {
        return preArgs;
    }

    public Node[] getPost() {
        return postArgs == null ? null : postArgs.children();
    }

    public ListNode getPostArgs() {
        return postArgs;
    }

    public void setPreArgs(ListNode preArgs) {
        this.preArgs = preArgs;
    }

    public Node getRestArg() {
        return restArg;
    }

    public boolean hasRestArg() {
        return restArg != null;
    }

    public boolean isNamedRestArg() {
        return !(restArg instanceof StarNode);
    }

    public boolean usesRestNum() {
        if (restArg == null) return false;

        boolean named = !(restArg instanceof StarNode);

        return named || !named && postArgsNum() > 0;
    }

    public int preArgsNum() {
        return preArgs == null ? 0 : preArgs.size();
    }

    public int postArgsNum() {
        return postArgs == null ? 0 : postArgs.size();
    }

    public int minimumArgsNum() {
        return preArgsNum() + postArgsNum();
    }
}
