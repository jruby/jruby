/*
 * OpAsgnNodeCompiler.java
 *
 * Created on March 10, 2007, 4:09 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.Node;
import org.jruby.ast.OpAsgnNode;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public class OpAsgnNodeCompiler implements NodeCompiler {
    
    /** Creates a new instance of OpAsgnNodeCompiler */
    public OpAsgnNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        // FIXME: This is a little more complicated than it needs to be; do we see now why closures would be nice in Java?
        
        final OpAsgnNode opAsgnNode = (OpAsgnNode)node;
        
        NodeCompilerFactory.getCompiler(opAsgnNode.getReceiverNode()).compile(opAsgnNode.getReceiverNode(), context); // [recv]
        
        context.duplicateCurrentValue(); // [recv, recv]
        context.invokeDynamic(opAsgnNode.getVariableName(), true, false, CallType.FUNCTIONAL, null, false); // [recv, varValue]
        
        BranchCallback doneBranch = new BranchCallback() {
            public void branch(Compiler context) {
                // get rid of extra receiver, leave the variable result present
                context.swapValues();
                context.consumeCurrentValue();
            }
        };
        
        // Just evaluate the value and stuff it in an argument array
        final ArrayCallback justEvalValue = new ArrayCallback() {
            public void nextValue(Compiler context, Object sourceArray,
                    int index) {
                NodeCompilerFactory.getCompiler(((Node[])sourceArray)[index]).compile(((Node[])sourceArray)[index], context);
            }
        };
        
        BranchCallback assignBranch = new BranchCallback() {
            public void branch(Compiler context) {
                // eliminate extra value, eval new one and assign
                context.consumeCurrentValue();
                context.createObjectArray(new Node[] {opAsgnNode.getValueNode()}, justEvalValue);
                context.invokeAttrAssign(opAsgnNode.getVariableNameAsgn());
            }
        };
        
        if (opAsgnNode.getOperatorName() == "||") {
            // if lhs is true, don't eval rhs and assign
            context.duplicateCurrentValue();
            context.performBooleanBranch(doneBranch, assignBranch);
        } else if (opAsgnNode.getOperatorName() == "&&") {
            // if lhs is true, eval rhs and assign
            context.duplicateCurrentValue();
            context.performBooleanBranch(assignBranch, doneBranch);
        } else {
            // eval new value, call operator on old value, and assign
            context.createObjectArray(new Node[] {opAsgnNode.getValueNode()}, justEvalValue);
            context.invokeDynamic(opAsgnNode.getOperatorName(), true, true, CallType.FUNCTIONAL, null, false);
            context.createObjectArray(1);
            context.invokeAttrAssign(opAsgnNode.getVariableNameAsgn());
        }

        context.pollThreadEvents();
    }
    
}
