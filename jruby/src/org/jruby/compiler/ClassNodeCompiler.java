/*
 * ClassNodeCompiler.java
 *
 * Created on April 23, 2007, 10:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.ClassNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class ClassNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of ClassNodeCompiler */
    public ClassNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        final ClassNode classNode = (ClassNode)node;
        
        final Node superNode = classNode.getSuperNode();
        
        final Node cpathNode = classNode.getCPath();
        
        ClosureCallback superCallback = new ClosureCallback() {
            public void compile(Compiler context) {
                if (superNode != null) {
                    NodeCompilerFactory.getCompiler(superNode).compile(superNode, context);
                } else {
                    context.loadObject();
                }
            }
        };
        
        ClosureCallback bodyCallback = new ClosureCallback() {
            public void compile(Compiler context) {
                if (classNode.getBodyNode() != null) {
                    NodeCompilerFactory.getCompiler(classNode.getBodyNode()).compile(classNode.getBodyNode(), context);
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
        
        context.defineClass(classNode.getCPath().getName(), classNode.getScope(), superCallback, pathCallback, bodyCallback);
    }
    
}
