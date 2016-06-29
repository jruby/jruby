/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.MetaClassNode;
import org.jruby.truffle.language.objects.MetaClassNodeGen;

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

    public abstract InternalMethod executeLookupMethod(Object self, String name);

    @Specialization(
            guards = {
                    "metaClass(self) == selfMetaClass",
                    "name == cachedName"
            },
            assumptions = "getUnmodifiedAssumption(selfMetaClass)",
            limit = "getCacheLimit()")
    protected InternalMethod lookupMethodCached(Object self, String name,
            @Cached("metaClass(self)") DynamicObject selfMetaClass,
            @Cached("name") String cachedName,
            @Cached("doLookup(selfMetaClass, name)") InternalMethod method) {
        return method;
    }

    public Assumption getUnmodifiedAssumption(DynamicObject module) {
        return Layouts.MODULE.getFields(module).getUnmodifiedAssumption();
    }

    @Specialization
    protected InternalMethod lookupMethodUncached(Object self, String name) {
        final DynamicObject selfMetaClass = metaClass(self);
        return doLookup(selfMetaClass, name);
    }

    protected DynamicObject metaClass(Object object) {
        return metaClassNode.executeMetaClass(object);
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

    protected int getCacheLimit() {
        return getContext().getOptions().METHOD_LOOKUP_CACHE;
    }

}
