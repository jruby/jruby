/*
 * IterNodeCompiler.java
 *
 * Created on January 3, 2007, 11:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
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
                    assign(iterNode.getVarNode(), 0, context);
                }
            }
        };
        
        context.createNewClosure(iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(), closureBody, closureArgs);
    }
    
    private void assign(Node node, int index, Compiler context) {
        switch (node.nodeId) {
//        case NodeTypes.ATTRASSIGNNODE:
//            attrAssignNode(runtime, context, self, node, value, block);
//            break;
//        case NodeTypes.CALLNODE:
//            callNode(runtime, context, self, node, value, block);
//            break;
//        case NodeTypes.CLASSVARASGNNODE:
//            classVarAsgnArgCompile(node, index, context);
//            break;
//        case NodeTypes.CLASSVARDECLNODE:
//            classVarDeclArgCompile(node, index, context);
//            break;
//        case NodeTypes.CONSTDECLNODE:
//            constDeclNode(runtime, context, self, node, value, block);
//            break;
        case NodeTypes.DASGNNODE:
            dasgnArgCompile(node, index, context);
            break;
        case NodeTypes.GLOBALASGNNODE:
            globalAsgnArgCompile(node, index, context);
            break;
        case NodeTypes.INSTASGNNODE:
            instAsgnArgCompile(node, index, context);
            break;
        case NodeTypes.LOCALASGNNODE:
            localAsgnArgCompile(node, index, context);
            break;
//        case NodeTypes.MULTIPLEASGNNODE:
//            result = multipleAsgnNode(runtime, context, self, node, value, check);
//            break;
        default:
            throw new NotCompilableException("Can't compile node: " + node);
        }
    }
    
    private void dasgnArgCompile(Node node,int argIndex, Compiler context) {
        DAsgnNode dasgnNode = (DAsgnNode)node;
        
        context.assignLocalVariableBlockArg(argIndex, dasgnNode.getIndex(), dasgnNode.getDepth());
    }
    
    private void localAsgnArgCompile(Node node,int argIndex, Compiler context) {
        LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
        
        context.assignLocalVariableBlockArg(argIndex, localAsgnNode.getIndex(), localAsgnNode.getDepth());
    }
    
    private void instAsgnArgCompile(Node node,int argIndex, Compiler context) {
        InstAsgnNode instAsgnNode = (InstAsgnNode)node;
        
        context.assignInstanceVariableBlockArg(argIndex,instAsgnNode.getName());
    }
    
    private void globalAsgnArgCompile(Node node,int argIndex, Compiler context) {
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
        
        context.assignGlobalVariableBlockArg(argIndex,globalAsgnNode.getName());
    }
    
}
