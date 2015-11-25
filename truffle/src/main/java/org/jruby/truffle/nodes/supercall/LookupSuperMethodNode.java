/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.MetaClassNode;
import org.jruby.truffle.nodes.objects.MetaClassNodeGen;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Caches {@link ModuleOperations#lookupSuperMethod}
 * on an actual instance.
 */
@NodeChild("self")
public abstract class LookupSuperMethodNode extends RubyNode {

    @Child MetaClassNode metaClassNode;

    public LookupSuperMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
    }

    public abstract InternalMethod executeLookupSuperMethod(VirtualFrame frame, Object self);

    // The check for same metaClass is overly restrictive,
    // but seems the be the only reasonable check in term of performance.
    // The ideal condition would be to check if both ancestor lists starting at
    // the current method's module are identical, which is non-trivial
    // if the current method's module is an (included) module and not a class.

    @Specialization(guards = {
            "getCurrentMethod(frame) == currentMethod",
            "self.getShape() == cachedShape"
    },
            assumptions = "getUnmodifiedAssumption(selfMetaClass)",
            limit = "getCacheLimit()")
    protected InternalMethod lookupSuperMethodCachedDynamicObject(VirtualFrame frame, DynamicObject self,
            @Cached("getCurrentMethod(frame)") InternalMethod currentMethod,
            @Cached("self.getShape()") Shape cachedShape,
            @Cached("metaClass(self)") DynamicObject selfMetaClass,
            @Cached("doLookup(currentMethod, selfMetaClass)") InternalMethod superMethod) {
        return superMethod;
    }

    @Specialization(guards = {
            "getCurrentMethod(frame) == currentMethod",
            "!isDynamicObject(self)",
            "self.getClass() == cachedClass"
    },
            assumptions = "getUnmodifiedAssumption(selfMetaClass)",
            limit = "getCacheLimit()")
    protected InternalMethod lookupSuperMethodCachedPrimitive(VirtualFrame frame, Object self,
            @Cached("getCurrentMethod(frame)") InternalMethod currentMethod,
            @Cached("self.getClass()") Class<?> cachedClass,
            @Cached("metaClass(self)") DynamicObject selfMetaClass,
            @Cached("doLookup(currentMethod, selfMetaClass)") InternalMethod superMethod) {
        return superMethod;
    }

    @Specialization
    protected InternalMethod lookupSuperMethodUncached(VirtualFrame frame, Object self) {
        final InternalMethod currentMethod = getCurrentMethod(frame);
        final DynamicObject selfMetaClass = metaClass(self);
        return doLookup(currentMethod, selfMetaClass);
    }

    public Assumption getUnmodifiedAssumption(DynamicObject module) {
        return Layouts.MODULE.getFields(module).getUnmodifiedAssumption();
    }

    protected InternalMethod getCurrentMethod(VirtualFrame frame) {
        return RubyArguments.getMethod(frame.getArguments());
    }

    protected DynamicObject metaClass(Object object) {
        return metaClassNode.executeMetaClass(object);
    }

    protected InternalMethod doLookup(InternalMethod currentMethod, DynamicObject selfMetaClass) {
        assert RubyGuards.isRubyClass(selfMetaClass);
        InternalMethod superMethod = ModuleOperations.lookupSuperMethod(currentMethod, selfMetaClass);
        // TODO (eregon, 12 June 2015): Is this correct?
        if (superMethod != null && superMethod.isUndefined()) {
            superMethod = null;
        }
        return superMethod;
    }

    protected int getCacheLimit() {
        return getContext().getOptions().METHOD_LOOKUP_CACHE;
    }

}
