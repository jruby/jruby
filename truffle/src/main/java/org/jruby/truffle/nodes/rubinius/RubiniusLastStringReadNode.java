/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.ThreadLocalObject;
import org.jruby.util.Memo;

public class RubiniusLastStringReadNode extends RubyNode {

    public RubiniusLastStringReadNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Memo<Integer> frameCount = new Memo<>(0);

        // Rubinius expects $_ to be thread-local, rather than frame-local.  If we see it in a method call, we need
        // to look to the caller's frame to get the correct value, otherwise it will be nil.
        final MaterializedFrame callerFrame = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<MaterializedFrame>() {

            @Override
            public MaterializedFrame visitFrame(FrameInstance frameInstance) {
                if (frameCount.get() == 0) {
                    return frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE, false).materialize();
                } else {
                    frameCount.set(frameCount.get() + 1);
                    return null;
                }
            }

        });

        final FrameSlot slot = callerFrame.getFrameDescriptor().findFrameSlot("$_");
        try {
            final ThreadLocalObject threadLocalObject = (ThreadLocalObject) callerFrame.getObject(slot);
            return threadLocalObject.get();
        } catch (FrameSlotTypeException e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
