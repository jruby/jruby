/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.threadlocal.ThreadLocalObject;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class RubiniusLastStringWriteNode extends RubyNode {

    @TruffleBoundary
    @Specialization
    public Object lastStringWrite(Object value) {
        // Rubinius expects $_ to be thread-local, rather than frame-local.  If we see it in a method call, we need
        // to look to the caller's frame to get the correct value, otherwise it will be nil.
        Frame callerFrame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.READ_WRITE, true);

        FrameSlot slot = callerFrame.getFrameDescriptor().findFrameSlot("$_");

        while (slot == null) {
            callerFrame = RubyArguments.getDeclarationFrame(callerFrame);

            if (callerFrame == null) {
                break;
            }

            slot = callerFrame.getFrameDescriptor().findFrameSlot("$_");
        }

        if (slot == null) {
            return value;
        }

        try {
            Object currentValue = callerFrame.getObject(slot);

            if (currentValue instanceof ThreadLocalObject) {
                ThreadLocalObject threadLocalObject = (ThreadLocalObject) currentValue;
                threadLocalObject.set(value);
            } else {
                callerFrame.setObject(slot, value);
            }
        } catch (FrameSlotTypeException e) {
            throw new UnsupportedOperationException(e);
        }

        return value;
    }

    protected abstract RubyNode getValue();

}
