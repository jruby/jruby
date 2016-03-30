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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.printf.PrintfTreeBuilder;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatFloatNode extends FormatNode {

    private final int spacePadding;
    private final int zeroPadding;
    private final int precision;
    private final char format;

    public FormatFloatNode(RubyContext context, int spacePadding, int zeroPadding, int precision, char format) {
        super(context);
        this.spacePadding = spacePadding;
        this.zeroPadding = zeroPadding;
        this.precision = precision;
        this.format = format;
    }

    @Specialization
    @TruffleBoundary
    public byte[] format(double value) {
        // TODO CS 3-May-15 write this without building a string and formatting

        final StringBuilder builder = new StringBuilder();

        builder.append("%");

        if (Double.isInfinite(value)) {

            if (spacePadding != PrintfTreeBuilder.DEFAULT) {
                builder.append(" ");
                builder.append(spacePadding + 5);
            }

            if (zeroPadding != PrintfTreeBuilder.DEFAULT && zeroPadding != 0) {
                builder.append("0");
                builder.append(zeroPadding + 5);
            }

            builder.append(format);

            final String infinityString = String.format(builder.toString(), value);
            final String shortenInfinityString = infinityString.substring(0, infinityString.length() - 5);
            return shortenInfinityString.getBytes(StandardCharsets.US_ASCII);

        } else {

            if (spacePadding != PrintfTreeBuilder.DEFAULT) {
                builder.append(" ");
                builder.append(spacePadding);

                if (zeroPadding != PrintfTreeBuilder.DEFAULT) {
                    builder.append(".");
                    builder.append(zeroPadding);
                }
            } else if (zeroPadding != PrintfTreeBuilder.DEFAULT && zeroPadding != 0) {
                builder.append("0");
                builder.append(zeroPadding);
            }

            if (precision != PrintfTreeBuilder.DEFAULT) {
                builder.append(".");
                builder.append(precision);
            }

            builder.append(format);

            return String.format(builder.toString(), value).getBytes(StandardCharsets.US_ASCII);
        }
    }

}
