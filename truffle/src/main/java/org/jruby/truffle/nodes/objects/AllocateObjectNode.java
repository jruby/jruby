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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

@NodeChild("classToAllocate")
public abstract class AllocateObjectNode extends RubyNode {

    public AllocateObjectNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeAllocate(DynamicObject classToAllocate);

    @Specialization(guards = {
            "!isSingleton(classToAllocate)",
            "cachedClassToAllocate == classToAllocate"
    })
    public DynamicObject allocateCached(
            DynamicObject classToAllocate,
            @Cached("classToAllocate") DynamicObject cachedClassToAllocate,
            @Cached("getInstanceFactory(classToAllocate)") DynamicObjectFactory factory) {
        return newInstance(factory);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(contains = "allocateCached", guards = "!isSingleton(classToAllocate)")
    public DynamicObject allocateUncached(DynamicObject classToAllocate) {
        return newInstance(getInstanceFactory(classToAllocate));
    }

    @Specialization(guards = "isSingleton(classToAllocate)")
    public DynamicObject allocateSingleton(DynamicObject classToAllocate) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeError("can't create instance of singleton class", this));
    }

    protected boolean isSingleton(DynamicObject classToAllocate) {
        return ClassNodes.isSingleton(classToAllocate);
    }

    protected DynamicObject newInstance(DynamicObjectFactory instanceFactory) {
        return instanceFactory.newInstance();
    }

}
