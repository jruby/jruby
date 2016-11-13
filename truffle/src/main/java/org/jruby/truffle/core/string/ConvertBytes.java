/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * This is a port of org.jruby.util.ConvertBytes to work with the JRuby+Truffle backend.
 * The original class is licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */

package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.util.ByteList;

import java.math.BigInteger;
import java.util.Arrays;

public class ConvertBytes {
    private final RubyContext context;
    private final Node node;
    private final FixnumOrBignumNode fixnumOrBignumNode;
    private final DynamicObject _str;
    private int str;
    private int end;
    private byte[] data;
    private int base;
    private final boolean badcheck;

    public ConvertBytes(RubyContext context, Node node, FixnumOrBignumNode fixnumOrBignumNode, DynamicObject _str, int base, boolean badcheck) {
        final Rope rope = StringOperations.rope(_str);

        this.context = context;
        this.node = node;
        this.fixnumOrBignumNode = fixnumOrBignumNode;
        this._str = _str;
        this.str = 0;
        this.data = rope.getBytes();
        this.end = str + rope.byteLength();
        this.badcheck = badcheck;
        this.base = base;
    }

    private static final byte[][] MIN_VALUE_BYTES;
    static {
        MIN_VALUE_BYTES = new byte[37][];
        for (int i = 2; i <= 36; i++) {
            MIN_VALUE_BYTES[i] =  ByteList.plain(Long.toString(Long.MIN_VALUE, i));
        }
    }

    /** rb_cstr_to_inum
     *
     */

    public static Object byteListToInum19(RubyContext context, Node node, FixnumOrBignumNode fixnumOrBignumNode, DynamicObject str, int base, boolean badcheck) {
        return new ConvertBytes(context, node, fixnumOrBignumNode, str, base, badcheck).byteListToInum();
    }

    private final static byte[] conv_digit = new byte[128];
    private final static boolean[] digit = new boolean[128];
    private final static boolean[] space = new boolean[128];
    private final static boolean[] spaceOrUnderscore = new boolean[128];

    static {
        Arrays.fill(conv_digit, (byte)-1);
        Arrays.fill(digit, false);
        for(char c = '0'; c <= '9'; c++) {
            conv_digit[c] = (byte)(c - '0');
            digit[c] = true;
        }

        for(char c = 'a'; c <= 'z'; c++) {
            conv_digit[c] = (byte)(c - 'a' + 10);
        }

        for(char c = 'A'; c <= 'Z'; c++) {
            conv_digit[c] = (byte)(c - 'A' + 10);
        }

        Arrays.fill(space, false);
        space['\t'] = true;
        space['\n'] = true;
        space[11] = true; // \v
        space['\f'] = true;
        space['\r'] = true;
        space[' '] = true;

        Arrays.fill(spaceOrUnderscore, false);
        spaceOrUnderscore['\t'] = true;
        spaceOrUnderscore['\n'] = true;
        spaceOrUnderscore[11] = true; // \v
        spaceOrUnderscore['\f'] = true;
        spaceOrUnderscore['\r'] = true;
        spaceOrUnderscore[' '] = true;
        spaceOrUnderscore['_'] = true;
    }

    /** conv_digit
     *
     */
    private byte convertDigit(byte c) {
        if(c < 0) {
            return -1;
        }
        return conv_digit[c];
    }

    /** ISSPACE
     *
     */
    private boolean isSpace(int str) {
        byte c;
        if(str == end || (c = data[str]) < 0) {
            return false;
        }
        return space[c];
    }

    /** ISSPACE || *str == '_'
     *
     */
    private boolean isSpaceOrUnderscore(int str) {
        byte c;
        if(str == end || (c = data[str]) < 0) {
            return false;
        }
        return spaceOrUnderscore[c];
    }

    private boolean getSign() {
        boolean sign = true;
        if(str < end) {
            if(data[str] == '+') {
                str++;
            } else if(data[str] == '-') {
                str++;
                sign = false;
            }
        }

        return sign;
    }

    private void ignoreLeadingWhitespace() {
        while(isSpace(str)) {
            str++;
        }
    }

