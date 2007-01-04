package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;

public class ArgsPushNode extends Node {
    private static final long serialVersionUID = 6442216183136232451L;
    private Node node1;
    private Node node2;
    
    public ArgsPushNode(ISourcePosition position, Node node1, Node node2) {
        super(position, NodeTypes.ARGSPUSHNODE);
        this.node1 = node1;
        this.node2 = node2;
    }

    public Instruction accept(NodeVisitor visitor) {
        return visitor.visitArgsPushNode(this);
    }
    
    public Node getFirstNode() {
        return node1;
    }
    
    public Node getSecondNode() {
        return node2;
    }

    public List childNodes() {
        // TODO Auto-generated method stub
        return null;
    }

}
