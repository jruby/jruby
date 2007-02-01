/*
 * SValueNodeCompiler.java
 *
 * Created on January 31, 2007, 10:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.SValueNode;

/**
 *
 * @author headius
 */
public class SValueNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of SValueNodeCompiler */
    public SValueNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        SValueNode svalueNode = (SValueNode)node;
        
        NodeCompilerFactory.getCompiler(svalueNode.getValue()).compile(svalueNode.getValue(), context);
        
        context.singlifySplattedValue();
    }
    
}
