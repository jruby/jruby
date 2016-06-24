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

    public FormatFloatNode(RubyContext context, int width, int precision, char format, boolean hasSpaceFlag, boolean hasZeroFlag) {
        super(context);
        final StringBuilder inifiniteFormatBuilder = new StringBuilder();
        inifiniteFormatBuilder.append("%");

        if (hasSpaceFlag) {
            inifiniteFormatBuilder.append(" ");
            inifiniteFormatBuilder.append(width + 5);
        }
        if (hasZeroFlag && width != 0) {
            inifiniteFormatBuilder.append("0");
            inifiniteFormatBuilder.append(width + 5);
        }
        if(!hasSpaceFlag && !hasZeroFlag){
            inifiniteFormatBuilder.append(width + 5);
        }


        inifiniteFormatBuilder.append(format);

        infiniteFormatString = inifiniteFormatBuilder.toString();

        final StringBuilder finiteFormatBuilder = new StringBuilder();
        finiteFormatBuilder.append("%");

        if (hasSpaceFlag) {
            finiteFormatBuilder.append(" ");
            finiteFormatBuilder.append(width);

            if (hasZeroFlag) {
                finiteFormatBuilder.append(".");
                finiteFormatBuilder.append(width);
            }
        } else if (hasZeroFlag) {
            finiteFormatBuilder.append("0");
            finiteFormatBuilder.append(width);
        } else if (!hasSpaceFlag && !hasSpaceFlag) {
            finiteFormatBuilder.append(width);
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
