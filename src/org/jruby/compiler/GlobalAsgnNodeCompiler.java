/*
 * GlobalAsgnNodeCompiler.java
 *
 * Created on January 17, 2007, 2:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class GlobalAsgnNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of GlobalAsgnNodeCompiler */
    public GlobalAsgnNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
        
        NodeCompilerFactory.getCompiler(globalAsgnNode.getValueNode()).compile(globalAsgnNode.getValueNode(), context);
        
        context.assignGlobalVariable(globalAsgnNode.getName());
    }
    
}
