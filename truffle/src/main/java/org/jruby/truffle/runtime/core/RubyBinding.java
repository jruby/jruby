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

import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

/**
 * Represents the Ruby {@code Binding} class.
 */
public class RubyBinding extends RubyBasicObject {

    private final Object self;
    private final MaterializedFrame frame;

    public RubyBinding(RubyClass bindingClass, Object self, MaterializedFrame frame) {
        super(bindingClass);

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

}
