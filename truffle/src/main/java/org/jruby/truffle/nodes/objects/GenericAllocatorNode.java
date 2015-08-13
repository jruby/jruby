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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

public abstract class GenericAllocatorNode extends CoreMethodArrayArgumentsNode {

    public GenericAllocatorNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization(guards = {
            "!isSingleton(rubyClass)",
            "cachedRubyClass == rubyClass"
    })
    public DynamicObject allocateCached(
            DynamicObject rubyClass,
            @Cached("rubyClass") DynamicObject cachedRubyClass,
            @Cached("getInstanceFactory(rubyClass)") DynamicObjectFactory factory) {
        return doAllocate(factory);
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(contains = "allocateCached", guards = "!isSingleton(rubyClass)")
    public DynamicObject allocateUncached(DynamicObject rubyClass) {
        return doAllocate(getInstanceFactory(rubyClass));
    }

    @Specialization(guards = "isSingleton(rubyClass)")
    public DynamicObject allocateSingleton(DynamicObject rubyClass) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeError("can't create instance of singleton class", this));
    }

    protected boolean isSingleton(DynamicObject rubyClass) {
        return ClassNodes.isSingleton(rubyClass);
    }

    protected DynamicObject doAllocate(DynamicObjectFactory instanceFactory) {
        return instanceFactory.newInstance();
    }

}
