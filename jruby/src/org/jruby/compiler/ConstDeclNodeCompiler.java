/*
 * ConstDeclNodeCompiler.java
 *
 * Created on April 7, 2007, 1:25 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;

/**
 *
 * @author headius
 */
public class ConstDeclNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of ConstDeclNodeCompiler */
    public ConstDeclNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        ConstDeclNode constDeclNode = (ConstDeclNode)node;
        
        if (constDeclNode.getConstNode() == null) {
            NodeCompilerFactory.getCompiler(constDeclNode.getValueNode()).compile(constDeclNode.getValueNode(), context);
        
            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constDeclNode.nodeId == NodeTypes.COLON2NODE) {
            NodeCompilerFactory.getCompiler(constDeclNode.getConstNode()).compile(constDeclNode.getValueNode(), context);
        
            NodeCompilerFactory.getCompiler(constDeclNode.getValueNode()).compile(constDeclNode.getValueNode(), context);
            
            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            NodeCompilerFactory.getCompiler(constDeclNode.getValueNode()).compile(constDeclNode.getValueNode(), context);
            
            context.assignConstantInObject(constDeclNode.getName());
        }
    }
    
}
