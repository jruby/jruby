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
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.objects.MetaClassWithShapeCacheNode;
import org.jruby.truffle.nodes.objects.MetaClassWithShapeCacheNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.InternalMethod;

public class CachedMethodMissingDispatchNode extends CachedDispatchNode {

    private final DynamicObject expectedClass;
    private final Assumption unmodifiedAssumption;
    private final InternalMethod method;

    @Child private MetaClassWithShapeCacheNode metaClassNode;
    @Child private DirectCallNode callNode;

    public CachedMethodMissingDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            DynamicObject expectedClass,
            InternalMethod method,
            DispatchAction dispatchAction) {
        super(context, cachedName, next, dispatchAction);

        this.expectedClass = expectedClass;
        this.unmodifiedAssumption = Layouts.MODULE.getFields(expectedClass).getUnmodifiedAssumption();
        this.method = method;
        this.metaClassNode = MetaClassWithShapeCacheNodeGen.create(context, getSourceSection(), null);
        this.callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());

        /*
         * The way that #method_missing is used is usually as an indirection to call some other method, and
         * possibly to modify the arguments. In both cases, but especially the latter, it makes a lot of sense
         * to manually clone the call target and to inline it.
         */

        if (callNode.isCallTargetCloningAllowed()
                && (getContext().getOptions().METHODMISSING_ALWAYS_CLONE || method.getSharedMethodInfo().shouldAlwaysClone())) {
            insert(callNode);
            callNode.cloneCallTarget();
        }

        if (callNode.isInlinable() && getContext().getOptions().METHODMISSING_ALWAYS_INLINE) {
            insert(callNode);
            callNode.forceInlining();
        }
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        return guardName(methodName) &&
                metaClassNode.executeMetaClass(receiver) == expectedClass;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects) {
        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return resetAndDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects,
                    "class modified");
        }

        if (!guard(methodName, receiverObject)) {
            return next.executeDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects);
        }

        switch (getDispatchAction()) {
            case CALL_METHOD:
                // When calling #method_missing we need to prepend the symbol

                final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjects.length];
                modifiedArgumentsObjects[0] = getCachedNameAsSymbol();
                ArrayUtils.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjects.length);

                return call(callNode, frame, method, receiverObject, blockObject, modifiedArgumentsObjects);

            case RESPOND_TO_METHOD:
                return false;

            default:
                throw new UnsupportedOperationException();
        }
    }

}