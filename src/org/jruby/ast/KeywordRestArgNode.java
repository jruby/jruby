package org.jruby.ast;

import java.util.List;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 */
public class KeywordRestArgNode extends Node {
    private Node variable;
    
    public KeywordRestArgNode(ISourcePosition position, Node variable) {
        super(position);
        
        this.variable = variable;
    }
    
    @Override
    public Object accept(NodeVisitor visitor) {
        return visitor.visitKeywordRestArgNode(this);
    }
    
    @Override
    public List<Node> childNodes() {
        return Node.createList(variable);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.KEYWORDRESTARGNODE;
    }
    
    public Node getVariable() {
        return variable;
    }
}
