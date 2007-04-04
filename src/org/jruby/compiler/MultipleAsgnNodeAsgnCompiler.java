/*
 * MultipleAsgnNodeAsgnCompiler.java
 *
 * Created on January 31, 2007, 11:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.ListNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.StarNode;

/**
 *
 * @author headius
 */
public class MultipleAsgnNodeAsgnCompiler implements NodeCompiler {
    
    /** Creates a new instance of MultipleAsgnNodeAsgnCompiler */
    public MultipleAsgnNodeAsgnCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        
        // FIXME: This is incomplete.
        
        context.lineNumber(node.getPosition());
        
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode)node;
        
        int varLen = multipleAsgnNode.getHeadNode() == null ? 0 : multipleAsgnNode.getHeadNode().size();
        
        { // normal items at the "head" of the masgn
            ArrayCallback headAssignCallback = new ArrayCallback() {
                public void nextValue(Compiler context, Object sourceArray,
                                      int index) {
                    ListNode headNode = (ListNode)sourceArray;
                    Node assignNode = headNode.get(index);

                    // perform assignment for the next node
                    NodeCompilerFactory.getAssignmentCompiler(assignNode).compile(assignNode, context);
                }
            };

            context.ensureRubyArray();
            context.forEachInValueArray(multipleAsgnNode.getHeadNode().size(), 0, multipleAsgnNode.getHeadNode(), headAssignCallback);
        }
        
        // FIXME: This needs to fit in somewhere
        //if (callAsProc && iter.hasNext()) {
        //    throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        //}
        
        { // "args node" handling
            Node argsNode = multipleAsgnNode.getArgsNode();
            if (argsNode != null) {
                if (argsNode instanceof StarNode) {
                    // no check for '*'
                } else {
                    BranchCallback trueBranch = new BranchCallback() {
                        public void branch(Compiler context) {
                            
                        }
                    };
                    
                    // check if the number of variables is exceeded by the number of values in the array
                    // the number of values
                    context.loadRubyArraySize();
                    context.loadInteger(varLen);
                    //context.performLTBranch(trueBranch, falseBranch);
                } 
            }
        }
    }
}
