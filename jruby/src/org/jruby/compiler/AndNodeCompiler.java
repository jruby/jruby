/*
 * AndNodeCompiler.java
 *
 * Created on January 12, 2007, 12:38 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.AndNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class AndNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of AndNodeCompiler */
    public AndNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final AndNode andNode = (AndNode)node;
        
        NodeCompilerFactory.getCompiler(andNode.getFirstNode()).compile(andNode.getFirstNode(), context);
        
        BranchCallback longCallback = new BranchCallback() {
            public void branch(Compiler context) {
                NodeCompilerFactory.getCompiler(andNode.getSecondNode()).compile(andNode.getSecondNode(), context);
            }
        };
        
        context.performLogicalAnd(longCallback);
    }
}
