package org.jruby.ast;

import java.util.List;

/*
 * Duby type extension for methods
 */
public class TypedArgumentNode extends ArgumentNode {
    private Node typeNode;
    
    public TypedArgumentNode(ArgumentNode argNode,  Node typeNode) {
        // getIndex should be ok since this is all called at a method local scope.
        super(argNode.getPosition(), argNode.getName(), argNode.getIndex());
        
        this.typeNode = typeNode;
    }
    
    public Node getTypeNode() {
        return typeNode;
    }
    
    @Override
    public List<Node> childNodes() {
        return createList(typeNode);
    }
}
