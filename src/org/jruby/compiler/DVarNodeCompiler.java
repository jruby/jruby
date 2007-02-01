/*
 * DVarNodeCompiler.java
 *
 * Created on January 31, 2007, 6:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.DVarNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class DVarNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of DVarNodeCompiler */
    public DVarNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        DVarNode dvarNode = (DVarNode)node;
        
        context.retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
    }
    
}
