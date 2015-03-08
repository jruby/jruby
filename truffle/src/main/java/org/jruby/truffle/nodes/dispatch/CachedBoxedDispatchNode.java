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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNode;
import org.jruby.truffle.nodes.literal.HashLiteralNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.methods.MarkerNode;
import org.jruby.truffle.nodes.methods.arguments.OptionalKeywordArgMissingNode;
import org.jruby.truffle.nodes.methods.arguments.UnknownArgumentErrorNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubySymbol;
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
            ProcOrNullNode block,
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
                block,
                isSplatted);
     }
     
    public static RubyNode[] expandedArgumentNodes(RubyContext context,
            InternalMethod method, RubyNode[] argumentNodes, boolean isSplatted) {
        final RubyNode[] result;

        boolean shouldExpand = true;
        if (method == null
                || method.getSharedMethodInfo().getKeywordArguments() == null) {
            // no keyword arguments in method definition
            shouldExpand = false;
        } else if (argumentNodes.length != 0
                && !(argumentNodes[argumentNodes.length - 1] instanceof HashLiteralNode)) {
            // last argument is not a Hash that could be expanded
            shouldExpand = false;
        } else if (method.getSharedMethodInfo().getArity() == null
                || method.getSharedMethodInfo().getArity().getRequired() >= argumentNodes.length) {
            shouldExpand = false;
        } else if (isSplatted
                || method.getSharedMethodInfo().getArity().allowsMore()) {
            // TODO: make optimization work if splat arguments are involed
            // the problem is that Markers and keyword args are used when
            // reading splatted args
            shouldExpand = false;
        }

        if (shouldExpand) {
            List<String> kwargs = method.getSharedMethodInfo().getKeywordArguments();
           
            int countArgNodes = argumentNodes.length + kwargs.size() + 1;
            if (argumentNodes.length == 0) {
                countArgNodes++;
            }
            
            result = new RubyNode[countArgNodes];
            int i;
           
            for (i = 0; i < argumentNodes.length - 1; ++i) {
                result[i] = argumentNodes[i];
            }
           
            int firstMarker = i++;
            result[firstMarker] = new MarkerNode(context, null);

            HashLiteralNode hashNode;
            if (argumentNodes.length > 0) {
                hashNode = (HashLiteralNode) argumentNodes[argumentNodes.length - 1];
            } else {
                hashNode = HashLiteralNode.create(context, null,
                        new RubyNode[0]);
            }

            List<String> restKeywordLabels = new ArrayList<String>();
            for (int j = 0; j < hashNode.size(); j++) {
                Object key = hashNode.getKey(j);
                boolean keyIsSymbol = key instanceof ObjectLiteralNode &&
                     ((ObjectLiteralNode) key).getObject() instanceof RubySymbol;
                
                if (!keyIsSymbol) {
                   // cannot optimize case where keyword label is dynamic (not a fixed RubySymbol)
                   return argumentNodes;
                }
                
                final String label = ((ObjectLiteralNode) hashNode.getKey(j)).getObject().toString();
                restKeywordLabels.add(label);
            }

            for (String kwarg : kwargs) {
               result[i] = new OptionalKeywordArgMissingNode(context, null);
               for (int j = 0; j < hashNode.size(); j++) {
                   final String label = ((ObjectLiteralNode) hashNode.getKey(j)).getObject().toString();
                   
                   if (label.equals(kwarg)) {
                       result[i] = hashNode.getValue(j);
                       restKeywordLabels.remove(label);
                       break;
                   }
               }
               i++;
            }
            result[i++] = new MarkerNode(context, null);

            if (restKeywordLabels.size() > 0
                    && !method.getSharedMethodInfo().getArity().hasKeyRest()) {
                result[firstMarker] = new UnknownArgumentErrorNode(context, null, restKeywordLabels.get(0));
            } else if (restKeywordLabels.size() > 0) {
                i = 0;
                RubyNode[] keyValues = new RubyNode[2 * restKeywordLabels
                        .size()];

               for (String label : restKeywordLabels) {
                   for (int j = 0; j < hashNode.size(); j++) {
                       final String argLabel = ((ObjectLiteralNode) hashNode.getKey(j)).getObject().toString();
                       
                       if (argLabel.equals(label)) {
                           keyValues[i++] = hashNode.getKey(j);
                           keyValues[i++] = hashNode.getValue(j);
                       }
                   }
               }
               
               HashLiteralNode restHash = HashLiteralNode.create(context, null, keyValues);
               result[firstMarker] = restHash;
            }

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
            ProcOrNullNode block,
            boolean isSplatted) {
        super(context, cachedName, next, indirect, dispatchAction, expandedArgumentNodes(context, method, argumentNodes, isSplatted), block, isSplatted);

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
    protected boolean guard(Object methodName, Object receiver) {
        return guardName(methodName) &&
                (receiver instanceof RubyBasicObject) &&
                ((RubyBasicObject) receiver).getMetaClass() == expectedClass;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        if (!guard(methodName, receiverObject)) {
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
                    (RubyProc) blockObject,
                    argumentsObjects,
                    "class modified");
        }

        switch (getDispatchAction()) {
            case CALL_METHOD: {
                argumentsObjects = executeArguments(frame, argumentsObjects);
                blockObject = executeBlock(frame, blockObject);
                if (isIndirect()) {
                    return indirectCallNode.call(
                            frame,
                            method.getCallTarget(),
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    (RubyProc) blockObject,
                                    (Object[]) argumentsObjects));
                } else {
                    return callNode.call(
                            frame,
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    (RubyProc) blockObject,
                                    (Object[]) argumentsObjects));
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
