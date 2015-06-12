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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.objects.MetaClassNode;
import org.jruby.truffle.nodes.objects.MetaClassNodeGen;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.methods.InternalMethod;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

@NodeChild("self")
public abstract class LookupSuperMethodNode extends RubyNode {

    public static int getCacheLimit() {
        return DispatchNode.DISPATCH_POLYMORPHIC_MAX;
    }

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

    @Specialization(
            guards = {
                    "getCurrentMethod(frame) == currentMethod",
                    "metaClass(frame, self) == selfMetaClass"
            },
            assumptions = "selfMetaClass.getUnmodifiedAssumption()", // guards against include/prepend/method redefinition
            limit = "getCacheLimit()")
    protected InternalMethod lookupSuperMethodCached(VirtualFrame frame, Object self,
            @Cached("getCurrentMethod(frame)") InternalMethod currentMethod,
            @Cached("metaClass(frame, self)") RubyClass selfMetaClass,
            @Cached("doLookup(currentMethod, selfMetaClass)") InternalMethod superMethod) {
        return superMethod;
    }

    @Specialization
    protected InternalMethod lookupSuperMethodUncached(VirtualFrame frame, Object self) {
        InternalMethod currentMethod = getCurrentMethod(frame);
        RubyClass selfMetaClass = metaClass(frame, self);
        return doLookup(currentMethod, selfMetaClass);
    }

    protected InternalMethod getCurrentMethod(VirtualFrame frame) {
        return RubyArguments.getMethod(frame.getArguments());
    }

    protected RubyClass metaClass(VirtualFrame frame, Object object) {
        return metaClassNode.executeMetaClass(frame, object);
    }

    protected InternalMethod doLookup(InternalMethod currentMethod, RubyClass selfMetaClass) {
        InternalMethod superMethod = ModuleOperations.lookupSuperMethod(currentMethod, selfMetaClass);
        // TODO (eregon, 12 June 2015): Is this correct?
        if (superMethod != null && superMethod.isUndefined()) {
            superMethod = null;
        }
        return superMethod;
    }

}
