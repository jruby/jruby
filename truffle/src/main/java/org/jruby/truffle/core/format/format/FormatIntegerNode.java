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
import org.jruby.util.ConvertBytes;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@NodeChildren({
        @NodeChild(value = "width", type = FormatNode.class),
    @NodeChild(value = "precision", type = FormatNode.class),
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatIntegerNode extends FormatNode {

    private final char format;
    private final boolean hasSpaceFlag;
    private final boolean hasZeroFlag;
    private final boolean hasPlusFlag;
    private final boolean hasMinusFlag;
    private final boolean hasFSharp;

    public FormatIntegerNode(RubyContext context, char format, boolean hasSpaceFlag, boolean hasZeroFlag, boolean hasPlusFlag, boolean hasMinusFlag, boolean hasFSharp) {
        super(context);
        this.format = format;
        this.hasSpaceFlag = hasSpaceFlag;
        this.hasZeroFlag = hasZeroFlag;
        this.hasPlusFlag = hasPlusFlag;
        this.hasMinusFlag = hasMinusFlag;
        this.hasFSharp = hasFSharp;
    }

    @Specialization
    public byte[] format(int width, int precision, int value) {
        String formatted;
        switch (this.format) {
            case 'X':
                formatted = Integer.toString(value, 16).toUpperCase();
                if (hasFSharp && value > 0) {
                    formatted = "0X" + formatted;
                }
                if (value < 0) {
                    formatted = "..F" + Integer.toString(-value, 16).toUpperCase();
                }
                break;
            case 'x':
                formatted = Integer.toString(value, 16);
                if (hasFSharp && value > 0) {
                    formatted = "0x" + formatted;
                }
                if (value < 0) {
                    formatted = "..f" + Integer.toString(-value, 16);
                }
                break;
            case 'o':
                formatted = Integer.toString(value, 8);
                if (hasFSharp && value > 0) {
                    formatted = "0" + formatted;
                }
                if (value < 0) {
                    formatted = "..7" + new String(elide(ConvertBytes.intToOctalBytes(value), format, precision));
                    ;
                }
                break;
            case 'd':
            case 'i':
            case 'u':
                if (value < 0) {
                    formatted = Long.toString(-value);
                } else {
                    formatted = Long.toString(value);
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return formatStart(width, precision, formatted, value < 0);
    }

    @Specialization
    public byte[] format(int width, int precision, long value) {
        String formatted;
        switch (this.format) {
            case 'X':
                formatted = Long.toString(value, 16).toUpperCase();
                if (hasFSharp && value > 0) {
                    formatted = "0X" + formatted;
                }
                if (value < 0) {
                    formatted = "..F" + Long.toString(-value, 16).toUpperCase();
                }
                break;
            case 'x':
                formatted = Long.toString(value, 16);
                if (hasFSharp && value > 0) {
                    formatted = "0x" + formatted;
                }
                if (value < 0) {
                    formatted = "..f" + Long.toString(-value, 16);
                }
                break;
            case 'o':
                formatted = Long.toString(value, 8);
                if (hasFSharp && value > 0) {
                    formatted = "0" + formatted;
                }
                if (value < 0) {
                    formatted = "..7" + new String(elide(ConvertBytes.longToOctalBytes(value), format, precision));
                }
                break;
            case 'd':
            case 'i':
            case 'u':
                if (value < 0) {
                    formatted = Long.toString(-value);
                } else {
                    formatted = Long.toString(value);
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return formatStart(width, precision, formatted, value < 0);
    }

    private static byte[] elide(byte[] bytes, char format, int precision) {
        return bytes;
    }


    private byte[] formatStart(int width, int precision, String formatted, boolean isNegative) {

        boolean leftJustified = hasMinusFlag;
        if (width < 0 && width != PrintfSimpleTreeBuilder.DEFAULT) {  // TODO handle default width better
            width = -width;
            leftJustified = true;
        }

        final boolean addNegative = isNegative && (format == 'd' || format == 'i' || format == 'u');

        if ((hasZeroFlag && !hasMinusFlag) || precision != PrintfSimpleTreeBuilder.DEFAULT) {
            int padZeros;
            if (precision != PrintfSimpleTreeBuilder.DEFAULT) {
                padZeros = precision;
            } else {
                padZeros = width;
                if (addNegative) {
                    padZeros -= 1;
                }
            }

            while (formatted.length() < padZeros) {
                formatted = "0" + formatted;
            }
        }

        if (addNegative) {
            formatted = "-" + formatted;
        }

        while (formatted.length() < width) {
            if (leftJustified) {
                formatted = formatted + " ";
            } else {
                formatted = " " + formatted;
            }
        }

        if (!isNegative) {
            if (hasSpaceFlag || hasPlusFlag) {
                if (!hasMinusFlag) {
                    if (hasPlusFlag) {
                        formatted = "+" + formatted;
                    } else {
                        formatted = " " + formatted;
                    }
                }
            }
        }

        return formatted.getBytes(StandardCharsets.US_ASCII);

    }

    @TruffleBoundary
    @Specialization(guards = "isRubyBignum(value)")
    public byte[] format(int width, int precision, DynamicObject value) {
        final BigInteger bigInteger = Layouts.BIGNUM.getValue(value);

        String formatted;

        switch (format) {
            case 'd':
            case 'i':
            case 'u':
                formatted = bigInteger.toString();
                break;

            case 'o':
                formatted = bigInteger.toString(8).toLowerCase(Locale.ENGLISH);
                break;

            case 'x':
                formatted = bigInteger.toString(16).toLowerCase(Locale.ENGLISH);
                break;

            case 'X':
                formatted = bigInteger.toString(16).toUpperCase(Locale.ENGLISH);
                break;

            default:
                throw new UnsupportedOperationException();
        }

        while (formatted.length() < precision) {
            formatted = "0" + formatted;
        }

        while (formatted.length() < width) {
            formatted = " " + formatted;
        }

        return formatted.getBytes(StandardCharsets.US_ASCII);
    }

}
