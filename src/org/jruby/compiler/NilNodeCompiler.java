/*
 * NilNodeCompiler.java
 *
 * Created on January 12, 2007, 12:55 PM
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
public class NilNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of NilNodeCompiler */
    public NilNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadNil();
        
        context.pollThreadEvents();
    }
    
}
