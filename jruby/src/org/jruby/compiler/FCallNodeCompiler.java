/*
 * FCallNodeCompiler.java
 *
 * Created on January 3, 2007, 2:20 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.FCallNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.Node;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public class FCallNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of FCallNodeCompiler */
    public FCallNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        FCallNode fcallNode = (FCallNode)node;
        
        if (NodeCompilerFactory.SAFE) {
            if (NodeCompilerFactory.UNSAFE_CALLS.contains(fcallNode.getName())) {
                throw new NotCompilableException("Can't compile call safely: " + node);
            }
        }
        
        if (fcallNode.getIterNode() == null) {
            // no block, go for simple version
            if (fcallNode.getArgsNode() != null) {
                // args compiler processes args and results in an args array for invocation
                NodeCompiler argsCompiler = NodeCompilerFactory.getArgumentsCompiler(fcallNode.getArgsNode());
                
                argsCompiler.compile(fcallNode.getArgsNode(), context);

                context.invokeDynamic(fcallNode.getName(), false, true, CallType.FUNCTIONAL, null, false);
            } else {
                context.invokeDynamic(fcallNode.getName(), false, false, CallType.FUNCTIONAL, null, false);
            }
        } else {
            // FIXME: Missing blockpasnode stuff here
            
            final IterNode iterNode = (IterNode) fcallNode.getIterNode();
            
            final ClosureCallback closureArg = new ClosureCallback() {
                public void compile(Compiler context) {
                    NodeCompilerFactory.getCompiler(iterNode).compile(iterNode, context);
                }
            };

            if (fcallNode.getArgsNode() != null) {
                // args compiler processes args and results in an args array for invocation
                NodeCompiler argsCompiler = NodeCompilerFactory.getArgumentsCompiler(fcallNode.getArgsNode());
                
                argsCompiler.compile(fcallNode.getArgsNode(), context);
                
                context.invokeDynamic(fcallNode.getName(), false, true, CallType.FUNCTIONAL, closureArg, false);
            } else {
                context.invokeDynamic(fcallNode.getName(), false, false, CallType.FUNCTIONAL, closureArg, false);
            }
        }
    }
    
}
