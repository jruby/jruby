package org.jruby.ast;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.util.KeyValuePair;

import java.util.ArrayList;
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

    public Node getConstant() {
        return constant;
    }

    public void setConstant(Node constant) {
        this.constant = constant;
    }

    // MRI: args_num in compile.c
    public int getArgumentSize() {
        return keywordArgs == null ? 0 : keywordArgs.getPairs().size();
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

    public boolean hasKeywordArgs() {
        return keywordArgs != null && !keywordArgs.isEmpty() || hasRestArg();
    }

    public HashNode getKeywordArgs() {
        return keywordArgs;
    }

    public Node[] getKeys() {
        return keywordArgs.getKeys();
    }

    public boolean hashNamedKeywordRestArg() {
        return hasRestArg() && !(restArg instanceof StarNode);
    }
}
