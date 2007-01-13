/*
 * InstAsgnNodeCompiler.java
 *
 * Created on January 12, 2007, 1:43 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class InstAsgnNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of InstAsgnNodeCompiler */
    public InstAsgnNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        InstAsgnNode instAsgnNode = (InstAsgnNode)node;
        
        NodeCompilerFactory.getCompiler(instAsgnNode.getValueNode()).compile(instAsgnNode.getValueNode(), context);
        context.assignInstanceVariable(instAsgnNode.getName());
    }
}
