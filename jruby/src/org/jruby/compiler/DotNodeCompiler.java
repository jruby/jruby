/*
 * DotNodeCompiler.java
 *
 * Created on March 9, 2007, 11:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.DotNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class DotNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of DotNodeCompiler */
    public DotNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        DotNode dotNode = (DotNode)node;

        NodeCompilerFactory.getCompiler(dotNode.getBeginNode()).compile(dotNode.getBeginNode(), context);
        NodeCompilerFactory.getCompiler(dotNode.getEndNode()).compile(dotNode.getEndNode(), context);
        
        context.createNewRange(dotNode.isExclusive());
    }
    
}
