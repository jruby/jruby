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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.InternalMethod;

@NodeChildren({
        @NodeChild("method"),
        @NodeChild("module")
})
public abstract class CanBindMethodToModuleNode extends RubyNode {

    public CanBindMethodToModuleNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract boolean executeCanBindMethodToModule(VirtualFrame frame, InternalMethod method, RubyModule module);

    @Specialization(
            guards = { "method.getDeclaringModule() == declaringModule", "module == cachedModule" },
            limit = "getCacheLimit()")
    protected boolean canBindMethodToCached(VirtualFrame frame, InternalMethod method, RubyModule module,
            @Cached("method.getDeclaringModule()") RubyModule declaringModule,
            @Cached("module") RubyModule cachedModule,
            @Cached("canBindMethodTo(declaringModule, cachedModule)") boolean canBindMethodTo) {
        return canBindMethodTo;
    }

    @Specialization
    protected boolean canBindMethodToUncached(VirtualFrame frame, InternalMethod method, RubyModule module) {
        final RubyModule declaringModule = method.getDeclaringModule();
        return canBindMethodTo(declaringModule, module);
    }

    protected boolean canBindMethodTo(RubyModule declaringModule, RubyModule module) {
        return ModuleOperations.canBindMethodTo(declaringModule, module);
    }

}
