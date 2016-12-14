/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.locals.WriteFrameSlotNode;
import org.jruby.truffle.language.locals.WriteFrameSlotNodeGen;

public class FrameOnStackNode extends RubyNode {

    @Child private RubyNode child;
    @Child private WriteFrameSlotNode writeMarker;

    public FrameOnStackNode(RubyNode child, FrameSlot markerSlot) {
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
