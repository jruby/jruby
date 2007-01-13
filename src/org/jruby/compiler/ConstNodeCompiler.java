/*
 * ConstNodeCompiler.java
 *
 * Created on January 12, 2007, 12:15 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.ConstNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class ConstNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of ConstNodeCompiler */
    public ConstNodeCompiler() {
    }

    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        ConstNode constNode = (ConstNode)node;
        
        context.retrieveConstant(constNode.getName());
    }
}
