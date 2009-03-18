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
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A method or operator call.
 */
public class CallNode extends Node implements INameNode, IArgumentNode, BlockAcceptingNode {
    private final Node receiverNode;
    private Node argsNode;
    protected Node iterNode;
    public CallSite callAdapter;

    @Deprecated
    public CallNode(ISourcePosition position, Node receiverNode, String name, Node argsNode) {
        this(position, receiverNode, name, argsNode, null);
    }
    
    public CallNode(ISourcePosition position, Node receiverNode, String name, Node argsNode, 
            Node iterNode) {
        super(position);
        
        assert receiverNode != null : "receiverNode is not null";
        
        this.receiverNode = receiverNode;
        setArgsNode(argsNode);
        this.iterNode = iterNode;
        this.callAdapter = MethodIndex.getCallSite(name);
    }

    public NodeType getNodeType() {
        return NodeType.CALLNODE;
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
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
     * Set the argsNode.  This is for re-writer and not general interpretation.
     * 
     * @param argsNode set the arguments for this node.
     */
    public Node setArgsNode(Node argsNode) {
        this.argsNode = argsNode;
        // If we have more than one arg, make sure the array created to contain them is not ObjectSpaced
        if (argsNode instanceof ArrayNode) {
            ((ArrayNode)argsNode).setLightweight(true);
        }
        
        return argsNode;
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
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        assert false: "No longer called";

        return null;
    }
    
    @Override
    public IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value, Block block, boolean checkArity) {
        IRubyObject receiver = receiverNode.interpret(runtime, context, self, block);

        if (argsNode == null) { // attribute set.
            RuntimeHelpers.invoke(context, receiver, getName(), new IRubyObject[] {value}, CallType.NORMAL, Block.NULL_BLOCK);
        } else { // element set
            RubyArray args = (RubyArray)argsNode.interpret(runtime, context, self, block);
            args.append(value);
            RuntimeHelpers.invoke(context, receiver, getName(), args.toJavaArray(), CallType.NORMAL, Block.NULL_BLOCK);
        }
        
        return runtime.getNil();
    }

    @Override
    public String definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        if (receiverNode.definition(runtime, context, self, aBlock) != null) {
            try {
                IRubyObject receiver = receiverNode.interpret(runtime, context, self, aBlock);
                RubyClass metaClass = receiver.getMetaClass();
                DynamicMethod method = metaClass.searchMethod(getName());
                Visibility visibility = method.getVisibility();
                
                if (visibility != Visibility.PRIVATE &&
                        (visibility != Visibility.PROTECTED || metaClass.getRealClass().isInstance(self))) {
                    if (!method.isUndefined()) {
                        return ASTInterpreter.getArgumentDefinition(runtime, context, getArgsNode(), "method", self, aBlock);
                    }
                }
            } catch (JumpException excptn) {
            }
        }

        return null;
    }
}
