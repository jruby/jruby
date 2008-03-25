/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

import java.util.List;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 *
 * @author enebo
 */
public class TypedArgumentNode extends ArgumentNode {
    private Node typeNode;
    
    public TypedArgumentNode(ISourcePosition position, String identifier, Node typeNode) {
        super(position, identifier);
        
        this.typeNode = typeNode;
    }
    
    public Node getTypeNode() {
        return typeNode;
    }
    
    public List<Node> childNodes() {
        return createList(typeNode);
    }
}
