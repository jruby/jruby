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
 * Copyright (C) 2006-2007 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.DefinedMessage;

/**
 * Node that represents an assignment of either an array element or attribute.
 */
public class AttrAssignNode extends Node implements INameNode, IArgumentNode {
    protected final Node receiverNode;
    private String name;
    private Node argsNode;
    public CallSite callAdapter;

    public AttrAssignNode(ISourcePosition position, Node receiverNode, String name, Node argsNode) {
        super(position);
        
        assert receiverNode != null : "receiverNode is not null";
        // TODO: At least ParserSupport.attrset passes argsNode as null.  ImplicitNil is wrong magic for 
        // setupArgs since it will IRubyObject[] { nil }.  So we need to figure out a nice fast
        // null pattern for setupArgs.
        // assert argsNode != null : "receiverNode is not null";
        
        this.receiverNode = receiverNode;
        this.name = name;
        setArgsInternal(argsNode);
        this.callAdapter = receiverNode instanceof SelfNode ?
                MethodIndex.getFunctionalCallSite(name) :
                MethodIndex.getCallSite(name);
    }

    public NodeType getNodeType() {
        return NodeType.ATTRASSIGNNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param visitor the visitor
     **/
    public Object accept(NodeVisitor visitor) {
        return visitor.visitAttrAssignNode(this);
    }

    /**
     * Gets the name.
     * name is the name of the method called
     * @return name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the receiverNode.
     * receiverNode is the object on which the method is being called
     * @return receiverNode
     */
    public Node getReceiverNode() {
        return receiverNode;
    }
    
    /**
     * Gets the argsNode.
     * argsNode representing the method's arguments' value for this call.
     * @return argsNode
     */
    public Node getArgsNode() {
        return argsNode;
    }
    
    
    protected Node newAttrAssignNode(ArrayNode argsNode) {
        int size = argsNode.size();
        
        switch (size) {
            case 1:
                return new AttrAssignOneArgNode(getPosition(), receiverNode, name, argsNode);
            case 2:
                return new AttrAssignTwoArgNode(getPosition(), receiverNode, name, argsNode);
            case 3:
                return new AttrAssignThreeArgNode(getPosition(), receiverNode, name, argsNode);
            default:
                return new AttrAssignNode(getPosition(), receiverNode, name, argsNode);
        }
    }
    
    protected Node newMutatedAttrAssignNode(ArrayNode argsNode) {
        int size = argsNode.size();
        
        switch (size) {
            case 1:
                if (!(this instanceof AttrAssignOneArgNode)) {
                    return new AttrAssignOneArgNode(getPosition(), receiverNode, name, argsNode);
                } else {
                    return this;
                }
            case 2:
                if (!(this instanceof AttrAssignTwoArgNode)) {
                    return new AttrAssignTwoArgNode(getPosition(), receiverNode, name, argsNode);
                } else {
                    return this;
                }
            case 3:
                if (!(this instanceof AttrAssignThreeArgNode)) {
                    return new AttrAssignThreeArgNode(getPosition(), receiverNode, name, argsNode);
                } else {
                    return this;
                }
            default:
                return new AttrAssignNode(getPosition(), receiverNode, name, argsNode);
        }
    }
    
    /**
     * Set the argsNode
     * 
     * @param argsNode set the arguments for this node.
     */
    public Node setArgsNode(Node argsNode) {
        // Empirical Observations:
        // null -> Some arity
        // argsNode == this.argsNode then check for arity changes
        // newline(splatnode) -> argspushnode
        if (this.argsNode == null && argsNode instanceof ArrayNode) {
            return newAttrAssignNode((ArrayNode) argsNode);
        } else if (this.argsNode == argsNode) {
            return newMutatedAttrAssignNode((ArrayNode)argsNode);
        }
        
        setArgsInternal(argsNode);
        
        return this;
    }
    
    private void setArgsInternal(Node argsNode) {
        this.argsNode = argsNode;
        
        if (argsNode instanceof ArrayNode) ((ArrayNode)argsNode).setLightweight(true);
    }

    public List<Node> childNodes() {
        return Node.createList(receiverNode, argsNode);
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject receiver = receiverNode.interpret(runtime, context, self, aBlock);
        IRubyObject[] args = ASTInterpreter.setupArgs(runtime, context, argsNode, self, aBlock);
        
        assert hasMetaClass(receiver) : receiverClassName(receiver);
        
        // If reciever is self then we do the call the same way as vcall
        CallSite callSite;
        callSite = callAdapter;
        callSite.call(context, self, receiver, args);

        return args[args.length - 1];
    }
    
    protected static boolean hasMetaClass(IRubyObject object) {
        return object.getMetaClass() != null;
    }
    
    protected static String receiverClassName(IRubyObject object) {
        return object.getClass().getName();
    }
    
    @Override
    public IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value, Block block, boolean checkArity) {        
        IRubyObject receiver = receiverNode.interpret(runtime, context, self, block);
        
        // If reciever is self then we do the call the same way as vcall
        if (receiver == self) {
            return selfAssign(runtime, context, self, value, block, checkArity);
        } else {
            return otherAssign(runtime, context, self, value, block, checkArity);
        }
    }
    
    private IRubyObject selfAssign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value, Block block, boolean checkArity) {        
        IRubyObject receiver = receiverNode.interpret(runtime, context, self, block);
        
        if (argsNode == null) { // attribute set.
            Helpers.invoke(context, receiver, name, value);
        } else { // element set
            RubyArray args = (RubyArray) argsNode.interpret(runtime, context, self, block);
            args.append(value);
            Helpers.invoke(context, receiver, name, args.toJavaArray());
        } 
        
        return runtime.getNil();
    }
    
    private IRubyObject otherAssign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value, Block block, boolean checkArity) {        
        IRubyObject receiver = receiverNode.interpret(runtime, context, self, block);

        if (argsNode == null) { // attribute set.
            Helpers.invoke(context, receiver, name, value, CallType.NORMAL, Block.NULL_BLOCK);
        } else { // element set
            RubyArray args = (RubyArray) argsNode.interpret(runtime, context, self, block);
            args.append(value);
            Helpers.invoke(context, receiver, name, args.toJavaArray(), CallType.NORMAL, Block.NULL_BLOCK);
        } 
        
        return runtime.getNil();
    }
    
    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        if (receiverNode.definition(runtime, context, self, aBlock) != null) {
            try {
                IRubyObject receiver = receiverNode.interpret(runtime, context, self, aBlock);
                RubyClass metaClass = receiver.getMetaClass();
                DynamicMethod method = metaClass.searchMethod(name);
                Visibility visibility = method.getVisibility();

                if (visibility != Visibility.PRIVATE && 
                        (visibility != Visibility.PROTECTED || metaClass.getRealClass().isInstance(self))) {
                    if (metaClass.isMethodBound(name, false)) {
                        return ASTInterpreter.getArgumentDefinition(runtime, context, argsNode, runtime.getDefinedMessage(DefinedMessage.ASSIGNMENT), self, aBlock);
                    }
                }
            } catch (JumpException e) {
            }
        }

        return null;
    }
}
