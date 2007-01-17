/*
 * NotNodeCompiler.java
 *
 * Created on January 17, 2007, 3:17 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.NotNode;

/**
 *
 * @author headius
 */
public class NotNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of NotNodeCompiler */
    public NotNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        NotNode notNode = (NotNode)node;
        
        NodeCompilerFactory.getCompiler(notNode.getConditionNode()).compile(notNode.getConditionNode(), context);
        
        context.negateCurrentValue();
    }
    
}
