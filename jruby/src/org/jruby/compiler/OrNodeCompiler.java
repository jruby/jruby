/*
 * OrNodeCompiler.java
 *
 * Created on January 12, 2007, 1:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.OrNode;

/**
 *
 * @author headius
 */
public class OrNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of OrNodeCompiler */
    public OrNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final OrNode orNode = (OrNode)node;
        
        NodeCompilerFactory.getCompiler(orNode.getFirstNode()).compile(orNode.getFirstNode(), context);
        
        BranchCallback longCallback = new BranchCallback() {
            public void branch(Compiler context) {
                NodeCompilerFactory.getCompiler(orNode.getSecondNode()).compile(orNode.getSecondNode(), context);
            }
        };
        
        context.performLogicalOr(longCallback);
    }
}
