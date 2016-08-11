/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is modified from org.jruby.util.Sprintf,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 *
 * Contains code modified from Sprintf.java
 *
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
import org.jruby.util.ByteList;
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

    private static final byte[] PREFIX_OCTAL = {'0'};
    private static final byte[] PREFIX_HEX_LC = {'0', 'x'};
    private static final byte[] PREFIX_HEX_UC = {'0', 'X'};
    private static final byte[] PREFIX_BINARY_LC = {'0', 'b'};
    private static final byte[] PREFIX_BINARY_UC = {'0', 'B'};

    private static final byte[] PREFIX_NEGATIVE = {'.', '.'};

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

    @TruffleBoundary
    @Specialization
    public byte[] format(int width, int precision, int arg) {
        return format(width, precision, (long) arg);
    }

    @TruffleBoundary
    @Specialization
    public byte[] format(int width, int precision, long arg) {

        ByteList buf = new ByteList();

        boolean usePrefixForZero = false;

        boolean hasMinusFlag = this.hasMinusFlag;
        if (width == PrintfSimpleTreeBuilder.DEFAULT) {
            width = 0;
        } else if (width < 0) {
            hasMinusFlag = true;
            width = -width;
        }

        if (precision < 0) {
            precision = PrintfSimpleTreeBuilder.DEFAULT;
        }

        byte[] bytes = null;
        int first = 0;
        byte[] prefix = null;
        boolean sign;
        boolean negative;
        byte signChar = 0;
        byte leadChar = 0;
        int base;

        char fchar = this.format;

        // 'd' and 'i' are the same
        if (fchar == 'i') fchar = 'd';

        // 'u' with space or plus flags is same as 'd'
        if (fchar == 'u' && (hasSpaceFlag || hasPlusFlag)) {
            fchar = 'd';
        }
        sign = (fchar == 'd' || (hasSpaceFlag || hasPlusFlag));

        switch (fchar) {
            case 'o':
                base = 8;
                break;
            case 'x':
            case 'X':
                base = 16;
                break;
            case 'b':
            case 'B':
                base = 2;
                break;
            case 'u':
            case 'd':
            default:
                base = 10;
                break;
        }

        boolean zero;

        negative = arg < 0;
        zero = arg == 0;
        if (negative && fchar == 'u') {
            bytes = getUnsignedNegativeBytes(arg);
        } else {
            bytes = getFixnumBytes(arg, base, sign, fchar == 'X');
        }

        if (hasFSharp) {
            if (!zero || usePrefixForZero) {
                switch (fchar) {
                    case 'o':
                        prefix = PREFIX_OCTAL;
                        break;
                    case 'x':
                        prefix = PREFIX_HEX_LC;
                        break;
                    case 'X':
                        prefix = PREFIX_HEX_UC;
                        break;
                    case 'b':
                        prefix = PREFIX_BINARY_LC;
                        break;
                    case 'B':
                        prefix = PREFIX_BINARY_UC;
                        break;
                }
            }
            if (prefix != null) width -= prefix.length;
        }
        int len = 0;
        if (sign) {
            if (negative) {
                signChar = '-';
                width--;
                first = 1; // skip '-' in bytes, will add where appropriate
            } else if (hasPlusFlag) {
                signChar = '+';
                width--;
            } else if (hasSpaceFlag) {
                signChar = ' ';
                width--;
            }
        } else if (negative) {
            if (base == 10) {
//                warning(ID.NEGATIVE_NUMBER_FOR_U, args, "negative number for %u specifier");
                leadChar = '.';
                len += 2;
            } else {
                if (!hasZeroFlag && precision == PrintfSimpleTreeBuilder.DEFAULT) len += 2; // ..

                first = skipSignBits(bytes, base);
                switch (fchar) {
                    case 'b':
                    case 'B':
                        leadChar = '1';
                        break;
                    case 'o':
                        leadChar = '7';
                        break;
                    case 'x':
                        leadChar = 'f';
                        break;
                    case 'X':
                        leadChar = 'F';
                        break;
                }
                if (leadChar != 0) len++;
            }
        }
        int numlen = bytes.length - first;
        len += numlen;


        boolean hasPrecisionFlag = precision != PrintfSimpleTreeBuilder.DEFAULT;

        //        if ((flags & (FLAG_ZERO|FLAG_PRECISION)) == FLAG_ZERO) {
        if (hasZeroFlag && !hasPrecisionFlag) {
            precision = width;
            width = 0;
        } else {
            if (precision < len) precision = len;

            width -= precision;
        }
        if (!hasMinusFlag) {
            buf.fill(' ', width);
            width = 0;
        }
        if (signChar != 0) buf.append(signChar);
        if (prefix != null) buf.append(prefix);

        if (len < precision) {
            if (leadChar == 0) {
                if (fchar != 'd' || usePrefixForZero || !negative ||
                    hasPrecisionFlag ||
                    (hasZeroFlag && !hasMinusFlag)) {
                    buf.fill('0', precision - len);
                }
            } else if (leadChar == '.') {
                buf.fill(leadChar, precision - len);
                buf.append(PREFIX_NEGATIVE);
            } else if (!usePrefixForZero) {
                buf.append(PREFIX_NEGATIVE);
                buf.fill(leadChar, precision - len - 1);
            } else {
                buf.fill(leadChar, precision - len + 1); // the 1 is for the stripped sign char
            }
        } else if (leadChar != 0) {
            if (((!hasZeroFlag && precision == PrintfSimpleTreeBuilder.DEFAULT) && usePrefixForZero) ||
                (!usePrefixForZero && "xXbBo".indexOf(fchar) != -1)) {
                buf.append(PREFIX_NEGATIVE);
            }
            if (leadChar != '.') buf.append(leadChar);
        }
        buf.append(bytes, first, numlen);

        if (width > 0) buf.fill(' ', width);
        if (len < precision && fchar == 'd' && negative &&
            !usePrefixForZero && hasMinusFlag) {
            buf.fill(' ', precision - len);
        }
        return buf.bytes();
    }


    private static byte[] getFixnumBytes(long arg, int base, boolean sign, boolean upper) {
        long val = arg;

        // limit the length of negatives if possible (also faster)
        if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
            if (sign) {
                return ConvertBytes.intToByteArray((int) val, base, upper);
            } else {
                switch (base) {
                    case 2:
                        return ConvertBytes.intToBinaryBytes((int) val);
                    case 8:
                        return ConvertBytes.intToOctalBytes((int) val);
                    case 10:
                    default:
                        return ConvertBytes.intToCharBytes((int) val);
                    case 16:
                        return ConvertBytes.intToHexBytes((int) val, upper);
                }
            }
        } else {
            if (sign) {
                return ConvertBytes.longToByteArray(val, base, upper);
            } else {
                switch (base) {
                    case 2:
                        return ConvertBytes.longToBinaryBytes(val);
                    case 8:
                        return ConvertBytes.longToOctalBytes(val);
                    case 10:
                    default:
                        return ConvertBytes.longToCharBytes(val);
                    case 16:
                        return ConvertBytes.longToHexBytes(val, upper);
                }
            }
        }
    }

    private static int skipSignBits(byte[] bytes, int base) {
        int skip = 0;
        int length = bytes.length;
        byte b;
        switch (base) {
            case 2:
                for (; skip < length && bytes[skip] == '1'; skip++) {
                }
                break;
            case 8:
                if (length > 0 && bytes[0] == '3') skip++;
                for (; skip < length && bytes[skip] == '7'; skip++) {
                }
                break;
            case 10:
                if (length > 0 && bytes[0] == '-') skip++;
                break;
            case 16:
                for (; skip < length && ((b = bytes[skip]) == 'f' || b == 'F'); skip++) {
                }
        }
        return skip;
    }

    private static byte[] getUnsignedNegativeBytes(long arg) {
        return ConvertBytes.longToCharBytes(((Long.MAX_VALUE + 1L) << 1) + arg);
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
