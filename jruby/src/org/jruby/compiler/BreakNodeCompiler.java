/*
 * BreakNodeCompiler.java
 *
 * Created on March 8, 2007, 1:33 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.BreakNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class BreakNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of BreakNodeCompiler */
    public BreakNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        BreakNode breakNode = (BreakNode)node;
        
        if (breakNode.getValueNode() != null) {
            NodeCompilerFactory.getCompiler(breakNode.getValueNode()).compile(breakNode.getValueNode(), context);
        } else {
            context.loadNil();
        }
        
        context.issueBreakEvent();
    }

}
