/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNode;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.FrameOnStackMarker;

public class FrameOnStackNode extends RubyNode {

    @Child private RubyNode child;
    @Child private WriteFrameSlotNode writeMarker;

    public FrameOnStackNode(RubyContext context, SourceSection sourceSection, RubyNode child, FrameSlot markerSlot) {
        super(context, sourceSection);
        this.child = child;
        writeMarker = WriteFrameSlotNodeGen.create(markerSlot);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final FrameOnStackMarker marker = new FrameOnStackMarker();

        writeMarker.executeWrite(frame, marker);

        try {
            return child.execute(frame);
        } finally {
            marker.setNoLongerOnStack();
        }
    }

}
