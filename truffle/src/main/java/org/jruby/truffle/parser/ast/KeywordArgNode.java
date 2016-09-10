/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.ast.visitor.NodeVisitor;
import org.jruby.truffle.parser.lexer.yacc.ISourcePosition;

import java.util.List;

/**
 *
 * @author enebo
 */
public class KeywordArgNode extends Node {
    private AssignableNode assignable;

    public KeywordArgNode(ISourcePosition position, AssignableNode assignable) {
        super(position, true);
        this.assignable = assignable;
    }

    @Override
    public Object accept(NodeVisitor visitor) {
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
