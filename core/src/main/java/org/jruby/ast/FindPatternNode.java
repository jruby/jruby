package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;

import java.util.List;

public class FindPatternNode extends Node {
    private final Node preRestArg;
    private final ListNode args;
    private final Node postRestArg;
    private Node constant;

    public FindPatternNode(int line, Node preRestArg, ListNode args, Node postRestArg) {
        super(line, false);

        this.preRestArg = preRestArg;
        this.args = args;
        this.postRestArg = postRestArg;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitFindPatternNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return createList(preRestArg, args, postRestArg, constant);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.FINDPATTERNNODE;
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

    public Node[] getArgs() {
        return args.children();
    }

    public Node getPreRestArg() {
        return preRestArg;
    }

    public Node getPostRestArg() {
        return postRestArg;
    }
}
