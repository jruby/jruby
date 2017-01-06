/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.objects.MetaClassNode;
import org.jruby.truffle.language.objects.MetaClassNodeGen;

/**
 * Caches {@link ModuleOperations#lookupMethod(DynamicObject, String)}
 * on an actual instance.
 */
@NodeChildren({ @NodeChild("self"), @NodeChild("name") })
public abstract class LookupMethodNode extends RubyNode {

    private final boolean ignoreVisibility;
    private final boolean onlyLookupPublic;

    @Child private MetaClassNode metaClassNode = MetaClassNodeGen.create(null);

    public LookupMethodNode(boolean ignoreVisibility, boolean onlyLookupPublic) {
        this.ignoreVisibility = ignoreVisibility;
        this.onlyLookupPublic = onlyLookupPublic;
    }

    public abstract InternalMethod executeLookupMethod(VirtualFrame frame, Object self, String name);

    @Specialization(
            guards = {
                    "metaClass(self) == selfMetaClass",
                    "name == cachedName"
            },
            assumptions = "getUnmodifiedAssumption(selfMetaClass)",
            limit = "getCacheLimit()")
    protected InternalMethod lookupMethodCached(VirtualFrame frame, Object self, String name,
            @Cached("metaClass(self)") DynamicObject selfMetaClass,
            @Cached("name") String cachedName,
            @Cached("doLookup(frame, self, name)") InternalMethod method) {
        return method;
    }

    protected Assumption getUnmodifiedAssumption(DynamicObject module) {
        return Layouts.MODULE.getFields(module).getUnmodifiedAssumption();
    }

    @Specialization
    protected InternalMethod lookupMethodUncached(VirtualFrame frame, Object self, String name) {
        return doLookup(frame, self, name);
    }

    protected DynamicObject metaClass(Object object) {
        return metaClassNode.executeMetaClass(object);
    }

    protected InternalMethod doLookup(VirtualFrame frame, Object self, String name) {
        return lookupMethodWithVisibility(getContext(), frame, self, name, ignoreVisibility, onlyLookupPublic);
    }

    @TruffleBoundary
    protected static InternalMethod doLookup(RubyContext context,
            DynamicObject callerClass, Object receiver, String name,
            boolean ignoreVisibility, boolean onlyLookupPublic) {

        final InternalMethod method = ModuleOperations.lookupMethod(context.getCoreLibrary().getMetaClass(receiver), name);

        // If no method was found, use #method_missing
        if (method == null) {
            return null;
        }

        // Check for methods that are explicitly undefined
        if (method.isUndefined()) {
            return null;
        }

        // Check visibility
        if (!ignoreVisibility) {
            if (onlyLookupPublic) {
                if (!method.getVisibility().isPublic()) {
                    return null;
                }
            } else if (!method.isVisibleTo(callerClass)) {
                return null;
            }
        }

        return method;
    }

    protected static DynamicObject getCallerClass(RubyContext context, VirtualFrame callingFrame,
            boolean ignoreVisibility, boolean onlyLookupPublic) {
        if (ignoreVisibility || onlyLookupPublic) {
            return null; // No need to check visibility
        } else {
            InternalMethod method = RubyArguments.getMethod(callingFrame);
            if (!context.getCoreLibrary().isSend(method)) {
                return context.getCoreLibrary().getMetaClass(RubyArguments.getSelf(callingFrame));
            } else {
                FrameInstance instance = context.getCallStack().getCallerFrameIgnoringSend();
                Frame callerFrame = instance.getFrame(FrameInstance.FrameAccess.READ_ONLY, true);
                return context.getCoreLibrary().getMetaClass(RubyArguments.getSelf(callerFrame));
            }
        }
    }

    public static InternalMethod lookupMethodWithVisibility(RubyContext context, VirtualFrame callingFrame,
            Object receiver, String name,
            boolean ignoreVisibility, boolean onlyLookupPublic) {
        DynamicObject callerClass = getCallerClass(context, callingFrame,
                ignoreVisibility, onlyLookupPublic);
        return doLookup(context, callerClass, receiver, name, ignoreVisibility, onlyLookupPublic);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().METHOD_LOOKUP_CACHE;
    }

}
