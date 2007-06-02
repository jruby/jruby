/*
 * YieldNodeCompiler.java
 *
 * Created on January 31, 2007, 1:00 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.YieldNode;

/**
 *
 * @author headius
 */
public class YieldNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of YieldNodeCompiler */
    public YieldNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        YieldNode yieldNode = (YieldNode)node;
        
        if (yieldNode.getArgsNode() != null) {
            NodeCompilerFactory.getCompiler(yieldNode.getArgsNode()).compile(yieldNode.getArgsNode(), context);
        }
        
        context.yield(yieldNode.getArgsNode() != null);
    }
}
