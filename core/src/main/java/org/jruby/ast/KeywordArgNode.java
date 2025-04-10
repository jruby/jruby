/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast;

import java.util.List;
import org.jruby.ast.visitor.NodeVisitor;

public class KeywordArgNode extends Node {
    private final AssignableNode assignable;

    public KeywordArgNode(int line, AssignableNode assignable) {
        super(line, true);
        this.assignable = assignable;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitKeywordArgNode(this);
    }

    @Override
    public List<Node> childNodes() {
        return Node.createList(assignable);
    }

    public int getIndex() {
        return ((IScopedNode) assignable).getIndex();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.KEYWORDARGNODE;
    }

    public AssignableNode getAssignable() {
        return assignable;
    }

}
