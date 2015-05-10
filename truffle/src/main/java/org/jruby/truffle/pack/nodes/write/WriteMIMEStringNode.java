/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.write;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;
import org.jruby.util.PackUtils;

/**
 * Read a string that contains MIME encoded data and write as actual binary
 * data.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteMIMEStringNode extends PackNode {

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
    public Object write(VirtualFrame frame, ByteList bytes) {
        writeBytes(frame, encode(bytes));
        return null;
    }

    @CompilerDirectives.TruffleBoundary
    private ByteList encode(ByteList bytes) {
        // TODO CS 30-Mar-15 should write our own optimizable version of MIME
        final ByteList output = new ByteList();
        PackUtils.qpencode(output, bytes, length);
        return output;
    }

}
