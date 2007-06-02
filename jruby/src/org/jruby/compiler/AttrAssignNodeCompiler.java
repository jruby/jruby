/*
 * AttrAssignNodeCompiler.java
 *
 * Created on March 10, 2007, 12:44 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class AttrAssignNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of AttrAssignNodeCompiler */
    public AttrAssignNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        AttrAssignNode attrAssignNode = (AttrAssignNode)node;
        
        NodeCompilerFactory.getCompiler(attrAssignNode.getReceiverNode()).compile(attrAssignNode.getReceiverNode(), context);
        NodeCompilerFactory.getArgumentsCompiler(attrAssignNode.getArgsNode()).compile(attrAssignNode.getArgsNode(), context);
        
        context.invokeAttrAssign(attrAssignNode.getName());
    }
    
}
