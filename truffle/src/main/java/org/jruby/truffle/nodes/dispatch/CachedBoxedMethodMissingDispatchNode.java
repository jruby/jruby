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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
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
import org.jruby.util.cli.Options;

public class CachedBoxedMethodMissingDispatchNode extends CachedDispatchNode {

    private final RubyClass expectedClass;
    private final Assumption unmodifiedAssumption;
    private final InternalMethod method;

    @Child private DirectCallNode callNode;
    @Child private IndirectCallNode indirectCallNode;

    public CachedBoxedMethodMissingDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            RubyClass expectedClass,
            InternalMethod method,
            boolean indirect,
            DispatchAction dispatchAction) {
        super(context, cachedName, next, indirect, dispatchAction);

        this.expectedClass = expectedClass;
        unmodifiedAssumption = expectedClass.getUnmodifiedAssumption();
        this.method = method;

        if (indirect) {
            indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
        } else {
            callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());

            /*
             * The way that #method_missing is used is usually as an indirection to call some other method, and
             * possibly to modify the arguments. In both cases, but especially the latter, it makes a lot of sense
             * to manually clone the call target and to inline it.
             */

            if (callNode.isCallTargetCloningAllowed()
                    && (Options.TRUFFLE_DISPATCH_METHODMISSING_ALWAYS_CLONED.load()
                    || method.getSharedMethodInfo().shouldAlwaysSplit())) {
                insert(callNode);
                callNode.cloneCallTarget();
            }

            if (callNode.isInlinable() && Options.TRUFFLE_DISPATCH_METHODMISSING_ALWAYS_INLINED.load()) {
                insert(callNode);
                callNode.forceInlining();
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
                    (RubyProc) blockObject,
                    argumentsObjects,
                    "class modified");
        }

        switch (getDispatchAction()) {
            case CALL_METHOD: {
                // When calling #method_missing we need to prepend the symbol

                final Object[] argumentsObjectsArray = (Object[]) argumentsObjects;
                final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjectsArray.length];
                modifiedArgumentsObjects[0] = getCachedNameAsSymbol();
                RubyArguments.arraycopy(argumentsObjectsArray, 0, modifiedArgumentsObjects, 1, argumentsObjectsArray.length);

                if (isIndirect()) {
                    return indirectCallNode.call(
                            frame,
                            method.getCallTarget(),
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    (RubyProc) blockObject,
                                    modifiedArgumentsObjects));
                } else {
                    return callNode.call(
                            frame,
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    (RubyProc) blockObject,
                                    modifiedArgumentsObjects));
                }
            }

            case RESPOND_TO_METHOD:
                return false;

            case READ_CONSTANT: {
                if (isIndirect()) {
                    return indirectCallNode.call(
                            frame,
                            method.getCallTarget(),
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    (RubyProc) blockObject,
                                    new Object[]{getCachedNameAsSymbol()}));
                } else {
                    return callNode.call(
                            frame,
                            RubyArguments.pack(
                                    method,
                                    method.getDeclarationFrame(),
                                    receiverObject,
                                    (RubyProc) blockObject,
                                    new Object[]{getCachedNameAsSymbol()}));
                }
            }

            default:
                throw new UnsupportedOperationException();
        }
    }

}
