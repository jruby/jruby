/*
 * SplatNodeCompiler.java
 *
 * Created on January 31, 2007, 10:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.SplatNode;

/**
 *
 * @author headius
 */
public class SplatNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of SplatNodeCompiler */
    public SplatNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        SplatNode splatNode = (SplatNode)node;
        
        NodeCompilerFactory.getCompiler(splatNode.getValue()).compile(splatNode.getValue(), context);
        
        context.splatCurrentValue();
    }
    
}
