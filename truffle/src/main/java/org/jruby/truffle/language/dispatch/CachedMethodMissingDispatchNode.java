/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.MetaClassNode;
import org.jruby.truffle.language.objects.MetaClassNodeGen;

public class CachedMethodMissingDispatchNode extends CachedDispatchNode {

    private final DynamicObject expectedClass;
    private final Assumption unmodifiedAssumption;
    private final InternalMethod method;

    @Child private MetaClassNode metaClassNode;
    @Child private DirectCallNode callNode;

    public CachedMethodMissingDispatchNode(
            Object cachedName,
            DispatchNode next,
            DynamicObject expectedClass,
            InternalMethod method,
            DispatchAction dispatchAction) {
        super(cachedName, next, dispatchAction);

        this.expectedClass = expectedClass;
        this.unmodifiedAssumption = Layouts.MODULE.getFields(expectedClass).getUnmodifiedAssumption();
        this.method = method;
        this.metaClassNode = MetaClassNodeGen.create(null);
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
                final Object[] modifiedArgumentsObjects = ArrayUtils.unshift(argumentsObjects, getCachedNameAsSymbol());

                return call(callNode, frame, method, receiverObject, blockObject, modifiedArgumentsObjects);

            case RESPOND_TO_METHOD:
                return false;

            default:
                throw new UnsupportedOperationException();
        }
    }

}