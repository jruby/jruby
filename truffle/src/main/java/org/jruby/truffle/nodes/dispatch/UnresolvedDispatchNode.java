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
import com.oracle.truffle.api.frame.VirtualFrame;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.util.cli.Options;

public final class UnresolvedDispatchNode extends DispatchNode {

    private int depth = 0;

    private final boolean ignoreVisibility;
    private final boolean indirect;
    private final MissingBehavior missingBehavior;

    @Child private KernelNodes.RequireNode requireNode;

    public UnresolvedDispatchNode(
            RubyContext context,
            boolean ignoreVisibility,
            boolean indirect,
            MissingBehavior missingBehavior,
            DispatchAction dispatchAction) {
        super(context, dispatchAction);
        this.ignoreVisibility = ignoreVisibility;
        this.indirect = indirect;
        this.missingBehavior = missingBehavior;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (depth == DISPATCH_POLYMORPHIC_MAX) {
            return getHeadNode().getFirstDispatchNode()
                    .replace(new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), missingBehavior))
                    .executeDispatch(frame, receiverObject,
                            methodName, blockObject, argumentsObjects);
        }

        depth++;

        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        if (isRubyBasicObject(receiverObject)) {
            return doRubyBasicObject(
                    frame,
                    first,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects);
        } else {
            return doUnboxedObject(
                    frame,
                    first,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects);
        }
    }

    private Object doUnboxedObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        final DispatchAction dispatchAction = getDispatchAction();

        final RubyClass callerClass;

        if (ignoreVisibility) {
            callerClass = null;
        } else {
            callerClass = getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));
        }

        if (dispatchAction == DispatchAction.CALL_METHOD || dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
            final InternalMethod method = lookup(callerClass, receiverObject, methodName.toString(), ignoreVisibility);

            if (method == null) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, receiverObject);
                return newDispatch.executeDispatch(frame, receiverObject,
                        methodName, blockObject, argumentsObjects);
            }

            if (receiverObject instanceof Boolean) {
                final Assumption falseUnmodifiedAssumption =
                        getContext().getCoreLibrary().getFalseClass().getUnmodifiedAssumption();

                final InternalMethod falseMethod =
                        lookup(callerClass, false, methodName.toString(),
                                ignoreVisibility);

                final Assumption trueUnmodifiedAssumption =
                        getContext().getCoreLibrary().getTrueClass().getUnmodifiedAssumption();

                final InternalMethod trueMethod =
                        lookup(callerClass, true, methodName.toString(),
                                ignoreVisibility);

                if ((falseMethod == null) && (trueMethod == null)) {
                    throw new UnsupportedOperationException();
                }

                final CachedBooleanDispatchNode newDispatch = new CachedBooleanDispatchNode(getContext(),
                        methodName, first,
                        falseUnmodifiedAssumption, null, falseMethod,
                        trueUnmodifiedAssumption, null, trueMethod, indirect, getDispatchAction());

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, receiverObject,
                        methodName, blockObject, argumentsObjects);
            } else {
                final CachedUnboxedDispatchNode newDispatch = new CachedUnboxedDispatchNode(getContext(),
                        methodName, first, receiverObject.getClass(),
                        getContext().getCoreLibrary().getLogicalClass(receiverObject).getUnmodifiedAssumption(), null, method, indirect, getDispatchAction());

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, receiverObject, methodName,
                        blockObject, argumentsObjects);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Object doRubyBasicObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        final DispatchAction dispatchAction = getDispatchAction();

        final RubyClass callerClass = ignoreVisibility ? null : getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));

        if (dispatchAction == DispatchAction.CALL_METHOD || dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
            final InternalMethod method = lookup(callerClass, receiverObject, methodName.toString(), ignoreVisibility);

            if (method == null) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, receiverObject);
                return newDispatch.executeDispatch(frame, receiverObject,
                        methodName, blockObject, argumentsObjects);
            }

            final DispatchNode newDispatch;

            if (receiverObject instanceof RubySymbol) {
                newDispatch = new CachedBoxedSymbolDispatchNode(getContext(), methodName, first, null, method, indirect, getDispatchAction());
            } else {
                newDispatch = new CachedBoxedDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), null, method, indirect, getDispatchAction());
            }

            first.replace(newDispatch);
            return newDispatch.executeDispatch(frame, receiverObject,
                    methodName, blockObject, argumentsObjects);

        } else if (dispatchAction == DispatchAction.READ_CONSTANT) {
            final RubyModule module = (RubyModule) receiverObject;
            final RubyConstant constant = lookupConstant(module, methodName.toString(),
                    ignoreVisibility);

            if (constant == null) {
                final DispatchNode newDispatch = createConstantMissingNode(methodName, callerClass, module);
                return newDispatch.executeDispatch(frame, module,
                        methodName, blockObject, argumentsObjects);
            }

            if (constant.isAutoload()) {
                if (requireNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    requireNode = insert(KernelNodesFactory.RequireNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
                }

                requireNode.require((RubyString) constant.getValue());

                return doRubyBasicObject(frame, first, receiverObject, methodName, blockObject, argumentsObjects);
            }

            // The module, the "receiver" is an instance of its singleton class.
            // But we want to check the module assumption, not its singleton class assumption.
            final DispatchNode newDispatch = new CachedBoxedDispatchNode(getContext(), methodName, first,
                    module.getSingletonClass(null), module.getUnmodifiedAssumption(), constant.getValue(),
                    null, indirect, getDispatchAction());

            first.replace(newDispatch);
            return newDispatch.executeDispatch(frame, receiverObject,
                    methodName, blockObject, argumentsObjects);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DispatchNode createConstantMissingNode(
            Object methodName,
            RubyClass callerClass,
            RubyBasicObject receiverObject) {
        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return first.replace(new CachedBoxedReturnMissingDispatchNode(getContext(), methodName, first,
                        receiverObject.getMetaClass(), indirect, getDispatchAction()));
            }

            case CALL_CONST_MISSING: {
                final InternalMethod method = lookup(callerClass, receiverObject, "const_missing", ignoreVisibility);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #const_missing", this));
                }

                if (DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                    return first.replace(new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), missingBehavior));
                }

                return first.replace(new CachedBoxedMethodMissingDispatchNode(getContext(), methodName, first,
                        receiverObject.getMetaClass(), method, DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT, getDispatchAction()));
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

    private DispatchNode createMethodMissingNode(
            Object methodName,
            Object receiverObject) {
        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return first.replace(new CachedBoxedReturnMissingDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), indirect, getDispatchAction()));
            }

            case CALL_METHOD_MISSING: {
                final InternalMethod method = lookup(null, receiverObject, "method_missing", true);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                if (DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                    return first.replace(new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), missingBehavior));
                }

                return first.replace(new CachedBoxedMethodMissingDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), method, DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT, getDispatchAction()));
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

}
