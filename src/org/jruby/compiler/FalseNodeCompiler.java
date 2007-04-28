/*
 * FalseNodeCompiler.java
 *
 * Created on January 12, 2007, 12:57 PM
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
public class FalseNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of FalseNodeCompiler */
    public FalseNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadFalse();

        context.pollThreadEvents();
    }
    
}
