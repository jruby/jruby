/*
 * IfNodeCompiler.java
 *
 * Created on January 3, 2007, 4:42 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.IfNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class IfNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of IfNodeCompiler */
    public IfNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final IfNode ifNode = (IfNode)node;
        
        NodeCompilerFactory.getCompiler(ifNode.getCondition()).compile(ifNode.getCondition(), context);
        
        BranchCallback trueCallback = new BranchCallback() {
            public void branch(Compiler context) {
                NodeCompilerFactory.getCompiler(ifNode.getThenBody()).compile(ifNode.getThenBody(), context);
            }
        };
        
        BranchCallback falseCallback = new BranchCallback() {
            public void branch(Compiler context) {
                if (ifNode.getElseBody() != null) {
                    NodeCompilerFactory.getCompiler(ifNode.getThenBody()).compile(ifNode.getElseBody(), context);
                }
            }
        };
        
        context.performBooleanBranch(trueCallback, falseCallback);
    }
    
}
