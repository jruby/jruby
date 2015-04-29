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

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;

public class SetMethodDeclarationContext extends RubyNode {

    @Child private RubyNode child;

    final Visibility visibility;
    final String what;

    public SetMethodDeclarationContext(RubyContext context, SourceSection sourceSection, Visibility visibility, String what, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
        this.visibility = visibility;
        this.what = what;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        FrameSlot slot = frame.getFrameDescriptor().findOrAddFrameSlot(RubyModule.VISIBILITY_FRAME_SLOT_ID, "visibility for " + what, FrameSlotKind.Object);
        Object oldVisibility = frame.getValue(slot);

        try {
            frame.setObject(slot, visibility);

            return child.execute(frame);
        } finally {
            frame.setObject(slot, oldVisibility);
        }
    }

}
