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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyNode;

/**
 * Caches {@link ModuleOperations#canBindMethodTo} for a method.
 */
@NodeChildren({
        @NodeChild("method"),
        @NodeChild("module")
})
public abstract class CanBindMethodToModuleNode extends RubyNode {

    public abstract boolean executeCanBindMethodToModule(InternalMethod method, DynamicObject module);

    @Specialization(
            guards = { "isRubyModule(module)", "method.getDeclaringModule() == declaringModule", "module == cachedModule" },
            limit = "getCacheLimit()")
    protected boolean canBindMethodToCached(InternalMethod method, DynamicObject module,
            @Cached("method.getDeclaringModule()") DynamicObject declaringModule,
            @Cached("module") DynamicObject cachedModule,
            @Cached("canBindMethodTo(declaringModule, cachedModule)") boolean canBindMethodTo) {
        return canBindMethodTo;
    }

    @Specialization(guards = "isRubyModule(module)")
    protected boolean canBindMethodToUncached(InternalMethod method, DynamicObject module) {
        final DynamicObject declaringModule = method.getDeclaringModule();
        return canBindMethodTo(declaringModule, module);
    }

    protected boolean canBindMethodTo(DynamicObject declaringModule, DynamicObject module) {
        return ModuleOperations.canBindMethodTo(declaringModule, module);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().BIND_CACHE;
    }

}
