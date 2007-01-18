/*
 * BeginNodeCompiler.java
 *
 * Created on January 18, 2007, 12:24 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.BeginNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class BeginNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of BeginNodeCompiler */
    public BeginNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        BeginNode beginNode = (BeginNode)node;
        
        NodeCompilerFactory.getCompiler(beginNode.getBodyNode()).compile(beginNode.getBodyNode(), context);
    }
    
}
