/*
 * WhileNodeCompiler.java
 *
 * Created on January 3, 2007, 5:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.WhileNode;

/**
 *
 * @author headius
 */
public class WhileNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of WhileNodeCompiler */
    public WhileNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final WhileNode whileNode = (WhileNode)node;
        
        BranchCallback condition = new BranchCallback() {
            public void branch(Compiler context) {
                NodeCompilerFactory.getCompiler(whileNode.getConditionNode()).compile(whileNode.getConditionNode(), context);
            }
        };
        
        BranchCallback body = new BranchCallback() {
            public void branch(Compiler context) {
                NodeCompilerFactory.getCompiler(whileNode.getBodyNode()).compile(whileNode.getBodyNode(), context);
            }
        };
        
        context.performBooleanLoop(condition, body, whileNode.evaluateAtStart());
    }
    
}
