/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.nodes.read;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jcodings.Encoding;
import org.jruby.truffle.core.format.nodes.PackNode;
import org.jruby.truffle.core.format.nodes.SourceNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.Layouts;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@NodeChildren({
        @NodeChild(value = "value", type = SourceNode.class),
})
public abstract class ReadBitStringNode extends PackNode {

    private final ByteOrder byteOrder;
    private final boolean star;
    private final int length;

    public ReadBitStringNode(RubyContext context, ByteOrder byteOrder, boolean star, int length) {
        super(context);
        this.byteOrder = byteOrder;
        this.star = star;
        this.length = length;
    }

    @Specialization
    public Object read(VirtualFrame frame, byte[] source) {
        // Bit string logic copied from jruby.util.Pack - see copyright and authorship there

        final ByteBuffer encode = ByteBuffer.wrap(source, getSourcePosition(frame), getSourceLength(frame) - getSourcePosition(frame));

        int occurrences = length;
        byte[] lElem;

        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            if (star || occurrences > encode.remaining() * 8) {
                occurrences = encode.remaining() * 8;
            }
            int bits = 0;
            lElem = new byte[occurrences];
            for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
                if ((lCurByte & 7) != 0) {
                    bits <<= 1;
                } else {
                    bits = encode.get();
                }
                lElem[lCurByte] = (bits & 128) != 0 ? (byte)'1' : (byte)'0';
            }
        } else {
            if (star || occurrences > encode.remaining() * 8) {
                occurrences = encode.remaining() * 8;
            }
            int bits = 0;
            lElem = new byte[occurrences];
            for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
                if ((lCurByte & 7) != 0) {
                    bits >>>= 1;
                } else {
                    bits = encode.get();
                }
                lElem[lCurByte] = (bits & 1) != 0 ? (byte)'1' : (byte)'0';
            }
        }

        final Encoding encoding = Encoding.load("ASCII");
        final ByteList result = new ByteList(lElem, encoding, false);
        setSourcePosition(frame, encode.position());

        return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), StringOperations.ropeFromByteList(result, StringSupport.CR_UNKNOWN), null);
    }

}
