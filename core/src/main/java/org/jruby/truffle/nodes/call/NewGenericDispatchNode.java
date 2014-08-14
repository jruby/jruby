/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.lookup.LookupNode;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.HashMap;
import java.util.Map;

public abstract class NewGenericDispatchNode extends NewDispatchNode {

    private final String name;
    private final Map<LookupNode, MethodCacheEntry> cache = new HashMap<>();
    @CompilerDirectives.CompilationFinal
    private boolean hasAnyMethodsMissing = false;

    @Child
    protected IndirectCallNode callNode;

    public NewGenericDispatchNode(RubyContext context, String name) {
        super(context);
        assert name != null;
        this.name = name;
        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    public NewGenericDispatchNode(NewGenericDispatchNode prev) {
        this(prev.getContext(), prev.name);
    }

    @Specialization(order=1)
    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, RubyBasicObject boxedCallingSelf, RubyBasicObject receiverObject, Object blockObject, Object argumentsObjects) {
        return doDispatch(frame, methodReceiverObject, boxedCallingSelf, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true));
    }

    private Object doDispatch(VirtualFrame frame, Object methodReceiverObject, RubyBasicObject boxedCallingSelf, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        MethodCacheEntry entry = lookupInCache(receiverObject.getLookupNode());

        if (entry == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            try {
                entry = new MethodCacheEntry(lookup(boxedCallingSelf, receiverObject, name), false);
            } catch (UseMethodMissingException e) {
                try {
                    entry = new MethodCacheEntry(lookup(boxedCallingSelf, receiverObject, "method_missing"), true);
                } catch (UseMethodMissingException e2) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(receiverObject.toString() + " didn't have a #method_missing", this));
                }
            }

            if (entry.isMethodMissing()) {
                hasAnyMethodsMissing = true;
            }

            cache.put(receiverObject.getLookupNode(), entry);

            if (cache.size() > RubyContext.GENERAL_DISPATCH_SIZE_WARNING_THRESHOLD) {
                final SourceSection sourceSection = getEncapsulatingSourceSection();

                // TODO(CS): figure out why we don't have proper source sections here

                if (sourceSection instanceof NullSourceSection) {
                    getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, "(unknown)", 0, "general call node cache has " + cache.size() + " entries");
                } else {
                    getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, getEncapsulatingSourceSection().getSource().getName(), getEncapsulatingSourceSection().getStartLine(), "general call node cache has " + cache.size() + " entries");
                }
            }
        }

        final Object[] argumentsToUse;

        if (hasAnyMethodsMissing && entry.isMethodMissing()) {
            final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjects.length];
            modifiedArgumentsObjects[0] = getContext().newSymbol(name);
            System.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjects.length);
            argumentsToUse = modifiedArgumentsObjects;
        } else {
            argumentsToUse = argumentsObjects;
        }

        return callNode.call(frame, entry.getMethod().getCallTarget(), RubyArguments.pack(entry.getMethod().getDeclarationFrame(), receiverObject, blockObject, argumentsToUse));
    }


    @Specialization(order=2)
    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        return dispatch(frame, methodReceiverObject, getContext().getCoreLibrary().box(callingSelf), getContext().getCoreLibrary().box(receiverObject), CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true));
    }


    @CompilerDirectives.SlowPath
    public MethodCacheEntry lookupInCache(LookupNode lookupNode) {
        return cache.get(lookupNode);
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