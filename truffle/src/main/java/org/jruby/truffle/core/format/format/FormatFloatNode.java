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

    private final String infiniteFormatString;
    private final String finiteFormatString;

    public FormatFloatNode(RubyContext context, int spacePadding, int zeroPadding, int precision, char format) {
        super(context);

        final StringBuilder inifiniteFormatBuilder = new StringBuilder();
        inifiniteFormatBuilder.append("%");

        if (spacePadding != PrintfTreeBuilder.DEFAULT) {
            inifiniteFormatBuilder.append(" ");
            inifiniteFormatBuilder.append(spacePadding + 5);
        }

        if (zeroPadding != PrintfTreeBuilder.DEFAULT && zeroPadding != 0) {
            inifiniteFormatBuilder.append("0");
            inifiniteFormatBuilder.append(zeroPadding + 5);
        }

        inifiniteFormatBuilder.append(format);

        infiniteFormatString = inifiniteFormatBuilder.toString();

        final StringBuilder finiteFormatBuilder = new StringBuilder();
        finiteFormatBuilder.append("%");

        if (spacePadding != PrintfTreeBuilder.DEFAULT) {
            finiteFormatBuilder.append(" ");
            finiteFormatBuilder.append(spacePadding);

            if (zeroPadding != PrintfTreeBuilder.DEFAULT) {
                finiteFormatBuilder.append(".");
                finiteFormatBuilder.append(zeroPadding);
            }
        } else if (zeroPadding != PrintfTreeBuilder.DEFAULT && zeroPadding != 0) {
            finiteFormatBuilder.append("0");
            finiteFormatBuilder.append(zeroPadding);
        }

        if (precision != PrintfTreeBuilder.DEFAULT) {
            finiteFormatBuilder.append(".");
            finiteFormatBuilder.append(precision);
        }

        finiteFormatBuilder.append(format);

        finiteFormatString = finiteFormatBuilder.toString();
    }

    @TruffleBoundary
    @Specialization(guards = "isInfinite(value)")
    public byte[] formatInfinite(double value) {
        final String infinityString = String.format(infiniteFormatString, value);
        final String shortenInfinityString = infinityString.substring(0, infinityString.length() - 5);
        return shortenInfinityString.getBytes(StandardCharsets.US_ASCII);
    }

    @TruffleBoundary
    @Specialization(guards = "!isInfinite(value)")
    public byte[] formatFinite(double value) {
        return String.format(finiteFormatString, value).getBytes(StandardCharsets.US_ASCII);
    }

    protected boolean isInfinite(double value) {
        return Double.isInfinite(value);
    }

}
