/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.threadlocal.ThreadLocalObject;

public class RubiniusLastStringReadNode extends RubyNode {

    @Override
    public Object execute(VirtualFrame frame) {
        return getLastString();
    }

    @TruffleBoundary
    private Object getLastString() {
        // Rubinius expects $_ to be thread-local, rather than frame-local.  If we see it in a method call, we need
        // to look to the caller's frame to get the correct value, otherwise it will be nil.
        final Frame callerFrame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.READ_ONLY, true);

        // TODO CS 4-Jan-16 - but it could be in higher frames!
        final FrameSlot slot = callerFrame.getFrameDescriptor().findFrameSlot("$_");

        try {
            final ThreadLocalObject threadLocalObject = (ThreadLocalObject) callerFrame.getObject(slot);
            return threadLocalObject.get();
        } catch (FrameSlotTypeException e) {
            throw new UnsupportedOperationException(e);
        }
    }

}
