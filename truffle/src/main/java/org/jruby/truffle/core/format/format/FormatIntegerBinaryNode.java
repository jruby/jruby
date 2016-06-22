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
import org.jruby.truffle.core.format.printf.PrintfTreeBuilder;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@NodeChildren({
    @NodeChild(value = "spacePadding", type = FormatNode.class),
    @NodeChild(value = "zeroPadding", type = FormatNode.class),
    @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatIntegerBinaryNode extends FormatNode {

    private final char format;
    private final boolean hasPlusFlag;
    private final boolean useAlternativeFormat;
    private final boolean isLeftJustified;
    private final boolean hasSpaceFlag;

    public FormatIntegerBinaryNode(RubyContext context, char format, boolean hasPlusFlag, boolean useAlternativeFormat,
                                   boolean isLeftJustified, boolean hasSpaceFlag) {
        super(context);
        this.format = format;
        this.hasPlusFlag = hasPlusFlag;
        this.useAlternativeFormat = useAlternativeFormat;
        this.isLeftJustified = isLeftJustified;
        this.hasSpaceFlag = hasSpaceFlag;
    }

    @Specialization
    public byte[] format(int spacePadding,
                         int zeroPadding,
                         int value) {
        final boolean isSpacePadded = spacePadding != PrintfTreeBuilder.DEFAULT;
        final boolean isNegative = value < 0;
        final boolean negativeAndPadded = isNegative && (isSpacePadded || this.hasPlusFlag);
        final String formatted = negativeAndPadded ? Integer.toBinaryString(-value) : Integer.toBinaryString(value);
        return getFormattedString(formatted, spacePadding, zeroPadding, isNegative, isSpacePadded, this.hasPlusFlag,
            this.useAlternativeFormat, this.format, this.isLeftJustified, this.hasSpaceFlag);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyBignum(value)")
    public byte[] format(int spacePadding, int zeroPadding, DynamicObject value) {
        final boolean isSpacePadded = spacePadding != PrintfTreeBuilder.DEFAULT;
        final BigInteger bigInteger = Layouts.BIGNUM.getValue(value);
        final boolean isNegative = bigInteger.signum() == -1;
        final boolean negativeAndPadded = isNegative && (isSpacePadded || this.hasPlusFlag);
        final String formatted = negativeAndPadded ? bigInteger.abs().toString(2) : bigInteger.toString(2);
        return getFormattedString(formatted, spacePadding, zeroPadding, isNegative, isSpacePadded, this.hasPlusFlag,
            this.useAlternativeFormat, this.format, this.isLeftJustified, this.hasSpaceFlag);
    }

    @TruffleBoundary
    private static byte[] getFormattedString(String formatted, int spacePadding, int zeroPadding, boolean isNegative,
                                             boolean isSpacePadded, boolean hasPlusFlag, boolean useAlternativeFormat,
                                             char format, boolean leftJustified, boolean hasSpaceFlag) {
        final boolean isLeftJustified = leftJustified || spacePadding < 0;
        if(spacePadding < 0){
            spacePadding = -spacePadding;
        }
        if (isNegative && !(isSpacePadded || hasPlusFlag)) {
            if (formatted.contains("0")) {
                formatted = formatted.substring(formatted.indexOf('0'), formatted.length());
                if (formatted.length() + 3 < zeroPadding) {
                    final int addOnes = zeroPadding - (formatted.length() + 3);
                    for (int i = addOnes; i > 0; i--) {
                        formatted = "1" + formatted;
                    }
                }
                formatted = "..1" + formatted;
            } else {
                formatted = "..1";
            }
        } else {
            while (formatted.length() < zeroPadding) {
                formatted = "0" + formatted;
            }
        }

        if (useAlternativeFormat) {
            if(format == 'B'){
                formatted = "0B" + formatted;
            } else {
                formatted = "0b" + formatted;
            }
        }

        if (hasSpaceFlag || hasPlusFlag) {
            if (isNegative) {
                formatted = "-" + formatted;
            } else {
                if (hasPlusFlag) {
                    formatted = "+" + formatted;
                } else {
                    formatted = " " + formatted;
                }
            }
        }

        while (formatted.length() < spacePadding) {
            if(isLeftJustified){
                formatted = formatted + " ";
            } else {
                formatted = " " + formatted;
            }
        }

        return formatted.getBytes(StandardCharsets.US_ASCII);
    }

}
