/*
 * IterNodeCompiler.java
 *
 * Created on January 3, 2007, 11:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.IterNode;
import org.jruby.ast.Node;
import org.jruby.runtime.Arity;

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

        // create the closure class and instantiate it
        final ClosureCallback closureBody = new ClosureCallback() {
            public void compile(Compiler context) {
                if (iterNode.getBodyNode() != null) {
                    NodeCompilerFactory.getCompiler(iterNode.getBodyNode()).compile(iterNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };

        // create the closure class and instantiate it
        final ClosureCallback closureArgs = new ClosureCallback() {
            public void compile(Compiler context) {
                if (iterNode.getVarNode() != null) {
                    AssignmentCompiler.assign(iterNode.getVarNode(), 0, context);
                }
            }
        };
        
        context.createNewClosure(iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(), closureBody, closureArgs);
    }
    
}
