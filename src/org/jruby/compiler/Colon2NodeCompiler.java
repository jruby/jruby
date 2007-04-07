/*
 * Colon2NodeCompiler.java
 *
 * Created on April 7, 2007, 1:30 AM
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
public class Colon2NodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of Colon2NodeCompiler */
    public Colon2NodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        throw new NotCompilableException("Can't compile node: " + node);
    }
    
}
