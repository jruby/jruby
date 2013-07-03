/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class OpAsgnNode extends Node {
    private final Node receiverNode;
    private final Node valueNode;
    public final CallSite variableCallAdapter;
    public final CallSite operatorCallAdapter;
    public final CallSite variableAsgnCallAdapter;

    public OpAsgnNode(ISourcePosition position, Node receiverNode, Node valueNode, String variableName, String operatorName) {
        super(position);
        
        assert receiverNode != null : "receiverNode is not null";
        assert valueNode != null : "valueNode is not null";
        
        this.receiverNode = receiverNode;
        this.valueNode = valueNode;
        this.variableCallAdapter = MethodIndex.getCallSite(variableName);
        this.operatorCallAdapter = MethodIndex.getCallSite(operatorName);
        this.variableAsgnCallAdapter = MethodIndex.getCallSite((variableName + "=").intern());
    }

    public NodeType getNodeType() {
        return NodeType.OPASGNNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitOpAsgnNode(this);
    }

    /**
     * Gets the methodName.
     * @return Returns a String
     */
    public String getOperatorName() {
        return operatorCallAdapter.methodName;
    }

    /**
     * Gets the receiverNode.
     * @return Returns a Node
     */
    public Node getReceiverNode() {
        return receiverNode;
    }

    /**
     * Gets the valueNode.
     * @return Returns a Node
     */
    public Node getValueNode() {
        return valueNode;
    }

    /**
     * Gets the varibaleName.
     * @return Returns a String
     */
    public String getVariableName() {
        return variableCallAdapter.methodName;
    }
    
    public String getVariableNameAsgn() {
        return variableAsgnCallAdapter.methodName;
    }
    
    public List<Node> childNodes() {
        return Node.createList(receiverNode, valueNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject receiver = receiverNode.interpret(runtime, context, self, aBlock);
        IRubyObject value = variableCallAdapter.call(context, self, receiver);
   
        if (getOperatorName() == "||") {
            if (value.isTrue()) {
                return ASTInterpreter.pollAndReturn(context, value);
            }
            value = valueNode.interpret(runtime, context, self, aBlock);
        } else if (getOperatorName() == "&&") {
            if (!value.isTrue()) {
                return ASTInterpreter.pollAndReturn(context, value);
            }
            value = valueNode.interpret(runtime,context, self, aBlock);
        } else {
            value = operatorCallAdapter.call(context, self, value, valueNode.interpret(runtime, context, self, aBlock));
        }
   
        variableAsgnCallAdapter.call(context, self, receiver, value);
   
        return ASTInterpreter.pollAndReturn(context, value);
    }
}
