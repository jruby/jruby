/*
 * InstVarNodeCompiler.java
 *
 * Created on January 12, 2007, 1:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.InstVarNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class InstVarNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of InstVarNodeCompiler */
    public InstVarNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        InstVarNode instVarNode = (InstVarNode)node;
        
        context.retrieveInstanceVariable(instVarNode.getName());
    }
    
}