    private void figureOutBase() {
        if(base <= 0) {
            if(str < end && data[str] == '0') {
                if(str + 1 < end) {
                    switch(data[str+1]) {
                        case 'x':
                        case 'X':
                            base = 16;
                            break;
                        case 'b':
                        case 'B':
                            base = 2;
                            break;
                        case 'o':
                        case 'O':
                            base = 8;
                            break;
                        case 'd':
                        case 'D':
                            base = 10;
                            break;
                        default:
                            base = 8;
                    }
                } else {
                    base = 8;
                }
            } else if(base < -1) {
                base = -base;
            } else {
                base = 10;
            }
        }
    }

    @SuppressWarnings("fallthrough")
    private int calculateLength() {
        int len = 0;
        byte second = ((str+1 < end) && data[str] == '0') ? data[str+1] : (byte)0;

        switch(base) {
            case 2:
                len = 1;
                if(second == 'b' || second == 'B') {
                    str+=2;
                }
                break;
            case 3:
                len = 2;
                break;
            case 8:
                if(second == 'o' || second == 'O') {
                    str+=2;
                }
            case 4: case 5: case 6: case 7:
                len = 3;
                break;
            case 10:
                if(second == 'd' || second == 'D') {
                    str+=2;
                }
            case 9: case 11: case 12:
            case 13: case 14: case 15:
                len = 4;
                break;
            case 16:
                len = 4;
                if(second == 'x' || second == 'X') {
                    str+=2;
                }
                break;
            default:
                if(base < 2 || 36 < base) {
                    throw new RaiseException(
                            context.getCoreExceptions().argumentErrorInvalidRadix(base, node));
                }
                if(base <= 32) {
                    len = 5;
                } else {
                    len = 6;
                }
                break;
        }

        return len;
    }

    private void squeezeZeroes() {
        byte c;
        if(str < end && data[str] == '0') {
            str++;
            int us = 0;
            while((str < end) && ((c = data[str]) == '0' || c == '_')) {
                if(c == '_') {
                    if(++us >= 2) {
                        break;
                    }
                } else {
                    us += 0;
                }
                str++;
            }
            if(str == end || isSpace(str)) {
                str--;
            }
        }
    }

    private long stringToLong(int nptr, int[] endptr, int base) {
        if(base < 0 || base == 1 || base > 36) {
            return 0;
        }
        int save = nptr;
        int s = nptr;
        boolean overflow = false;

        while(isSpace(s)) {
            s++;
        }

        if(s != end) {
            boolean negative = false;
            if(data[s] == '-') {
                negative = true;
                s++;
            } else if(data[s] == '+') {
                negative = false;
                s++;
            }

            save = s;
            byte c;
            long i = 0;

            final long cutoff = Long.MAX_VALUE / base;
            final long cutlim = Long.MAX_VALUE % base;

            while(s < end) {
                c = convertDigit(data[s]);

                if(c == -1 || c >= base) {
                    break;
                }
                s++;

                if(i > cutoff || (i == cutoff && c > cutlim)) {
                    overflow = true;
                } else {
                    i *= base;
                    i += c;
                }
            }

            if(s != save) {
                if(endptr != null) {
                    endptr[0] = s;
                }

                if(overflow) {
                    throw new ConvertBytes.ERange(negative ? ConvertBytes.ERange.Kind.Underflow : ConvertBytes.ERange.Kind.Overflow);
                }

                if(negative) {
                    return -i;
                } else {
                    return i;
                }
            }
        }

        if(endptr != null) {
            if(save - nptr >= 2 && (data[save-1] == 'x' || data[save-1] == 'X') && data[save-2] == '0') {
                endptr[0] = save-1;
            } else {
                endptr[0] = nptr;
            }
        }
        return 0;
    }

    @TruffleBoundary
    public Object byteListToInum() {
        if(_str == null) {
            if(badcheck) {
                invalidString("Integer");
            }
            return 0;
        }

        ignoreLeadingWhitespace();

        boolean sign = getSign();

        if(str < end) {
            if(data[str] == '+' || data[str] == '-') {
                if(badcheck) {
                    invalidString("Integer");
                }
                return 0;
            }
        }

        figureOutBase();

        int len = calculateLength();

        squeezeZeroes();

        byte c = 0;
        if(str < end) {
            c = data[str];
        }
        c = convertDigit(c);
        if(c < 0 || c >= base) {
            if(badcheck) {
                invalidString("Integer");
            }
            return 0;
        }

        if (base <= 10) {
            len *= (trailingLength());
        } else {
            len *= (end-str);
        }

        if(len < Long.SIZE-1) {
            int[] endPlace = new int[]{str};
            long val = stringToLong(str, endPlace, base);

            if(endPlace[0] < end && data[endPlace[0]] == '_') {
                return bigParse(len, sign);
            }
            if(badcheck) {
                if(endPlace[0] == str) {
                    invalidString("Integer"); // no number
                }

                while(isSpace(endPlace[0])) {
                    endPlace[0]++;
                }

                if(endPlace[0] < end) {
                    invalidString("Integer"); // trailing garbage
                }
            }

            if(sign) {
                if (CoreLibrary.fitsIntoInteger(val)) {
                    return (int) val;
                } else {
                    return val;
                }
            } else {
                if (CoreLibrary.fitsIntoInteger(-val)) {
                    return (int) -val;
                } else {
                    return -val;
                }
            }
        }
        return bigParse(len, sign);
    }

