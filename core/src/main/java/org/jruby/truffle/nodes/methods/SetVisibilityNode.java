/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;

public class SetVisibilityNode extends RubyNode {

    @Child protected RubyNode body;

    final Visibility visibility;

    public SetVisibilityNode(RubyContext context, SourceSection sourceSection, RubyNode body, Visibility visibility) {
        super(context, sourceSection);
        this.body = body;
        this.visibility = visibility;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();
        final FrameSlot visibilitySlot = frame.getFrameDescriptor().findFrameSlot(RubyModule.VISIBILITY_FRAME_SLOT_ID);

        frame.setObject(visibilitySlot, visibility);

        return body.execute(frame);
    }

}
