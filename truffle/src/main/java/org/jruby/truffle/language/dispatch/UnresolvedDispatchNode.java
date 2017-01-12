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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.InternalMethod;

public final class UnresolvedDispatchNode extends DispatchNode {

    private int depth = 0;

    private final boolean ignoreVisibility;
    private final MissingBehavior missingBehavior;

    public UnresolvedDispatchNode(
            boolean ignoreVisibility,
            MissingBehavior missingBehavior,
            DispatchAction dispatchAction) {
        super(dispatchAction);
        this.ignoreVisibility = ignoreVisibility;
        this.missingBehavior = missingBehavior;
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        return false;
    }

    @Override
    public Object executeDispatch(
            final VirtualFrame frame,
            final Object receiverObject,
            final Object methodName,
            DynamicObject blockObject,
            final Object[] argumentsObjects) {
        CompilerDirectives.transferToInterpreter();
        // Useful debug aid to catch a running-away NotProvided or undefined
        assert !(receiverObject instanceof NotProvided) : RubyLanguage.fileLine(getEncapsulatingSourceSection());

        // Make sure to have an up-to-date Shape.
        if (receiverObject instanceof DynamicObject) {
            synchronized (receiverObject) {
                ((DynamicObject) receiverObject).updateShape();
            }
        }

        final DispatchNode dispatch = atomic(() -> {
            final DispatchNode first = getHeadNode().getFirstDispatchNode();

            // First try to see if we did not a miss a specialization added by another thread.

            DispatchNode lookupDispatch = first;
            while (lookupDispatch != null) {
                if (lookupDispatch.guard(methodName, receiverObject)) {
                    // This one worked, no need to rewrite anything.
                    return lookupDispatch;
                }
                lookupDispatch = lookupDispatch.getNext();
            }

            // We need a new node to handle this case.

            final DispatchNode newDispathNode;

            if (depth == getContext().getOptions().DISPATCH_CACHE) {
                newDispathNode = new UncachedDispatchNode(ignoreVisibility, getDispatchAction(), missingBehavior);
            } else {
                depth++;
                if (RubyGuards.isForeignObject(receiverObject)) {
                    newDispathNode = new CachedForeignDispatchNode(first, methodName);
                } else if (RubyGuards.isRubyBasicObject(receiverObject)) {
                    newDispathNode = doDynamicObject(frame, first, receiverObject, methodName, argumentsObjects);
                } else {
                    newDispathNode = doUnboxedObject(frame, first, receiverObject, methodName);
                }
            }

            first.replace(newDispathNode);
            return newDispathNode;
        });

        return dispatch.executeDispatch(frame, receiverObject, methodName, blockObject, argumentsObjects);
    }

    private DispatchNode doUnboxedObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName) {

        final String methodNameString = toString(methodName);
        final InternalMethod method = lookup(frame, receiverObject, methodNameString, ignoreVisibility);

        if (method == null) {
            return createMethodMissingNode(first, methodName, receiverObject);
        }

        if (receiverObject instanceof Boolean) {
            final Assumption falseUnmodifiedAssumption = Layouts.MODULE.getFields(coreLibrary().getFalseClass()).getUnmodifiedAssumption();
            final InternalMethod falseMethod = lookup(frame, false, methodNameString, ignoreVisibility);

            final Assumption trueUnmodifiedAssumption = Layouts.MODULE.getFields(coreLibrary().getTrueClass()).getUnmodifiedAssumption();
            final InternalMethod trueMethod = lookup(frame, true, methodNameString, ignoreVisibility);
            assert falseMethod != null || trueMethod != null;

            return new CachedBooleanDispatchNode(
                    methodName, first,
                    falseUnmodifiedAssumption, falseMethod,
                    trueUnmodifiedAssumption, trueMethod,
                    getDispatchAction());
        } else {
            return new CachedUnboxedDispatchNode(
                    methodName, first, receiverObject.getClass(),
                    Layouts.MODULE.getFields(coreLibrary().getLogicalClass(receiverObject)).getUnmodifiedAssumption(), method, getDispatchAction());
        }
    }

    private DispatchNode doDynamicObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName,
            Object[] argumentsObjects) {

        String methodNameString = toString(methodName);
        final InternalMethod method = lookup(frame, receiverObject, methodNameString, ignoreVisibility);

        if (method == null) {
            return createMethodMissingNode(first, methodName, receiverObject);
        }

        final DynamicObject receiverMetaClass = coreLibrary().getMetaClass(receiverObject);
        if (RubyGuards.isRubySymbol(receiverObject)) {
            return new CachedBoxedSymbolDispatchNode(getContext(), methodName, first, method, getDispatchAction());
        } else if (Layouts.CLASS.getIsSingleton(receiverMetaClass)) {
            return new CachedSingletonDispatchNode(methodName, first, ((DynamicObject) receiverObject),
                    receiverMetaClass, method, getDispatchAction());
        } else {
            return new CachedBoxedDispatchNode(getContext(), methodName, first, ((DynamicObject) receiverObject).getShape(),
                    receiverMetaClass, method, getDispatchAction());
        }
    }

    private String toString(Object methodName) {
        if (methodName instanceof String) {
            return (String) methodName;
        } else if (RubyGuards.isRubyString(methodName)) {
            return methodName.toString();
        } else if (RubyGuards.isRubySymbol(methodName)) {
            return Layouts.SYMBOL.getString((DynamicObject) methodName);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DispatchNode createMethodMissingNode(
            DispatchNode first,
            Object methodName,
            Object receiverObject) {
        switch (missingBehavior) {
            case RETURN_MISSING: {
                return new CachedReturnMissingDispatchNode(methodName, first, coreLibrary().getMetaClass(receiverObject),
                        getDispatchAction());
            }

            case CALL_METHOD_MISSING: {
                final InternalMethod method = lookup(null, receiverObject, "method_missing", true);

                if (method == null) {
                    throw new RaiseException(coreExceptions().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                return new CachedMethodMissingDispatchNode(methodName, first, coreLibrary().getMetaClass(receiverObject),
                        method, getDispatchAction());
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

}
