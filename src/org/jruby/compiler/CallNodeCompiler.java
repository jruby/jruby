/*
 * CallNodeCompiler.java
 *
 * Created on January 3, 2007, 4:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.CallNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.Node;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public class CallNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of CallNodeCompiler */
    public CallNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final CallNode callNode = (CallNode)node;
        
        if (NodeCompilerFactory.SAFE) {
            if (NodeCompilerFactory.UNSAFE_CALLS.contains(callNode.getName())) {
                throw new NotCompilableException("Can't compile call safely: " + node);
            }
        }
        
        ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(Compiler context) {
                NodeCompilerFactory.getCompiler(callNode.getReceiverNode()).compile(callNode.getReceiverNode(), context);
            }
        };
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(Compiler context) {
                NodeCompiler argsCompiler = NodeCompilerFactory.getArgumentsCompiler(callNode.getArgsNode());
                
                argsCompiler.compile(callNode.getArgsNode(), context);
            }
        };
                
        if (callNode.getIterNode() == null) {
            // no block, go for simple version
            if (callNode.getArgsNode() != null) {
                context.invokeDynamic(callNode.getName(), receiverCallback, argsCallback, CallType.NORMAL, null, false);
            } else {
                context.invokeDynamic(callNode.getName(), receiverCallback, null, CallType.NORMAL, null, false);
            }
        } else {
            // FIXME: Missing blockpassnode handling
            final IterNode iterNode = (IterNode) callNode.getIterNode();
            
            final ClosureCallback closureArg = new ClosureCallback() {
                public void compile(Compiler context) {
                    NodeCompilerFactory.getCompiler(iterNode).compile(iterNode, context);
                }
            };
            
            if (callNode.getArgsNode() != null) {
                context.invokeDynamic(callNode.getName(), receiverCallback, argsCallback, CallType.NORMAL, closureArg, false);
            } else {
                context.invokeDynamic(callNode.getName(), receiverCallback, null, CallType.NORMAL, closureArg, false);
            }
        }
    }
    
}
