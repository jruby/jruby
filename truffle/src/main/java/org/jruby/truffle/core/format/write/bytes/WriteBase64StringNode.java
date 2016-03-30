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
import org.jruby.truffle.core.format.exceptions.NoImplicitConversionException;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class WriteBase64StringNode extends FormatNode {

    private final int length;
    private final boolean ignoreStar;

    public WriteBase64StringNode(RubyContext context, int length, boolean ignoreStar) {
        super(context);
        this.length = length;
        this.ignoreStar = ignoreStar;
    }

    @Specialization
    public Object write(long bytes) {
        throw new NoImplicitConversionException(bytes, "String");
    }

    @Specialization
    public Object write(VirtualFrame frame, byte[] bytes) {
        writeBytes(frame, encode(bytes));
        return null;
    }

    @TruffleBoundary
    private byte[] encode(byte[] bytes) {
        // TODO CS 30-Mar-15 should write our own optimisable version of Base64

        final ByteList output = new ByteList();
        Pack.encodeUM(null, new ByteList(bytes, false), length, ignoreStar, 'm', output);
        return output.bytes();
    }

}
