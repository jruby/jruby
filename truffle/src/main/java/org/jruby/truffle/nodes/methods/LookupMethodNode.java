/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.objects.MetaClassNode;
import org.jruby.truffle.nodes.objects.MetaClassNodeGen;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Caches {@link ModuleOperations#lookupMethod(DynamicObject, String)}
 * on an actual instance.
 */
@NodeChildren({ @NodeChild("self"), @NodeChild("name") })
public abstract class LookupMethodNode extends RubyNode {

    @Child MetaClassNode metaClassNode;

    public LookupMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
    }

    public abstract InternalMethod executeLookupMethod(VirtualFrame frame, Object self, String name);

    @Specialization(
            guards = {
                    "metaClass(frame, self) == selfMetaClass",
                    "name == cachedName"
            },
            assumptions = "getUnmodifiedAssumption(selfMetaClass)",
            limit = "getCacheLimit()")
    protected InternalMethod lookupMethodCached(VirtualFrame frame, Object self, String name,
            @Cached("metaClass(frame, self)") DynamicObject selfMetaClass,
            @Cached("name") String cachedName,
            @Cached("doLookup(selfMetaClass, name)") InternalMethod method) {
        return method;
    }

    public Assumption getUnmodifiedAssumption(DynamicObject module) {
        return ModuleNodes.getFields(module).getUnmodifiedAssumption();
    }

    @Specialization
    protected InternalMethod lookupMethodUncached(VirtualFrame frame, Object self, String name) {
        final DynamicObject selfMetaClass = metaClass(frame, self);
        return doLookup(selfMetaClass, name);
    }

    protected DynamicObject metaClass(VirtualFrame frame, Object object) {
        return metaClassNode.executeMetaClass(frame, object);
    }

    protected InternalMethod doLookup(DynamicObject selfMetaClass, String name) {
        assert RubyGuards.isRubyClass(selfMetaClass);
        InternalMethod method = ModuleOperations.lookupMethod(selfMetaClass, name);
        // TODO (eregon, 26 June 2015): Is this OK for all usages?
        if (method != null && method.isUndefined()) {
            method = null;
        }
        return method;
    }

}
