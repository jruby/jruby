/*
 * ModuleNodeCompiler.java
 *
 * Created on April 24, 2007, 1:20 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class ModuleNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of ModuleNodeCompiler */
    public ModuleNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final ModuleNode moduleNode = (ModuleNode)node;
        
        final Node cpathNode = moduleNode.getCPath();
        
        ClosureCallback bodyCallback = new ClosureCallback() {
            public void compile(Compiler context) {
                if (moduleNode.getBodyNode() != null) {
                    NodeCompilerFactory.getCompiler(moduleNode.getBodyNode()).compile(moduleNode.getBodyNode(), context);
                }
                context.loadNil();
            }
        };
        
        ClosureCallback pathCallback = new ClosureCallback() {
            public void compile(Compiler context) {
                if (cpathNode instanceof Colon2Node) {
                    Node leftNode = ((Colon2Node)cpathNode).getLeftNode();
                    if (leftNode != null) {
                        NodeCompilerFactory.getCompiler(leftNode).compile(leftNode, context);
                    } else {
                        context.loadNil();
                    }
                } else if (cpathNode instanceof Colon3Node) {
                    context.loadObject();
                } else {
                    context.loadNil();
                }
            }
        };
        
        context.defineModule(moduleNode.getCPath().getName(), moduleNode.getScope(), pathCallback, bodyCallback);
    }
    
}
