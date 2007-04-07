/*
 * ReturnNodeCompiler.java
 *
 * Created on April 7, 2007, 12:41 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.ReturnNode;

/**
 *
 * @author headius
 */
public class ReturnNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of ReturnNodeCompiler */
    public ReturnNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        ReturnNode returnNode = (ReturnNode)node;
        
        NodeCompilerFactory.getCompiler(returnNode.getValueNode()).compile(returnNode.getValueNode(), context);
        
        context.performReturn();
    }
    
}
