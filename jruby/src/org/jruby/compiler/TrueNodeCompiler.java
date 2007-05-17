/*
 * TrueNodeCompiler.java
 *
 * Created on January 12, 2007, 12:58 PM
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
public class TrueNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of TrueNodeCompiler */
    public TrueNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadTrue();
        
        context.pollThreadEvents();
    }
    
}
