/*
 * AssignmentCompiler.java
 *
 * Created on April 3, 2007, 2:58 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;

/**
 *
 * @author headius
 */
public class AssignmentCompiler {
    public static void assign(Node node, int index, Compiler context) {
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
//            multipleAsgnNodeCompile(node, index, context);
//            break;
        default:
            throw new NotCompilableException("Can't compile node: " + node);
        }
    }
    
    private static void dasgnArgCompile(Node node,int argIndex, Compiler context) {
        DAsgnNode dasgnNode = (DAsgnNode)node;
        
        context.assignLocalVariableBlockArg(argIndex, dasgnNode.getIndex(), dasgnNode.getDepth());
    }
    
    private static void localAsgnArgCompile(Node node,int argIndex, Compiler context) {
        LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
        
        context.assignLocalVariableBlockArg(argIndex, localAsgnNode.getIndex(), localAsgnNode.getDepth());
    }
    
    private static void instAsgnArgCompile(Node node,int argIndex, Compiler context) {
        InstAsgnNode instAsgnNode = (InstAsgnNode)node;
        
        context.assignInstanceVariableBlockArg(argIndex,instAsgnNode.getName());
    }
    
    private static void globalAsgnArgCompile(Node node,int argIndex, Compiler context) {
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
        
        context.assignGlobalVariableBlockArg(argIndex,globalAsgnNode.getName());
    }
}
