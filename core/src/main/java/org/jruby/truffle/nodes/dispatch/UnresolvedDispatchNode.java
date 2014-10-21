/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.util.cli.Options;

public final class UnresolvedDispatchNode extends DispatchNode {

    private final boolean ignoreVisibility;
    private final Dispatch.MissingBehavior missingBehavior;

    public UnresolvedDispatchNode(RubyContext context, boolean ignoreVisibility,
                                  Dispatch.MissingBehavior missingBehavior) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        this.missingBehavior = missingBehavior;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (getDepth() == Options.TRUFFLE_DISPATCH_POLYMORPHIC_MAX.load()) {
            return getHeadNode().getFirstDispatchNode()
                    .replace(GenericDispatchNodeFactory.create(getContext(), ignoreVisibility,
                            null, null, null, null, null, null, null))
                    .executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                            methodName, blockObject, argumentsObjects, dispatchAction);
        }

        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        if (receiverObject instanceof RubyBasicObject) {
            return doRubyBasicObject(
                    frame,
                    first,
                    (LexicalScope) lexicalScope,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects,
                    dispatchAction,
                    methodReceiverObject);
        } else {
            return doUnboxedObject(
                    frame,
                    first,
                    (LexicalScope) lexicalScope,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects,
                    dispatchAction,
                    methodReceiverObject);
        }
    }

    private Object doUnboxedObject(
            VirtualFrame frame,
            DispatchNode first,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction,
            Object methodReceiverObject) {
        final RubyBasicObject boxedReceiverObject = getContext().getCoreLibrary().box(receiverObject);
        final RubyClass callerClass = getContext().getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD || dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
            final RubyMethod method = lookup(callerClass, boxedReceiverObject, methodName.toString(), ignoreVisibility,
                    dispatchAction);

            if (method == null) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            if (receiverObject instanceof Boolean) {
                final Assumption falseUnmodifiedAssumption =
                        getContext().getCoreLibrary().getFalseClass().getUnmodifiedAssumption();

                final RubyMethod falseMethod =
                        lookup(callerClass, getContext().getCoreLibrary().box(false), methodName.toString(),
                                ignoreVisibility, dispatchAction);

                if (falseMethod == null) {
                    throw new UnsupportedOperationException();
                }

                final Assumption trueUnmodifiedAssumption =
                        getContext().getCoreLibrary().getTrueClass().getUnmodifiedAssumption();

                final RubyMethod trueMethod =
                        lookup(callerClass, getContext().getCoreLibrary().box(true), methodName.toString(),
                                ignoreVisibility, dispatchAction);

                if (trueMethod == null) {
                    throw new UnsupportedOperationException();
                }

                final CachedBooleanDispatchNode newDispatch = CachedBooleanDispatchNodeFactory.create(getContext(),
                        methodName, first,
                        falseUnmodifiedAssumption, null, falseMethod,
                        trueUnmodifiedAssumption, null, trueMethod,
                        null, null, null, null, null, null, null);

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            } else {
                final CachedUnboxedDispatchNode newDispatch = CachedUnboxedDispatchNodeFactory.create(getContext(),
                        methodName, first, receiverObject.getClass(),
                        boxedReceiverObject.getLogicalClass().getUnmodifiedAssumption(), null, method, null, null, null, null,
                        null, null, null);

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject, methodName,
                        blockObject, argumentsObjects, dispatchAction);
            }
        } else if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
            final RubyModule module = (RubyModule) boxedReceiverObject;
            final RubyConstant constant = lookupConstant(lexicalScope, module, methodName.toString(),
                    ignoreVisibility, dispatchAction);

            if (constant == null) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            if (receiverObject instanceof Boolean && false) {/*
                final Assumption falseUnmodifiedAssumption =
                        getContext().getCoreLibrary().getFalseClass().getUnmodifiedAssumption();

                final RubyConstant falseConstant =
                        lookupConstant(lexicalScope, getContext().getCoreLibrary().box(false),
                                methodName.toString(), ignoreVisibility, dispatchAction);

                if (falseConstant == null) {
                    throw new UnsupportedOperationException();
                }

                final Assumption trueUnmodifiedAssumption =
                        getContext().getCoreLibrary().getTrueClass().getUnmodifiedAssumption();

                final RubyConstant trueConstant =
                        lookupConstant(lexicalScope, getContext().getCoreLibrary().box(true),
                                methodName.toString(), ignoreVisibility, dispatchAction);

                if (trueConstant == null) {
                    throw new UnsupportedOperationException();
                }

                final CachedBooleanDispatchNode newDispatch = CachedBooleanDispatchNodeFactory.create(getContext(),
                        methodName, first,
                        falseUnmodifiedAssumption, falseConstant.getValue(), null,
                        trueUnmodifiedAssumption, trueConstant.getValue(), null,
                        null, null, null, null, null, null, null);

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);*/
                return constant.getValue(); // TODO: cache
            } else {
                return constant.getValue(); // TODO: cache

                /*final CachedUnboxedDispatchNode newDispatch = CachedUnboxedDispatchNodeFactory.create(getContext(),
                        methodName, first, receiverObject.getClass(),
                        boxedReceiverObject.getLogicalClass().getUnmodifiedAssumption(), constant.getValue(), null, null, null, null, null,
                        null, null, null);

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject, methodName,
                        blockObject, argumentsObjects, dispatchAction);*/
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Object doRubyBasicObject(
            VirtualFrame frame,
            DispatchNode first,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction,
            Object methodReceiverObject) {
        final RubyBasicObject boxedReceiverObject = getContext().getCoreLibrary().box(receiverObject);
        final RubyClass callerClass = getContext().getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD || dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
            final RubyMethod method = lookup(callerClass, boxedReceiverObject, methodName.toString(), ignoreVisibility,
                    dispatchAction);

            if (method == null) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            final DispatchNode newDispatch;

            if (receiverObject instanceof RubySymbol) {
                newDispatch = CachedBoxedSymbolDispatchNodeFactory.create(getContext(), methodName, first, null, method, null,
                        null, null, null, null, null, null);
            } else {
                newDispatch = CachedBoxedDispatchNodeFactory.create(getContext(), methodName, first,
                        boxedReceiverObject.getMetaClass(), null, method, null, null, null, null, null, null, null);
            }

            first.replace(newDispatch);
            return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                    methodName, blockObject, argumentsObjects, dispatchAction);
        } else if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
            final RubyModule module = (RubyModule) boxedReceiverObject;
            final RubyConstant constant = lookupConstant(lexicalScope, module, methodName.toString(),
                    ignoreVisibility, dispatchAction);

            if (constant == null) {
                final DispatchNode newDispatch = createConstantMissingNode(methodName, callerClass,
                        boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            return constant.getValue(); // TODO: cache
            /*final DispatchNode newDispatch;

            if (receiverObject instanceof RubySymbol) {
                newDispatch = CachedBoxedSymbolDispatchNodeFactory.create(getContext(), methodName, first, constant.getValue(), null, null,
                        null, null, null, null, null, null);
            } else {
                newDispatch = CachedBoxedDispatchNodeFactory.create(getContext(), methodName, first,
                        boxedReceiverObject.getMetaClass(), constant.getValue(), null, null, null, null, null, null, null, null);
            }

            first.replace(newDispatch);
            return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                    methodName, blockObject, argumentsObjects, dispatchAction);*/
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DispatchNode createConstantMissingNode(
            Object methodName,
            RubyClass callerClass,
            RubyBasicObject receiverObject,
            Dispatch.DispatchAction dispatchAction) {
        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return first.replace(CachedBoxedReturnMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), null, null, null, null, null, null, null));
            }

            case CALL_CONST_MISSING: {
                final RubyMethod method = lookup(callerClass, receiverObject, "const_missing", ignoreVisibility, dispatchAction);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #const_missing", this));
                }

                return first.replace(CachedBoxedMethodMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), method, null, null, null, null, null, null, null));
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

    private DispatchNode createMethodMissingNode(
            Object methodName,
            RubyBasicObject receiverObject,
            Dispatch.DispatchAction dispatchAction) {
        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return first.replace(CachedBoxedReturnMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), null, null, null, null, null, null, null));
            }

            case CALL_METHOD_MISSING: {
                final RubyMethod method = lookup(null, receiverObject, "method_missing", true, dispatchAction);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                return first.replace(CachedBoxedMethodMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), method, null, null, null, null, null, null, null));
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

    private int getDepth() {
        final DispatchHeadNode head = getHeadNode();
        Node parent = getParent();

        int depth = 1;

        while (parent != head) {
            depth++;
            parent = parent.getParent();
        }

        return depth;
    }

}