    private int trailingLength() {
        int newLen = 0;
        for (int i=str; i < end; i++) {
            if (Character.isDigit(data[i])) newLen++;
            else return newLen;
        }
        return newLen;
    }

    private Object bigParse(int len, boolean sign) {
        if(badcheck && str < end && data[str] == '_') {
            invalidString("Integer");
        }

        char[] result = new char[end-str];
        int resultIndex = 0;

        byte nondigit = -1;

        // str2big_scan_digits
        {
            while(str < end) {
                byte c = data[str++];
                byte cx = c;
                if(c == '_') {
                    if(nondigit != -1) {
                        if(badcheck) {
                            invalidString("Integer");
                        }
                        break;
                    }
                    nondigit = c;
                    continue;
                } else if((c = convertDigit(c)) < 0) {
                    break;
                }
                if(c >= base) {
                    break;
                }
                nondigit = -1;
                result[resultIndex++] = (char)cx;
            }

            if(resultIndex == 0) { return 0; }

            int tmpStr = str;
            if (badcheck) {
                // no str-- here because we don't null-terminate strings
                if (1 < tmpStr && data[tmpStr-1] == '_') invalidString("Integer");
                while (tmpStr < end && Character.isWhitespace(data[tmpStr])) tmpStr++;
                if (tmpStr < end) {
                    invalidString("Integer");
                }

            }
        }

        String s = new String(result, 0, resultIndex);
        BigInteger z = (base == 10) ? stringToBig(s) : new BigInteger(s, base);
        if(!sign) { z = z.negate(); }

        if(badcheck) {
            if(1 < str && data[str-1] == '_') {
                invalidString("Integer");
            }
            while(str < end && isSpace(str)) {
                str++;
            }
            if(str < end) {
                invalidString("Integer");
            }
        }

        return fixnumOrBignumNode.fixnumOrBignum(z);
    }

    private BigInteger stringToBig(String str) {
        str = str.replaceAll("_", "");
        int size = str.length();
        int nDigits = 512;
        if (size < nDigits) { nDigits = size; }

        int j = size - 1;
        int i = j - nDigits + 1;

        BigInteger digits[] = new BigInteger[j / nDigits + 1];

        for(int z = 0; j >= 0; z++) {
            digits[z] = new BigInteger(str.substring(i, j + 1).trim());
            j = i - 1;
            i = j - nDigits + 1;
            if(i < 0) { i = 0; }
        }

        BigInteger b10x = BigInteger.TEN.pow(nDigits);
        int n = digits.length;
        while(n > 1) {
            i = 0;
            j = 0;
            while(i < n / 2) {
                digits[i] = digits[j].add(digits[j + 1].multiply(b10x));
                i += 1;
                j += 2;
            }
            if(j == n-1) {
                digits[i] = digits[j];
                i += 1;
            }
            n = i;
            b10x = b10x.multiply(b10x);
        }

        return digits[0];
    }

    public static class ERange extends RuntimeException {
        private static final long serialVersionUID = 3393153027217708024L;

        public static enum Kind {
            Overflow,
            Underflow
        }
        private ConvertBytes.ERange.Kind kind;
        public ERange() {
            super();
        }
        public ERange(ConvertBytes.ERange.Kind kind) {
            super();
            this.kind = kind;
        }
        public ConvertBytes.ERange.Kind getKind() {
            return kind;
        }
    }

    /** rb_invalid_str
     *
     */
    @TruffleBoundary
    private void invalidString(String type) {
        throw new RaiseException(
                context.getCoreExceptions().argumentErrorInvalidValue(_str, type, node));
    }
}
