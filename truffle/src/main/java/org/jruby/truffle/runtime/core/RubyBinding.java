/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

/**
 * Represents the Ruby {@code Binding} class.
 */
public class RubyBinding extends RubyBasicObject {

    @CompilerDirectives.CompilationFinal private Object self;
    @CompilerDirectives.CompilationFinal private MaterializedFrame frame;

    public RubyBinding(RubyClass bindingClass) {
        super(bindingClass);
    }

    public RubyBinding(RubyClass bindingClass, Object self, MaterializedFrame frame) {
        super(bindingClass);

        initialize(self, frame);
    }

    public void initialize(Object self, MaterializedFrame frame) {
        assert self != null;
        assert frame != null;

        this.self = self;
        this.frame = frame;
    }

    public Object getSelf() {
        return self;
    }

    public MaterializedFrame getFrame() {
        return frame;
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        getContext().getObjectSpaceManager().visitFrame(frame, visitor);
    }

    public static class BindingAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBinding(rubyClass);
        }
    }

}
