/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.evaluator.Instruction;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.InterpretedBlock;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A method or operator call.
 */
public class CallNode extends Node implements INameNode, IArgumentNode, BlockAcceptingNode {
    private final Node receiverNode;
    private Node argsNode;
    private Node iterNode;
    public CallSite callAdapter;

    public CallNode(ISourcePosition position, Node receiverNode, String name, Node argsNode) {
        this(position, receiverNode, name, argsNode, null);
    }
    
    public CallNode(ISourcePosition position, Node receiverNode, String name, Node argsNode, 
            Node iterNode) {
        super(position, NodeType.CALLNODE);
        
        assert receiverNode != null : "receiverNode is not null";
        
        this.receiverNode = receiverNode;
        setArgsNode(argsNode);
        this.iterNode = iterNode;
        this.callAdapter = MethodIndex.getCallSite(name);
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Instruction accept(NodeVisitor iVisitor) {
        return iVisitor.visitCallNode(this);
    }
    
    public Node getIterNode() {
        return iterNode;
    }
    
    public Node setIterNode(Node iterNode) {
        this.iterNode = iterNode;
        // refresh call adapter, since it matters if this is iter-based or not
        callAdapter = MethodIndex.getCallSite(callAdapter.methodName);
        
        return this;
    }

    /**
     * Gets the argsNode representing the method's arguments' value for this call.
     * @return argsNode
     */
    public Node getArgsNode() {
        return argsNode;
    }

    /**
     * Set the argsNode
     * 
     * @param argsNode set the arguments for this node.
     */
    public void setArgsNode(Node argsNode) {
        this.argsNode = argsNode;
        // If we have more than one arg, make sure the array created to contain them is not ObjectSpaced
        if (argsNode instanceof ArrayNode) {
            ((ArrayNode)argsNode).setLightweight(true);
        }
    }

    /**
     * Gets the name.
	 * name is the name of the method called
     * @return name
     */
    public String getName() {
        return callAdapter.methodName;
    }

    /**
     * Gets the receiverNode.
	 * receiverNode is the object on which the method is being called
     * @return receiverNode
     */
    public Node getReceiverNode() {
        return receiverNode;
    }
    
    public List<Node> childNodes() {
        return Node.createList(receiverNode, argsNode, iterNode);
    }
    
    @Override
    public String toString() {
        return "CallNode: " + getName();
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject receiver = receiverNode.interpret(runtime, context, self, aBlock);
        
        if (iterNode == null && argsNode != null && argsNode.nodeId == NodeType.ARRAYNODE) {
            ArrayNode arrayNode = (ArrayNode)argsNode;
            
            switch (arrayNode.size()) {
            case 0:
                return callAdapter.call(context, receiver);
            case 1:
                IRubyObject arg0 = arrayNode.get(0).interpret(runtime, context, self, aBlock);
                return callAdapter.call(context, receiver, arg0);
            case 2:
                arg0 = arrayNode.get(0).interpret(runtime, context, self, aBlock);
                IRubyObject arg1 = arrayNode.get(1).interpret(runtime, context, self, aBlock);
                return callAdapter.call(context, receiver, arg0, arg1);
            case 3:
                arg0 = arrayNode.get(0).interpret(runtime, context, self, aBlock);
                arg1 = arrayNode.get(1).interpret(runtime, context, self, aBlock);
                IRubyObject arg2 = arrayNode.get(2).interpret(runtime, context, self, aBlock);
                return callAdapter.call(context, receiver, arg0, arg1, arg2);
            }
        }
        
        IRubyObject[] args = ASTInterpreter.setupArgs(runtime, context, argsNode, self, aBlock);
        
        assert receiver.getMetaClass() != null : receiver.getClass().getName();

        Block block = ASTInterpreter.getBlock(runtime, context, self, aBlock, iterNode);

        // No block provided lets look at fast path for STI dispatch.
        if (!block.isGiven()) {
            return callAdapter.call(context, receiver, args);
        }
            
        while (true) {
            try {
                return callAdapter.call(context, receiver, args, block);
            } catch (JumpException.RetryJump rj) {
                // allow loop to retry
            } catch (JumpException.BreakJump bj) {
                return (IRubyObject) bj.getValue();
            }
        }
    }
    
    public Block getBlock(ThreadContext context, IRubyObject self, IterNode iter) {
        assert iter != null : "iter is not null";
        
        StaticScope scope = iter.getScope();
        scope.determineModule();
            
        // Create block for this iter node
        // FIXME: We shouldn't use the current scope if it's not actually from the same hierarchy of static scopes
        return InterpretedBlock.newInterpretedClosure(context, iter.getBlockBody(), self);
    }
}
