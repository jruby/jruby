/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.literal.FixnumLiteralNode;
import org.jruby.truffle.nodes.literal.HashLiteralNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.literal.StringLiteralNode;
import org.jruby.truffle.nodes.methods.MarkerNode;
import org.jruby.truffle.nodes.methods.arguments.MissingKeywordArgumentNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;

public class CachedBoxedDispatchNode extends CachedDispatchNode {

    private final RubyClass expectedClass;
    private final Assumption unmodifiedAssumption;

    private final Object value;
    
    private final InternalMethod method;
    @Child private DirectCallNode callNode;
    @Child private IndirectCallNode indirectCallNode;
    
    public CachedBoxedDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            RubyClass expectedClass,
            Object value,
            InternalMethod method,
            boolean indirect,
            DispatchAction dispatchAction,
            RubyNode[] argumentNodes,
            boolean isSplatted) {
        this(
                context,
                cachedName,
                next,
                expectedClass,
                expectedClass.getUnmodifiedAssumption(),
                value,
                method,
                indirect,
                dispatchAction,
                argumentNodes,
                isSplatted);
    }
    
    public static RubyNode[] expandedArgumentNodes(RubyContext context, InternalMethod method, RubyNode[] argumentNodes) {
    	final RubyNode[] result;
    	
        if (method != null && method.getSharedMethodInfo().getKeywordArguments() != null && 
        		argumentNodes[argumentNodes.length - 1] instanceof HashLiteralNode) {
        	List<String> kwargs = method.getSharedMethodInfo().getKeywordArguments();
        	
        	result = new RubyNode[argumentNodes.length + kwargs.size() + 1];
        	int i;
        	
        	for (i = 0; i < argumentNodes.length - 1; ++i) {
        		result[i] = argumentNodes[i];
        	}
        	
        	result[i++] = new MarkerNode(context, null);
        	HashLiteralNode hashNode = (HashLiteralNode) argumentNodes[argumentNodes.length - 1];
        	
        	for (String kwarg : kwargs) {
        		result[i] = new MissingKeywordArgumentNode(context, null, kwarg);
        		for (int j = 0; j < hashNode.getKeyValues().length; j += 2) {
        			String label;
					label = ((ObjectLiteralNode) hashNode.getKeyValues()[j]).execute(null).toString();
        			
        			if (label.equals(kwarg)) {
        				result[i++] = hashNode.getKeyValues()[j + 1];
        			}
        		}
        	}
        	
        	result[i++] = new MarkerNode(context, null);
        }
        else {
        	result = argumentNodes;
        }
        
        return result;
    }
    /**
     * Allows to give the assumption, which is different than the expectedClass assumption for constant lookup.
     */
    public CachedBoxedDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            RubyClass expectedClass,
            Assumption unmodifiedAssumption,
            Object value,
            InternalMethod method,
            boolean indirect,
            DispatchAction dispatchAction,
            RubyNode[] argumentNodes,
            boolean isSplatted) {
        super(context, cachedName, next, indirect, dispatchAction, expandedArgumentNodes(context, method, argumentNodes), isSplatted);

        this.expectedClass = expectedClass;
        this.unmodifiedAssumption = unmodifiedAssumption;
        this.next = next;
        this.value = value;
        this.method = method;
        
        if (method != null) {
            if (indirect) {
                indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
            } else {
                callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());

                if (callNode.isCallTargetCloningAllowed() && method.getSharedMethodInfo().shouldAlwaysSplit()) {
                    insert(callNode);
                    callNode.cloneCallTarget();
                }
            }
        }
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        if (!guardName(methodName) || !(receiverObject instanceof RubyBasicObject) || ((RubyBasicObject) receiverObject).getMetaClass() != expectedClass) {
            return next.executeDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return resetAndDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                    argumentsObjects,
                    "class modified");
        }
        
        switch (getDispatchAction()) {
            case CALL_METHOD: {
                if (isIndirect()) {
                    return indirectCallNode.call(
                            frame,
                            method.getCallTarget(),
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                    CompilerDirectives.unsafeCast(executeArguments(frame, argumentsObjects), Object[].class, true)));
                } else {
                	Object args = executeArguments(frame, argumentsObjects);
                    return callNode.call(
                            frame,
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                    CompilerDirectives.unsafeCast(args, Object[].class, true)));
                }
            }

            case RESPOND_TO_METHOD:
                return true;

            case READ_CONSTANT:
                return value;

            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return String.format("CachedBoxedDispatchNode(:%s, %s@%x, %s, %s)",
                getCachedNameAsSymbol().toString(),
                expectedClass.getName(), expectedClass.hashCode(),
                value == null ? "null" : DebugOperations.inspect(getContext(), value),
                method == null ? "null" : method.toString());
    }

}
