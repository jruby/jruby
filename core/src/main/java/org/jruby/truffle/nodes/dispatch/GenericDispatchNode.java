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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.lookup.LookupNode;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.util.cli.Options;

import java.util.HashMap;
import java.util.Map;

public abstract class GenericDispatchNode extends DispatchNode {

    private final boolean ignoreVisibility;

    @Child protected IndirectCallNode callNode;
    @Child protected BoxingNode box;

    private final Map<MethodCacheKey, MethodCacheEntry> cache;
    @CompilerDirectives.CompilationFinal private boolean hasAnyMethodsMissing = false;

    @CompilerDirectives.CompilationFinal private boolean hasSeenSymbolAsMethodName = false;
    @CompilerDirectives.CompilationFinal private boolean hasSeenRubyStringAsMethodName = false;
    @CompilerDirectives.CompilationFinal private boolean hasSeenJavaStringAsMethodName = false;

    public GenericDispatchNode(RubyContext context, boolean ignoreVisibility) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        cache = new HashMap<>();
        callNode = Truffle.getRuntime().createIndirectCallNode();
        box = new BoxingNode(context, null, null);
    }

    public GenericDispatchNode(GenericDispatchNode prev) {
        super(prev);
        ignoreVisibility = prev.ignoreVisibility;
        cache = prev.cache;
        hasAnyMethodsMissing = prev.hasAnyMethodsMissing;
        callNode = prev.callNode;
        box = prev.box;
    }

    @Specialization(order=1)
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            RubyBasicObject callingSelf,
            RubyBasicObject receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        MethodCacheEntry entry = lookupInCache(receiverObject.getLookupNode(), methodName);

        if (entry == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            try {
                final RubyMethod method = lookup(callingSelf, receiverObject, methodName.toString(),
                        ignoreVisibility, dispatchAction);
                entry = new MethodCacheEntry(method, false);
            } catch (UseMethodMissingException e) {
                try {
                    final RubyMethod method = lookup(callingSelf, receiverObject, "method_missing", ignoreVisibility,
                            dispatchAction);
                    entry = new MethodCacheEntry(method, true);
                } catch (UseMethodMissingException e2) {
                    if (dispatchAction == Dispatch.DispatchAction.RESPOND) {
                        // TODO(CS): we should cache the fact that we would throw an exception - this will miss each time
                        getContext().getRuntime().getWarnings().warn(
                                IRubyWarnings.ID.TRUFFLE,
                                getEncapsulatingSourceSection().getSource().getName(),
                                getEncapsulatingSourceSection().getStartLine(),
                                "Lack of method_missing isn't cached and is being used for respond");
                        return false;
                    } else {
                        throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                                receiverObject.toString() + " didn't have a #method_missing", this));
                    }
                }
            }

            if (entry.isMethodMissing()) {
                hasAnyMethodsMissing = true;
            }

            if (cache.size() <= Options.TRUFFLE_DISPATCH_MEGAMORPHIC_MAX.load()) {
                cache.put(new MethodCacheKey(receiverObject.getLookupNode(), methodName), entry);
            }
        }

        if (dispatchAction == Dispatch.DispatchAction.CALL) {
            final Object[] argumentsObjectsArray = CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true);

            final Object[] argumentsToUse;

            if (hasAnyMethodsMissing && entry.isMethodMissing()) {
                final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjectsArray.length];

                final RubySymbol methodNameAsSymbol;

                if (hasSeenSymbolAsMethodName && methodName instanceof RubySymbol) {
                    methodNameAsSymbol = (RubySymbol) methodName;
                } else if (hasSeenRubyStringAsMethodName && methodName instanceof RubyString) {
                    methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
                } else if (hasSeenJavaStringAsMethodName && methodName instanceof String) {
                    methodNameAsSymbol = getContext().newSymbol((String) methodName);
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();

                    if (methodName instanceof RubySymbol) {
                        hasSeenSymbolAsMethodName = true;
                        methodNameAsSymbol = (RubySymbol) methodName;
                    } else if (methodName instanceof RubyString) {
                        hasSeenRubyStringAsMethodName = true;
                        methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
                    } else if (methodName instanceof String) {
                        hasSeenJavaStringAsMethodName = true;
                        methodNameAsSymbol = getContext().newSymbol((String) methodName);
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }

                modifiedArgumentsObjects[0] = methodNameAsSymbol;

                System.arraycopy(argumentsObjectsArray, 0, modifiedArgumentsObjects, 1, argumentsObjectsArray.length);
                argumentsToUse = modifiedArgumentsObjects;
            } else {
                argumentsToUse = argumentsObjectsArray;
            }

            return callNode.call(
                    frame,
                    entry.getMethod().getCallTarget(),
                    RubyArguments.pack(
                            entry.getMethod(),
                            entry.getMethod().getDeclarationFrame(),
                            receiverObject,
                            CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                            argumentsToUse));
        } else if (dispatchAction == Dispatch.DispatchAction.RESPOND) {
            if (hasAnyMethodsMissing) {
                return !entry.isMethodMissing();
            } else {
                return true;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Specialization(order=2)
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            Object callingSelf,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        return dispatch(
                frame,
                methodReceiverObject,
                box.box(callingSelf),
                box.box(receiverObject),
                methodName,
                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true),
                dispatchAction);
    }

    @CompilerDirectives.SlowPath
    public MethodCacheEntry lookupInCache(LookupNode lookupNode, Object methodName) {
        return cache.get(new MethodCacheKey(lookupNode, methodName));
    }

    private class MethodCacheKey {

        private final LookupNode lookupNode;
        private final Object methodName;

        private MethodCacheKey(LookupNode lookupNode, Object methodName) {
            this.lookupNode = lookupNode;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodCacheKey that = (MethodCacheKey) o;

            if (!lookupNode.equals(that.lookupNode)) return false;
            if (!methodName.equals(that.methodName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = lookupNode.hashCode();
            result = 31 * result + methodName.hashCode();
            return result;
        }

    }

    private class MethodCacheEntry {

        private final RubyMethod method;
        private final boolean methodMissing;

        private MethodCacheEntry(RubyMethod method, boolean methodMissing) {
            assert method != null;
            this.method = method;
            this.methodMissing = methodMissing;
        }

        public RubyMethod getMethod() {
            return method;
        }

        public boolean isMethodMissing() {
            return methodMissing;
        }
    }

}