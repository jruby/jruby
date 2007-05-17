/*
 * DAsgnNodeCompiler.java
 *
 * Created on January 31, 2007, 6:27 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.DAsgnNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class DAsgnNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of DAsgnNodeCompiler */
    public DAsgnNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        DAsgnNode dasgnNode = (DAsgnNode)node;
        
        NodeCompilerFactory.getCompiler(dasgnNode.getValueNode()).compile(dasgnNode.getValueNode(), context);
        
        context.assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth());
    }
    
}
