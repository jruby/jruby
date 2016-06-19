/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.format;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@NodeChildren({
    @NodeChild(value = "spacePadding", type = FormatNode.class),
    @NodeChild(value = "zeroPadding", type = FormatNode.class),
    @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatIntegerBinaryNode extends FormatNode {

    private final char format;

    public FormatIntegerBinaryNode(RubyContext context, char format) {
        super(context);
        this.format = format;
    }

    @Specialization
    public byte[] format(int spacePadding,
                         int zeroPadding,
                         int value) {
        return getFormattedString(Integer.toBinaryString(value), spacePadding, zeroPadding, value < 0);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyBignum(value)")
    public byte[] format(int spacePadding, int zeroPadding, DynamicObject value) {
        final BigInteger bigInteger = Layouts.BIGNUM.getValue(value);
        return getFormattedString(bigInteger.toString(2), spacePadding, zeroPadding, bigInteger.signum() == -1);
    }

    @TruffleBoundary
    private static byte[] getFormattedString(String formatted, int spacePadding, int zeroPadding, boolean negative) {
        if (negative) {
            if(formatted.contains("0")){
                formatted = "..1" + formatted.substring(formatted.indexOf('0'), formatted.length());
            } else {
                formatted = "..1";
            }
        }
        while (formatted.length() < spacePadding) {
            formatted = " " + formatted;
        }
        while (formatted.length() < zeroPadding) {
            formatted = "0" + formatted;
        }
        return formatted.getBytes(StandardCharsets.US_ASCII);
    }

}
