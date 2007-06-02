/*
 * SValueNodeCompiler.java
 *
 * Created on January 31, 2007, 10:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class MultipleAsgnNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of SValueNodeCompiler */
    public MultipleAsgnNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode)node;
        
        NodeCompilerFactory.getCompiler(multipleAsgnNode.getValueNode()).compile(multipleAsgnNode.getValueNode(), context);
        
        NodeCompilerFactory.getAssignmentCompiler(multipleAsgnNode).compile(multipleAsgnNode, context);
    }
}
