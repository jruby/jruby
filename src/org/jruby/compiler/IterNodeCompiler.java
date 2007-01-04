/*
 * IterNodeCompiler.java
 *
 * Created on January 3, 2007, 11:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.BlockAcceptingNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class IterNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of IterNodeCompiler */
    public IterNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final IterNode iterNode = (IterNode)node;
        
        if (iterNode.getIterNode() instanceof BlockAcceptingNode) {
            ((BlockAcceptingNode)iterNode.getIterNode()).setIterNode(iterNode);
            
            NodeCompilerFactory.getCompiler(iterNode.getIterNode()).compile(iterNode.getIterNode(), context);
        }
        
        // FIXME: handle other iternode cases
    }
    
}
