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
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.interop.messages.Argument;
import com.oracle.truffle.interop.messages.Read;
import com.oracle.truffle.interop.messages.Receiver;
import com.oracle.truffle.interop.node.ForeignObjectAccessNode;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.objects.SingletonClassNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.concurrent.Callable;

public final class UnresolvedDispatchNode extends DispatchNode {

    private int depth = 0;

    private final boolean ignoreVisibility;
    private final MissingBehavior missingBehavior;

    public UnresolvedDispatchNode(
            RubyContext context,
            boolean ignoreVisibility,
            MissingBehavior missingBehavior,
            DispatchAction dispatchAction) {
        super(context, dispatchAction);
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
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final DispatchNode dispatch = atomic(new Callable<DispatchNode>() {
            @Override
            public DispatchNode call() throws Exception {
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
                    newDispathNode = new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), missingBehavior);
                } else {
                    depth++;
                    if (receiverObject instanceof DynamicObject) {
                        newDispathNode = doDynamicObject(frame, first, receiverObject, methodName, argumentsObjects);
                    } else if (RubyGuards.isForeignObject(receiverObject)) {
                        newDispathNode = createForeign(argumentsObjects, first, methodName);
                    } else {
                        newDispathNode = doUnboxedObject(frame, first, receiverObject, methodName);
                    }
                }

                first.replace(newDispathNode);
                return newDispathNode;
            }
        });

        return dispatch.executeDispatch(frame, receiverObject, methodName, blockObject, argumentsObjects);
    }

    private DispatchNode createForeign(Object[] argumentsObjects, DispatchNode first, Object methodName) {
        return new CachedForeignDispatchNode(getContext(), first, methodName, argumentsObjects.length);
    }

    private DispatchNode doUnboxedObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName) {
        final DynamicObject callerClass;

        if (ignoreVisibility) {
            callerClass = null;
        } else {
            callerClass = getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));
        }

        final String methodNameString = toString(methodName);
        final InternalMethod method = lookup(callerClass, receiverObject, methodNameString, ignoreVisibility);

        if (method == null) {
            return createMethodMissingNode(first, methodName, receiverObject);
        }

        if (receiverObject instanceof Boolean) {
            final Assumption falseUnmodifiedAssumption = Layouts.MODULE.getFields(getContext().getCoreLibrary().getFalseClass()).getUnmodifiedAssumption();
            final InternalMethod falseMethod = lookup(callerClass, false, methodNameString, ignoreVisibility);

            final Assumption trueUnmodifiedAssumption = Layouts.MODULE.getFields(getContext().getCoreLibrary().getTrueClass()).getUnmodifiedAssumption();
            final InternalMethod trueMethod = lookup(callerClass, true, methodNameString, ignoreVisibility);
            assert falseMethod != null || trueMethod != null;

            return new CachedBooleanDispatchNode(getContext(),
                    methodName, first,
                    falseUnmodifiedAssumption, falseMethod,
                    trueUnmodifiedAssumption, trueMethod,
                    getDispatchAction());
        } else {
            return new CachedUnboxedDispatchNode(getContext(),
                    methodName, first, receiverObject.getClass(),
                    Layouts.MODULE.getFields(getContext().getCoreLibrary().getLogicalClass(receiverObject)).getUnmodifiedAssumption(), method, getDispatchAction());
        }
    }

    private DispatchNode doDynamicObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName,
            Object[] argumentsObjects) {
        final DynamicObject callerClass;

        if (ignoreVisibility) {
            callerClass = null;
        } else if (getDispatchAction() == DispatchAction.RESPOND_TO_METHOD) {
            final Frame callerFrame = RubyCallStack.getCallerFrame(getContext()).getFrame(FrameInstance.FrameAccess.READ_ONLY, true);
            callerClass = getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(callerFrame.getArguments()));
        } else {
            callerClass = getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));
        }

        final InternalMethod method = lookup(callerClass, receiverObject, toString(methodName), ignoreVisibility);

        if (method == null) {
            final DispatchNode multilanguage = tryMultilanguage(frame, first, methodName, argumentsObjects);
            if (multilanguage != null) {
                return multilanguage;
            }

            return createMethodMissingNode(first, methodName, receiverObject);
        }

        final DynamicObject receiverMetaClass = getContext().getCoreLibrary().getMetaClass(receiverObject);
        if (RubyGuards.isRubySymbol(receiverObject)) {
            return new CachedBoxedSymbolDispatchNode(getContext(), methodName, first, method, getDispatchAction());
        } else if (Layouts.CLASS.getIsSingleton(receiverMetaClass)) {
            return new CachedSingletonDispatchNode(getContext(), methodName, first, ((DynamicObject) receiverObject),
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

    private DispatchNode tryMultilanguage(VirtualFrame frame, DispatchNode first, Object methodName, Object[] argumentsObjects) {
        if (getContext().getMultilanguageObject() != null) {
            CompilerAsserts.neverPartOfCompilation();
            TruffleObject multilanguageObject = getContext().getMultilanguageObject();
            ForeignObjectAccessNode readLanguage = ForeignObjectAccessNode.getAccess(Read.create(Receiver.create(), Argument.create()));
            TruffleObject language = (TruffleObject) readLanguage.executeForeign(frame, multilanguageObject, methodName);
            if (language != null) {
                // EXECUTE(READ(...),...) on language
                return new CachedForeignGlobalDispatchNode(getContext(), first, methodName, language, argumentsObjects.length);
            }
        }
        return null;
    }

    private DispatchNode createMethodMissingNode(
            DispatchNode first,
            Object methodName,
            Object receiverObject) {
        // TODO (eregon, 26 Aug. 2015): should handle primitive types as well
        final Shape shape = (receiverObject instanceof DynamicObject) ? ((DynamicObject) receiverObject).getShape() : null;

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return new CachedBoxedReturnMissingDispatchNode(getContext(), methodName, first, shape,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), getDispatchAction());
            }

            case CALL_METHOD_MISSING: {
                final InternalMethod method = lookup(null, receiverObject, "method_missing", true);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                return new CachedBoxedMethodMissingDispatchNode(getContext(), methodName, first, shape,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), method, getDispatchAction());
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

}
