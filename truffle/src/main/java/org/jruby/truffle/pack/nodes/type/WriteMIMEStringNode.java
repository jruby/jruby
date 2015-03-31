/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.runtime.Endianness;
import org.jruby.truffle.pack.runtime.Nil;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteMIMEStringNode extends PackNode {

    private final int length;

    public WriteMIMEStringNode(int length) {
        this.length = length;
    }

    @Specialization
    public Object write(VirtualFrame frame, Nil nil) {
        return null;
    }

    @Specialization
    public Object write(VirtualFrame frame, ByteList bytes) {
        writeBytes(frame, encode(bytes));
        return null;
    }

    @CompilerDirectives.TruffleBoundary
    private ByteList encode(ByteList bytes) {
        // TODO CS 30-Mar-15 should write our own optimisable version of MIME
        final ByteList output = new ByteList();
        Pack.qpencode(output, bytes, length);
        return output;
    }

}
