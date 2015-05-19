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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;

public class CachedBoxedDispatchNode extends CachedDispatchNode {

    private final RubyClass expectedClass;
    private final Assumption unmodifiedAssumption;

    private final InternalMethod method;
    @Child private DirectCallNode callNode;
    @Child private IndirectCallNode indirectCallNode;

    public CachedBoxedDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            RubyClass expectedClass,
            InternalMethod method,
            boolean indirect,
            DispatchAction dispatchAction) {
        super(context, cachedName, next, indirect, dispatchAction);

        this.expectedClass = expectedClass;
        this.unmodifiedAssumption = expectedClass.getUnmodifiedAssumption();
        this.next = next;
        this.method = method;

        if (method != null) {
            if (indirect) {
                indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
            } else {
                callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());

                if ((callNode.isCallTargetCloningAllowed() && method.getSharedMethodInfo().shouldAlwaysSplit())
                        || (method.getDeclaringModule() != null
                        && method.getDeclaringModule().getName().equals("TruffleInterop"))) {
                    insert(callNode);
                    callNode.cloneCallTarget();
                }
            }
        }
    }

    @Override
    public boolean guard(Object methodName, Object receiver) {
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

            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return String.format("CachedBoxedDispatchNode(:%s, %s@%x, %s)",
                getCachedNameAsSymbol().toString(),
                expectedClass.getName(), expectedClass.hashCode(),
                method == null ? "null" : method.toString());
    }

    public boolean couldOptimizeKeywordArguments() {
        // TODO CS 18-Apr-15 doesn't seem to work with Truffle?
        return false; //method.getSharedMethodInfo().getArity().getKeywordArguments() != null && next instanceof UnresolvedDispatchNode;
    }

    public InternalMethod getMethod() {
        return method;
    }

    public Assumption getUnmodifiedAssumption() {
        return unmodifiedAssumption;
    }
}
