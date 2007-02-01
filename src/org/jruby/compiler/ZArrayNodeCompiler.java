/*
 * ZArrayNodeCompiler.java
 *
 * Created on January 31, 2007, 6:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class ZArrayNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of ZArrayNodeCompiler */
    public ZArrayNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        context.createEmptyArray();
    }
    
}
