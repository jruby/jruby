/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.write.bytes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;

/**
 * Simply write bytes.
 */
@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class WritePaddedBytesNode extends FormatNode {

    private final ConditionProfile leftJustifiedProfile = ConditionProfile.createBinaryProfile();
    private final int padding;
    private final boolean leftJustified;

    public WritePaddedBytesNode(RubyContext context, int padding, boolean leftJustified) {
        super(context);
        this.padding = padding;
        this.leftJustified = leftJustified;
    }

    @Specialization
    public Object write(VirtualFrame frame, byte[] bytes) {
        if (leftJustifiedProfile.profile(leftJustified)) {
            return writeLeftJustified(frame, bytes);
        } else {
            return writeRightJustified(frame, bytes);
        }
    }

    private Object writeLeftJustified(VirtualFrame frame, byte[] bytes) {
        writeBytes(frame, bytes);

        for (int n = 0; n < padding - bytes.length; n++) {
            writeByte(frame, (byte) ' ');
        }

        return null;
    }

    private Object writeRightJustified(VirtualFrame frame, byte[] bytes) {
        for (int n = 0; n < padding - bytes.length; n++) {
            writeByte(frame, (byte) ' ');
        }

        writeBytes(frame, bytes);
        return null;
    }

}
