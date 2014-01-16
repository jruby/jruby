/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast;

import java.util.List;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 * @author enebo
 */
public class KeywordArgNode extends Node {
    private AssignableNode assignable;
    
    public KeywordArgNode(ISourcePosition position, AssignableNode assignable) {
        super(position);
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

    @Override
    public NodeType getNodeType() {
        return NodeType.KEYWORDARGNODE;
    }

    public AssignableNode getAssignable() {
        return assignable;
    }
    
}
