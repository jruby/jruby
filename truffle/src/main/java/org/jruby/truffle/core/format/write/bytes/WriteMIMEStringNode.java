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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.util.ByteList;
import org.jruby.util.PackUtils;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class WriteMIMEStringNode extends FormatNode {

    private final int length;

    public WriteMIMEStringNode(RubyContext context, int length) {
        super(context);
        this.length = length;
    }

    @Specialization(guards = "isNil(nil)")
    public Object write(Object nil) {
        return null;
    }

    @Specialization
    public Object write(VirtualFrame frame, byte[] bytes) {
        writeBytes(frame, encode(bytes));
        return null;
    }

    @TruffleBoundary
    private byte[] encode(byte[] bytes) {
        // TODO CS 30-Mar-15 should write our own optimizable version of MIME

        final ByteList output = new ByteList();
        PackUtils.qpencode(output, new ByteList(bytes, false), length);
        return output.bytes();
    }

}
