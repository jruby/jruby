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

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.nodes.*;
import org.jruby.truffle.pack.runtime.Endianness;
import org.jruby.truffle.pack.runtime.Signedness;

import java.nio.ByteOrder;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class IntegerNode extends PackNode {

    private final int size;
    private final Signedness signedness;
    private final Endianness endianness;

    public IntegerNode(int size, Signedness signedness, Endianness endianness) {
        this.size = size;
        this.signedness = signedness;
        this.endianness = endianness;
    }

    @Specialization(guards = {"is32Bit()", "isUnsigned()", "isLittle()"})
    public Object pack32UnsignedLittle(VirtualFrame frame, int[] source) {
        write32UnsignedLittle(frame, readInt(frame, source));
        return null;
    }

    @Specialization(guards = {"is32Bit()", "isUnsigned()", "isLittle()"})
    public Object pack32UnsignedLittle(VirtualFrame frame, IRubyObject[] source) {
        write32UnsignedLittle(frame, readInt(frame, source));
        return null;
    }

    @Specialization(guards = {"is32Bit()", "isUnsigned()", "isBig()"})
    public Object pack32UnsignedBig(VirtualFrame frame, int[] source) {
        write32UnsignedBig(frame, readInt(frame, source));
        return null;
    }

    @Specialization(guards = {"is32Bit()", "isUnsigned()", "isBig()"})
    public Object pack32UnsignedBig(VirtualFrame frame, IRubyObject[] source) {
        write32UnsignedBig(frame, readInt(frame, source));
        return null;
    }

    protected boolean is32Bit() {
        return size == 32;
    }

    protected boolean isUnsigned() {
        return signedness == Signedness.UNSIGNED;
    }

    protected boolean isLittle() {
        return endianness == Endianness.LITTLE;
    }

    protected boolean isBig() {
        return endianness == Endianness.BIG;
    }

}
