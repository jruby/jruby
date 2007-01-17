/*
 * GlobalVarNodeCompiler.java
 *
 * Created on January 17, 2007, 3:13 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class GlobalVarNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of GlobalVarNodeCompiler */
    public GlobalVarNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        GlobalVarNode globalVarNode = (GlobalVarNode)node;
        
        context.retrieveGlobalVariable(globalVarNode.getName());
    }
    
}
