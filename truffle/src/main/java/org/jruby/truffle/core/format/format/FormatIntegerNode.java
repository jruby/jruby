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

    private static final BigInteger BIG_32 = BigInteger.valueOf(((long)Integer.MAX_VALUE + 1L) << 1);
    private static final BigInteger BIG_64 = BIG_32.shiftLeft(32);
    private static final BigInteger BIG_MINUS_32 = BigInteger.valueOf((long)Integer.MIN_VALUE << 1);
    private static final BigInteger BIG_MINUS_64 = BIG_MINUS_32.shiftLeft(32);

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

        final char fchar = this.getFormatCharacter();
        final boolean sign = this.getSign(fchar);
        final int base = getBase(fchar);
        final boolean zero = arg == 0;
        final boolean negative = arg < 0;

        final byte[] bytes;
        if (negative && fchar == 'u') {
            bytes = getUnsignedNegativeBytes(arg);
        } else {
            bytes = getFixnumBytes(arg, base, sign, fchar == 'X');
        }

        return formatBytes(width, precision, fchar, sign, base, zero, negative, bytes);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyBignum(value)")
    public byte[] format(int width, int precision, DynamicObject value) {
        final BigInteger bigInteger = Layouts.BIGNUM.getValue(value);
        final boolean negative = bigInteger.signum() < 0;
        final boolean zero = bigInteger.equals(BigInteger.ZERO);
        final char fchar = this.getFormatCharacter();
        final boolean sign = this.getSign(fchar);
        final int base = getBase(fchar);

        final byte[] bytes;
        if (negative && fchar == 'u') {
            bytes = getUnsignedNegativeBytes(bigInteger);
        } else {
            bytes = getBignumBytes(bigInteger, base, sign, fchar == 'X');
        }
        return formatBytes(width, precision, fchar, sign, base, zero, negative, bytes);
    }

    private byte[] formatBytes(int width, int precision, char fchar, boolean sign, int base, boolean zero, boolean negative, byte[] bytes) {
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

        int first = 0;
        byte[] prefix = null;

        byte signChar = 0;
        byte leadChar = 0;

        ByteList buf = new ByteList();

        if (hasFSharp) {
            if (!zero) {
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
                if (fchar != 'd' || !negative ||
                    hasPrecisionFlag ||
                    (hasZeroFlag && !hasMinusFlag)) {
                    buf.fill('0', precision - len);
                }
            } else if (leadChar == '.') {
                buf.fill(leadChar, precision - len);
                buf.append(PREFIX_NEGATIVE);
            } else {
                buf.append(PREFIX_NEGATIVE);
                buf.fill(leadChar, precision - len - 1);
            }
        } else if (leadChar != 0) {
            if ( "xXbBo".indexOf(fchar) != -1) {
                buf.append(PREFIX_NEGATIVE);
            }
            if (leadChar != '.') buf.append(leadChar);
        }
        buf.append(bytes, first, numlen);

        if (width > 0) buf.fill(' ', width);
        if (len < precision && fchar == 'd' && negative  && hasMinusFlag) {
            buf.fill(' ', precision - len);
        }
        return buf.bytes();
    }

    private boolean getSign(char fchar) {
        return (fchar == 'd' || (hasSpaceFlag || hasPlusFlag));
    }

    private static int getBase(char fchar) {
        final int base;
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
        return base;
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

    private char getFormatCharacter(){
        char fchar = this.format;

        // 'd' and 'i' are the same
        if (fchar == 'i') fchar = 'd';

        // 'u' with space or plus flags is same as 'd'
        if (fchar == 'u' && (hasSpaceFlag || hasPlusFlag)) {
            fchar = 'd';
        }
        return fchar;
    }




    private byte[] getUnsignedNegativeBytes(BigInteger bigval) {
        // calculation for negatives when %u specified
        // for values >= Integer.MIN_VALUE * 2, MRI uses (the equivalent of)
        //   long neg_u = (((long)Integer.MAX_VALUE + 1) << 1) + val
        // for smaller values, BigInteger math is required to conform to MRI's
        // result.

        // ok, now it gets expensive...
        int shift = 0;
        // go through negated powers of 32 until we find one small enough
        for (BigInteger minus = BIG_MINUS_64;
             bigval.compareTo(minus) < 0;
             minus = minus.shiftLeft(32), shift++) {
        }
        // add to the corresponding positive power of 32 for the result.
        // meaningful? no. conformant? yes. I just write the code...
        BigInteger nPower32 = shift > 0 ? BIG_64.shiftLeft(32 * shift) : BIG_64;
        return stringToBytes(nPower32.add(bigval).toString(), false);
    }

    private static byte[] getBignumBytes(BigInteger val, int base, boolean sign, boolean upper) {
        if (sign || base == 10 || val.signum() >= 0) {
            return stringToBytes(val.toString(base), upper);
        }

        // negative values
        byte[] bytes = val.toByteArray();
        switch (base) {
            case 2:
                return ConvertBytes.twosComplementToBinaryBytes(bytes);
            case 8:
                return ConvertBytes.twosComplementToOctalBytes(bytes);
            case 16:
                return ConvertBytes.twosComplementToHexBytes(bytes, upper);
            default:
                return stringToBytes(val.toString(base), upper);
        }
    }


    private static byte[] stringToBytes(CharSequence s, boolean upper) {
        int len = s.length();
        byte[] bytes = new byte[len];
        if (upper) {
            for (int i = len; --i >= 0; ) {
                int b = (byte) ((int) s.charAt(i) & 0xff);
                if (b >= 'a' && b <= 'z') {
                    bytes[i] = (byte) (b & ~0x20);
                } else {
                    bytes[i] = (byte) b;
                }
            }
        } else {
            for (int i = len; --i >= 0; ) {
                bytes[i] = (byte) ((int) s.charAt(i) & 0xff);
            }
        }
        return bytes;
    }

}
