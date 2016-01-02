/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.nodes.read;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubyString;
import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.nodes.SourceNode;
import org.jruby.truffle.format.runtime.exceptions.FormatException;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.StringSupport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@NodeChildren({
        @NodeChild(value = "value", type = SourceNode.class),
})
public abstract class ReadBase64StringNode extends PackNode {

    private final int requestedLength;

    public ReadBase64StringNode(RubyContext context, int requestedLength) {
        super(context);
        this.requestedLength = requestedLength;
    }

    @Specialization
    public Object read(VirtualFrame frame, byte[] source) {
        // Bit string logic copied from jruby.util.Pack - see copyright and authorship there

        final ByteBuffer encode = ByteBuffer.wrap(source, getSourcePosition(frame), getSourceLength(frame) - getSourcePosition(frame));

        int occurrences = requestedLength;

        int length = encode.remaining()*3/4;
        byte[] lElem = new byte[length];
        int a = -1, b = -1, c = 0, d;
        int index = 0;
        int s = -1;

        if (occurrences == 0){
            if (encode.remaining()%4 != 0) {
                throw new FormatException("invalid base64");
            }
            while (encode.hasRemaining() && s != '=') {
                a = b = c = -1;
                d = -2;

                // obtain a
                s = Pack.safeGet(encode);
                a = Pack.b64_xtable[s];
                if (a == -1) throw new FormatException("invalid base64");

                // obtain b
                s = Pack.safeGet(encode);
                b = Pack.b64_xtable[s];
                if (b == -1) throw new FormatException("invalid base64");

                // obtain c
                s = Pack.safeGet(encode);
                c = Pack.b64_xtable[s];
                if (s == '=') {
                    if (Pack.safeGet(encode) != '=') throw new FormatException("invalid base64");
                    break;
                }
                if (c == -1) throw new FormatException("invalid base64");

                // obtain d
                s = Pack.safeGet(encode);
                d = Pack.b64_xtable[s];
                if (s == '=') break;
                if (d == -1) throw new FormatException("invalid base64");

                // calculate based on a, b, c and d
                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                lElem[index++] = (byte)((c << 6 | d) & 255);
            }

            if (encode.hasRemaining()) throw new FormatException("invalid base64");

            if (a != -1 && b != -1) {
                if (c == -1 && s == '=') {
                    if ((b & 15) > 0) throw new FormatException("invalid base64");
                    lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                } else if(c != -1 && s == '=') {
                    if ((c & 3) > 0) throw new FormatException("invalid base64");
                    lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                    lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                }
            }
        }
        else {

            while (encode.hasRemaining()) {
                a = b = c = d = -1;

                // obtain a
                s = Pack.safeGet(encode);
                while (((a = Pack.b64_xtable[s]) == -1) && encode.hasRemaining()) {
                    s = Pack.safeGet(encode);
                }
                if (a == -1) break;

                // obtain b
                s = Pack.safeGet(encode);
                while (((b = Pack.b64_xtable[s]) == -1) && encode.hasRemaining()) {
                    s = Pack.safeGet(encode);
                }
                if (b == -1) break;

                // obtain c
                s = Pack.safeGet(encode);
                while (((c = Pack.b64_xtable[s]) == -1) && encode.hasRemaining()) {
                    if (s == '=') break;
                    s = Pack.safeGet(encode);
                }
                if ((s == '=') || c == -1) {
                    if (s == '=') {
                        encode.position(encode.position() - 1);
                    }
                    break;
                }

                // obtain d
                s = Pack.safeGet(encode);
                while (((d = Pack.b64_xtable[s]) == -1) && encode.hasRemaining()) {
                    if (s == '=') break;
                    s = Pack.safeGet(encode);
                }
                if ((s == '=') || d == -1) {
                    if (s == '=') {
                        encode.position(encode.position() - 1);
                    }
                    break;
                }

                // calculate based on a, b, c and d
                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                lElem[index++] = (byte)((c << 6 | d) & 255);
            }

            if (a != -1 && b != -1) {
                if (c == -1 && s == '=') {
                    lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                } else if(c != -1 && s == '=') {
                    lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                    lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                }
            }
        }

        final Encoding encoding = Encoding.load("ASCII");
        final ByteList result = new ByteList(lElem, 0, index, encoding, false);
        setSourcePosition(frame, encode.position());

        return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), result, StringSupport.CR_UNKNOWN, null);
    }

}
