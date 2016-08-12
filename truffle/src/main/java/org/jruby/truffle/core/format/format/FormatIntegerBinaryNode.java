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
import org.jruby.truffle.core.format.printf.PrintfSimpleTreeBuilder;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@NodeChildren({
    @NodeChild(value = "width", type = FormatNode.class),
    @NodeChild(value = "precision", type = FormatNode.class),
    @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatIntegerBinaryNode extends FormatNode {

    private final char format;
    private final boolean hasPlusFlag;
    private final boolean useAlternativeFormat;
    private final boolean hasMinusFlag;
    private final boolean hasSpaceFlag;
    private final boolean hasZeroFlag;

    public FormatIntegerBinaryNode(RubyContext context, char format, boolean hasPlusFlag, boolean useAlternativeFormat,
                                   boolean hasMinusFlag, boolean hasSpaceFlag, boolean hasZeroFlag) {
        super(context);
        this.format = format;
        this.hasPlusFlag = hasPlusFlag;
        this.useAlternativeFormat = useAlternativeFormat;
        this.hasMinusFlag = hasMinusFlag;
        this.hasSpaceFlag = hasSpaceFlag;
        this.hasZeroFlag = hasZeroFlag;
    }

    @Specialization
    public byte[] format(int width, int precision, int value) {
        final boolean isNegative = value < 0;
        final boolean negativeAndPadded = isNegative && (this.hasSpaceFlag || this.hasPlusFlag);
        final String formatted = negativeAndPadded ? Integer.toBinaryString(-value) : Integer.toBinaryString(value);
        return getFormattedString(formatted, width, precision, isNegative, this.hasSpaceFlag, this.hasPlusFlag,
            this.hasZeroFlag, this.useAlternativeFormat, this.hasMinusFlag, this.format);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyBignum(value)")
    public byte[] format(int width, int precision, DynamicObject value) {
        final BigInteger bigInteger = Layouts.BIGNUM.getValue(value);
        final boolean isNegative = bigInteger.signum() == -1;
        final boolean negativeAndPadded = isNegative && (this.hasSpaceFlag || this.hasPlusFlag);

        final String formatted;
        if(negativeAndPadded) {
            formatted = bigInteger.abs().toString(2);
        } else if (!isNegative) {
            formatted = bigInteger.toString(2);
        } else {
            StringBuilder builder = new StringBuilder();
            final byte[] bytes = bigInteger.toByteArray();
            for(byte b: bytes){
                builder.append(Integer.toBinaryString(b & 0xFF));
            }
            formatted = builder.toString();
        }
        return getFormattedString(formatted, width, precision, isNegative, this.hasSpaceFlag, this.hasPlusFlag,
            this.hasZeroFlag, this.useAlternativeFormat, this.hasMinusFlag, this.format);
    }

    @TruffleBoundary
    private static byte[] getFormattedString(String formatted, int width, int precision, boolean isNegative,
                                             boolean isSpacePadded, boolean hasPlusFlag, boolean hasZeroFlag,
                                             boolean useAlternativeFormat, boolean hasMinusFlag,
                                             char format) {
        if(width < 0 && width != PrintfSimpleTreeBuilder.DEFAULT){
            width = -width;
            hasMinusFlag = true;
        }

        if (isNegative && !(isSpacePadded || hasPlusFlag)) {
            if (formatted.contains("0")) {
                formatted = formatted.substring(formatted.indexOf('0'), formatted.length());
                if (formatted.length() + 3 < precision) {
                    final int addOnes = precision - (formatted.length() + 3);
                    for (int i = addOnes; i > 0; i--) {
                        formatted = "1" + formatted;
                    }
                }
                formatted = "..1" + formatted;
            } else {
                formatted = "..1";
            }
        } else {
            if(hasZeroFlag || precision != PrintfSimpleTreeBuilder.DEFAULT) {
                if(!hasMinusFlag){
                    final int padZeros = precision != PrintfSimpleTreeBuilder.DEFAULT ? precision : width;
                    while (formatted.length() < padZeros) {
                        formatted = "0" + formatted;
                    }
                }
            }
        }

        while (formatted.length() < width) {
            if(!hasMinusFlag){
                formatted = " " + formatted;
            } else {
                formatted = formatted + " ";
            }

        }



        if (useAlternativeFormat) {
            if(format == 'B'){
                formatted = "0B" + formatted;
            } else {
                formatted = "0b" + formatted;
            }
        }

        if (isSpacePadded || hasPlusFlag) {
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

        return formatted.getBytes(StandardCharsets.US_ASCII);
    }

}
