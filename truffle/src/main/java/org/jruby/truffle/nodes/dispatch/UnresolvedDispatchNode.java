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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNode;
import java.util.concurrent.Callable;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
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
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

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
            DispatchAction dispatchAction,
            RubyNode[] argumentNodes,
            ProcOrNullNode block,
            boolean isSplatted) {
        super(context, dispatchAction, argumentNodes, block, isSplatted);
        
        this.ignoreVisibility = ignoreVisibility;
        this.indirect = indirect;
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
            Object blockObject,
            final Object argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final DispatchNode dispatch = atomic(new Callable<DispatchNode>() {
            @Override
            public DispatchNode call() throws Exception {
                final DispatchNode first = getHeadNode().getFirstDispatchNode();


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

                if (depth == DISPATCH_POLYMORPHIC_MAX) {
                    newDispathNode = new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), argumentNodes, block, isSplatted, missingBehavior);
                } else {
                    depth++;
                    if (isRubyBasicObject(receiverObject)) {
                        newDispathNode = doRubyBasicObject(frame, first, receiverObject, methodName, argumentsObjects);
                    }
                    else if (isForeign(receiverObject)) {
                        return createForeign(argumentsObjects, first, methodName);
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

    private boolean isForeign(Object receiverObject) {
        return false;
    }

    private DispatchNode createForeign(Object argumentsObjects, DispatchNode first, Object methodName) {
        throw new UnsupportedOperationException();
    }

    private DispatchNode doUnboxedObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName) {
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
                return createMethodMissingNode(first, methodName, receiverObject);
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

                return new CachedBooleanDispatchNode(getContext(),
                        methodName, first,
                        falseUnmodifiedAssumption, null, falseMethod,
                        trueUnmodifiedAssumption, null, trueMethod, indirect, getDispatchAction(),
                        argumentNodes, block, isSplatted);
            } else {
                return new CachedUnboxedDispatchNode(getContext(),
                        methodName, first, receiverObject.getClass(),
                        getContext().getCoreLibrary().getLogicalClass(receiverObject).getUnmodifiedAssumption(), null, method, indirect, getDispatchAction(),
                        argumentNodes, block, isSplatted);

            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DispatchNode doRubyBasicObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName,
            Object argumentsObjects) {
        final DispatchAction dispatchAction = getDispatchAction();

        final RubyClass callerClass = ignoreVisibility ? null : getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));

        if (dispatchAction == DispatchAction.CALL_METHOD || dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
            final InternalMethod method = lookup(callerClass, receiverObject, methodName.toString(), ignoreVisibility);

            if (method == null) {
                final DispatchNode multilanguage = tryMultilanguage(frame, first, methodName, argumentsObjects);
                if (multilanguage != null) {
                    return multilanguage;
                }

                return createMethodMissingNode(first, methodName, receiverObject);
            }

            if (receiverObject instanceof RubySymbol) {
                return new CachedBoxedSymbolDispatchNode(getContext(), methodName, first, null, method, indirect, getDispatchAction(), argumentNodes, block, isSplatted);
            } else {
                return new CachedBoxedDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), null, method, indirect, getDispatchAction(), argumentNodes, block, isSplatted);
            }

        } else if (dispatchAction == DispatchAction.READ_CONSTANT) {
            final RubyModule module = (RubyModule) receiverObject;
            final RubyConstant constant = lookupConstant(module, methodName.toString(),
                    ignoreVisibility);

            if (constant == null) {
                return createConstantMissingNode(first, methodName, callerClass, module);
            }

            if (constant.isAutoload()) {
                if (requireNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    requireNode = insert(KernelNodesFactory.RequireNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
                }

                requireNode.require((RubyString) constant.getValue());

                return doRubyBasicObject(frame, first, receiverObject, methodName, argumentsObjects);
            }

            // The module, the "receiver" is an instance of its singleton class.
            // But we want to check the module assumption, not its singleton class assumption.
            return new CachedBoxedDispatchNode(getContext(), methodName, first,
                    module.getSingletonClass(null), module.getUnmodifiedAssumption(), constant.getValue(),
                    null, indirect, getDispatchAction(), argumentNodes, block, isSplatted);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DispatchNode tryMultilanguage(VirtualFrame frame, DispatchNode first,  Object methodName, Object argumentsObjects) {
        return null;
    }

    private DispatchNode createConstantMissingNode(
            DispatchNode first,
            Object methodName,
            RubyClass callerClass,
            RubyBasicObject receiverObject) {
        switch (missingBehavior) {
            case RETURN_MISSING: {
                return new CachedBoxedReturnMissingDispatchNode(getContext(), methodName, first,
                        receiverObject.getMetaClass(), indirect, getDispatchAction(), argumentNodes, block, isSplatted);
            }

            case CALL_CONST_MISSING: {
                final InternalMethod method = lookup(callerClass, receiverObject, "const_missing", ignoreVisibility);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #const_missing", this));
                }

                if (DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                    return new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), argumentNodes, block, isSplatted, missingBehavior);
                }

                return new CachedBoxedMethodMissingDispatchNode(getContext(), methodName, first,
                        receiverObject.getMetaClass(), method, DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT, getDispatchAction(), argumentNodes, block, isSplatted);
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

    private DispatchNode createMethodMissingNode(
            DispatchNode first,
            Object methodName,
            Object receiverObject) {
        switch (missingBehavior) {
            case RETURN_MISSING: {
                return new CachedBoxedReturnMissingDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), indirect, getDispatchAction(), argumentNodes, block, isSplatted);
            }

            case CALL_METHOD_MISSING: {
                final InternalMethod method = lookup(null, receiverObject, "method_missing", true);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                if (DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                    return new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), argumentNodes, block, isSplatted, missingBehavior);
                }

                return new CachedBoxedMethodMissingDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), method, Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT.load(), getDispatchAction(), argumentNodes, block, isSplatted);
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

}
