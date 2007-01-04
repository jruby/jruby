/*
 * DefnNodeCompiler.java
 *
 * Created on January 4, 2007, 2:07 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class DefnNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of DefnNodeCompiler */
    public DefnNodeCompiler() {
        
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final DefnNode defnNode = (DefnNode)node;
        
        ClosureCallback body = new ClosureCallback() {
            public void compile(Compiler context) {
                NodeCompilerFactory.getCompiler(defnNode.getBodyNode()).compile(defnNode.getBodyNode(), context);
            }
        };
        
        int arity = 0;
        
        // FIXME: simple arity check assumes simple arg list; expand to support weirder arg lists
        arity = ((ArgsNode)defnNode.getArgsNode()).getArgsCount();
        
        context.defineNewMethod(defnNode.getName(), arity, defnNode.getScope().getNumberOfVariables(), body);
    }
    
}
