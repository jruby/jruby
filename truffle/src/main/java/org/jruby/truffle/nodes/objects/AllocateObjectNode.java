/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

@NodeChildren({
        @NodeChild("classToAllocate"),
        @NodeChild("values")
})
public abstract class AllocateObjectNode extends RubyNode {

    public AllocateObjectNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public DynamicObject allocate(DynamicObject classToAllocate, Object... values) {
        return executeAllocateX(classToAllocate, values);
    }

    public abstract DynamicObject executeAllocateX(DynamicObject classToAllocate, Object[] values);

    @Specialization(guards = {
            "cachedClassToAllocate == classToAllocate",
            "!cachedIsSingleton"
    })
    public DynamicObject allocateCached(
            DynamicObject classToAllocate,
            Object[] values,
            @Cached("classToAllocate") DynamicObject cachedClassToAllocate,
            @Cached("isSingleton(classToAllocate)") boolean cachedIsSingleton,
            @Cached("getInstanceFactory(classToAllocate)") DynamicObjectFactory factory) {
        return factory.newInstance(values);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(contains = "allocateCached", guards = "!isSingleton(classToAllocate)")
    public DynamicObject allocateUncached(DynamicObject classToAllocate, Object[] values) {
        return getInstanceFactory(classToAllocate).newInstance(values);
    }

    @Specialization(guards = "isSingleton(classToAllocate)")
    public DynamicObject allocateSingleton(DynamicObject classToAllocate, Object[] values) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeError("can't create instance of singleton class", this));
    }

    protected boolean isSingleton(DynamicObject classToAllocate) {
        return ClassNodes.isSingleton(classToAllocate);
    }

}
