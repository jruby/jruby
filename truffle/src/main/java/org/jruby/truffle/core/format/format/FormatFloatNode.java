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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.printf.PrintfSimpleTreeBuilder;
import org.jruby.util.ByteList;
import org.jruby.util.Sprintf;

import java.text.NumberFormat;
import java.util.Locale;

@NodeChildren({
    @NodeChild(value = "width", type = FormatNode.class),
    @NodeChild(value = "precision", type = FormatNode.class),
    @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatFloatNode extends FormatNode {

    private static final byte[] NAN_VALUE = {'N', 'a', 'N'};
    private static final byte[] INFINITY_VALUE = {'I', 'n', 'f'};

    private final char format;
    private final boolean hasSpaceFlag;
    private final boolean hasZeroFlag;
    private final boolean hasPlusFlag;
    private final boolean hasMinusFlag;
    private final boolean hasFSharpFlag;

    public FormatFloatNode(RubyContext context, char format, boolean hasSpaceFlag, boolean hasZeroFlag, boolean hasPlusFlag, boolean hasMinusFlag, boolean hasFSharpFlag) {
        super(context);
        this.format = format;
        this.hasSpaceFlag = hasSpaceFlag;
        this.hasZeroFlag = hasZeroFlag;
        this.hasPlusFlag = hasPlusFlag;
        this.hasMinusFlag = hasMinusFlag;
        this.hasFSharpFlag = hasFSharpFlag;
    }

    @TruffleBoundary
    @Specialization
    public byte[] formatInfinite(int width, int precision, double dval) {
//        if (arg == null || name != null) {
//            arg = args.next(name);
//            name = null;
//        }

//        if (!(arg instanceof RubyFloat)) {
//            // FIXME: what is correct 'recv' argument?
//            // (this does produce the desired behavior)
//            if (usePrefixForZero) {
//                arg = RubyKernel.new_float(arg,arg);
//            } else {
//                arg = RubyKernel.new_float19(arg,arg);
//            }
//        }
//        double dval = ((RubyFloat)arg).getDoubleValue();
        boolean hasPrecisionFlag = precision != PrintfSimpleTreeBuilder.DEFAULT;
        final char fchar = this.format;

        boolean nan = dval != dval;
        boolean inf = dval == Double.POSITIVE_INFINITY || dval == Double.NEGATIVE_INFINITY;
        boolean negative = dval < 0.0d || (dval == 0.0d && (new Float(dval)).equals(new Float(-0.0)));

        byte[] digits;
        int nDigits = 0;
        int exponent = 0;

        int len = 0;
        byte signChar;

        final ByteList buf = new ByteList();

        if (nan || inf) {
            if (nan) {
                digits = NAN_VALUE;
                len = NAN_VALUE.length;
            } else {
                digits = INFINITY_VALUE;
                len = INFINITY_VALUE.length;
            }
            if (negative) {
                signChar = '-';
                width--;
            } else if (hasPlusFlag) {
                signChar = '+';
                width--;
            } else if (hasSpaceFlag) {
                signChar = ' ';
                width--;
            } else {
                signChar = 0;
            }
            width -= len;

            if (width > 0 && !hasZeroFlag && !hasMinusFlag) {
                buf.fill(' ', width);
                width = 0;
            }
            if (signChar != 0) buf.append(signChar);

            if (width > 0 && !hasMinusFlag) {
                buf.fill('0', width);
                width = 0;
            }
            buf.append(digits);
            if (width > 0) buf.fill(' ', width);

//            offset++;
//            incomplete = false;
//            break;
            return buf.bytes();
        }

        final Locale locale = Locale.ENGLISH;
        NumberFormat nf = Sprintf.getNumberFormat(locale);
        nf.setMaximumFractionDigits(Integer.MAX_VALUE);
        String str = nf.format(dval);

        // grrr, arghh, want to subclass sun.misc.FloatingDecimal, but can't,
        // so we must do all this (the next 70 lines of code), which has already
        // been done by FloatingDecimal.
        int strlen = str.length();
        digits = new byte[strlen];
        int nTrailingZeroes = 0;
        int i = negative ? 1 : 0;
        int decPos = 0;
        byte ival;
        int_loop:
        for (; i < strlen; ) {
            switch (ival = (byte) str.charAt(i++)) {
                case '0':
                    if (nDigits > 0) nTrailingZeroes++;

                    break; // switch
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    for (; nTrailingZeroes > 0; nTrailingZeroes--) {
                        digits[nDigits++] = '0';
                    }
                    digits[nDigits++] = ival;
                    break; // switch
                case '.':
                    break int_loop;
            }
        }
        decPos = nDigits + nTrailingZeroes;
        dec_loop:
        for (; i < strlen; ) {
            switch (ival = (byte) str.charAt(i++)) {
                case '0':
                    if (nDigits > 0) {
                        nTrailingZeroes++;
                    } else {
                        exponent--;
                    }
                    break; // switch
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    for (; nTrailingZeroes > 0; nTrailingZeroes--) {
                        digits[nDigits++] = '0';
                    }
                    digits[nDigits++] = ival;
                    break; // switch
                case 'E':
                    break dec_loop;
            }
        }
        if (i < strlen) {
            int expSign;
            int expVal = 0;
            if (str.charAt(i) == '-') {
                expSign = -1;
                i++;
            } else {
                expSign = 1;
            }
            for (; i < strlen; ) {
                expVal = expVal * 10 + ((int) str.charAt(i++) - (int) '0');
            }
            exponent += expVal * expSign;
        }
        exponent += decPos - nDigits;

        // gotta have at least a zero...
        if (nDigits == 0) {
            digits[0] = '0';
            nDigits = 1;
            exponent = 0;
        }

        // OK, we now have the significand in digits[0...nDigits]
        // and the exponent in exponent.  We're ready to format.

        int intDigits, intZeroes, intLength;
        int decDigits, decZeroes, decLength;
        byte expChar;

        if (negative) {
            signChar = '-';
            width--;
        } else if (hasPlusFlag) {
            signChar = '+';
            width--;
        } else if (hasSpaceFlag) {
            signChar = ' ';
            width--;
        } else {
            signChar = 0;
        }
        if (!hasPrecisionFlag) {
            precision = 6;
        }

        switch (fchar) {
            case 'E':
            case 'G':
                expChar = 'E';
                break;
            case 'e':
            case 'g':
                expChar = 'e';
                break;
            default:
                expChar = 0;
        }

        final byte decimalSeparator = (byte) Sprintf.getDecimalFormat(locale).getDecimalSeparator();

        switch (fchar) {
            case 'g':
            case 'G':
                // an empirically derived rule: precision applies to
                // significand length, irrespective of exponent

                // an official rule, clarified: if the exponent
                // <clarif>after adjusting for exponent form</clarif>
                // is < -4,  or the exponent <clarif>after adjusting
                // for exponent form</clarif> is greater than the
                // precision, use exponent form
                boolean expForm = (exponent + nDigits - 1 < -4 ||
                    exponent + nDigits > (precision == 0 ? 1 : precision));
                // it would be nice (and logical!) if exponent form
                // behaved like E/e, and decimal form behaved like f,
                // but no such luck. hence:
                if (expForm) {
                    // intDigits isn't used here, but if it were, it would be 1
                            /* intDigits = 1; */
                    decDigits = nDigits - 1;
                    // precision for G/g includes integer digits
                    precision = Math.max(0, precision - 1);

                    if (precision < decDigits) {
                        int n = round(digits, nDigits, precision, precision != 0);
                        if (n > nDigits) nDigits = n;
                        decDigits = Math.min(nDigits - 1, precision);
                    }
                    exponent += nDigits - 1;

                    boolean isSharp = hasFSharpFlag;

                    // deal with length/width

                    len++; // first digit is always printed

                    // MRI behavior: Be default, 2 digits
                    // in the exponent. Use 3 digits
                    // only when necessary.
                    // See comment for writeExp method for more details.
                    if (exponent > 99) {
                        len += 5; // 5 -> e+nnn / e-nnn
                    } else {
                        len += 4; // 4 -> e+nn / e-nn
                    }

                    if (isSharp) {
                        // in this mode, '.' is always printed
                        len++;
                    }

                    if (precision > 0) {
                        if (!isSharp) {
                            // MRI behavior: In this mode
                            // trailing zeroes are removed:
                            // 1.500E+05 -> 1.5E+05
                            int j = decDigits;
                            for (; j >= 1; j--) {
                                if (digits[j] == '0') {
                                    decDigits--;
                                } else {
                                    break;
                                }
                            }

                            if (decDigits > 0) {
                                len += 1; // '.' is printed
                                len += decDigits;
                            }
                        } else {
                            // all precision numebers printed
                            len += precision;
                        }
                    }

                    width -= len;

                    if (width > 0 && !hasZeroFlag && !hasMinusFlag) {
                        buf.fill(' ', width);
                        width = 0;
                    }
                    if (signChar != 0) {
                        buf.append(signChar);
                    }
                    if (width > 0 && !hasMinusFlag) {
                        buf.fill('0', width);
                        width = 0;
                    }

                    // now some data...
                    buf.append(digits[0]);

                    boolean dotToPrint = isSharp
                        || (precision > 0 && decDigits > 0);

                    if (dotToPrint) {
                        buf.append(decimalSeparator); // '.' // args.getDecimalSeparator()
                    }

                    if (precision > 0 && decDigits > 0) {
                        buf.append(digits, 1, decDigits);
                        precision -= decDigits;
                    }

                    if (precision > 0 && isSharp) {
                        buf.fill('0', precision);
                    }

                    writeExp(buf, exponent, expChar);

                    if (width > 0) {
                        buf.fill(' ', width);
                    }
                } else { // decimal form, like (but not *just* like!) 'f'
                    intDigits = Math.max(0, Math.min(nDigits + exponent, nDigits));
                    intZeroes = Math.max(0, exponent);
                    intLength = intDigits + intZeroes;
                    decDigits = nDigits - intDigits;
                    decZeroes = Math.max(0, -(decDigits + exponent));
                    decLength = decZeroes + decDigits;
                    precision = Math.max(0, precision - intLength);

                    if (precision < decDigits) {
                        int n = round(digits, nDigits, intDigits + precision - 1, precision != 0);
                        if (n > nDigits) {
                            // digits array shifted, update all
                            nDigits = n;
                            intDigits = Math.max(0, Math.min(nDigits + exponent, nDigits));
                            intLength = intDigits + intZeroes;
                            decDigits = nDigits - intDigits;
                            decZeroes = Math.max(0, -(decDigits + exponent));
                            precision = Math.max(0, precision - 1);
                        }
                        decDigits = precision;
                        decLength = decZeroes + decDigits;
                    }
                    len += intLength;
                    if (decLength > 0) {
                        len += decLength + 1;
                    } else {
                        if (hasFSharpFlag) {
                            len++; // will have a trailing '.'
                            if (precision > 0) { // g fills trailing zeroes if #
                                len += precision;
                            }
                        }
                    }

                    width -= len;

                    if (width > 0 && !hasZeroFlag && !hasMinusFlag) {
                        buf.fill(' ', width);
                        width = 0;
                    }
                    if (signChar != 0) {
                        buf.append(signChar);
                    }
                    if (width > 0 && !hasMinusFlag) {
                        buf.fill('0', width);
                        width = 0;
                    }
                    // now some data...
                    if (intLength > 0) {
                        if (intDigits > 0) { // s/b true, since intLength > 0
                            buf.append(digits, 0, intDigits);
                        }
                        if (intZeroes > 0) {
                            buf.fill('0', intZeroes);
                        }
                    } else {
                        // always need at least a 0
                        buf.append('0');
                    }
                    if (decLength > 0 || hasFSharpFlag) {
                        buf.append(decimalSeparator);
                    }
                    if (decLength > 0) {
                        if (decZeroes > 0) {
                            buf.fill('0', decZeroes);
                            precision -= decZeroes;
                        }
                        if (decDigits > 0) {
                            buf.append(digits, intDigits, decDigits);
                            precision -= decDigits;
                        }
                        if (hasFSharpFlag && precision > 0) {
                            buf.fill('0', precision);
                        }
                    }
                    if (hasFSharpFlag && precision > 0) buf.fill('0', precision);
                    if (width > 0) buf.fill(' ', width);
                }
                break;

            case 'f':
                intDigits = Math.max(0, Math.min(nDigits + exponent, nDigits));
                intZeroes = Math.max(0, exponent);
                intLength = intDigits + intZeroes;
                decDigits = nDigits - intDigits;
                decZeroes = Math.max(0, -(decDigits + exponent));
                decLength = decZeroes + decDigits;

                if (precision < decLength) {
                    if (precision < decZeroes) {
                        decDigits = 0;
                        decZeroes = precision;
                    } else {
                        int n = round(digits, nDigits, intDigits + precision - decZeroes - 1, false);
                        if (n > nDigits) {
                            // digits arr shifted, update all
                            nDigits = n;
                            intDigits = Math.max(0, Math.min(nDigits + exponent, nDigits));
                            intLength = intDigits + intZeroes;
                            decDigits = nDigits - intDigits;
                            decZeroes = Math.max(0, -(decDigits + exponent));
                            decLength = decZeroes + decDigits;
                        }
                        decDigits = precision - decZeroes;
                    }
                    decLength = decZeroes + decDigits;
                }
                if (precision > 0) {
                    len += Math.max(1, intLength) + 1 + precision;
                    // (1|intlen).prec
                } else {
                    len += Math.max(1, intLength);
                    // (1|intlen)
                    if (hasFSharpFlag) {
                        len++; // will have a trailing '.'
                    }
                }

                width -= len;

                if (width > 0 && !hasZeroFlag && !hasMinusFlag) {
                    buf.fill(' ', width);
                    width = 0;
                }
                if (signChar != 0) {
                    buf.append(signChar);
                }
                if (width > 0 && !hasMinusFlag) {
                    buf.fill('0', width);
                    width = 0;
                }
                // now some data...
                if (intLength > 0) {
                    if (intDigits > 0) { // s/b true, since intLength > 0
                        buf.append(digits, 0, intDigits);
                    }
                    if (intZeroes > 0) {
                        buf.fill('0', intZeroes);
                    }
                } else {
                    // always need at least a 0
                    buf.append('0');
                }
                if (precision > 0 || hasFSharpFlag) {
                    buf.append(decimalSeparator);
                }
                if (precision > 0) {
                    if (decZeroes > 0) {
                        buf.fill('0', decZeroes);
                        precision -= decZeroes;
                    }
                    if (decDigits > 0) {
                        buf.append(digits, intDigits, decDigits);
                        precision -= decDigits;
                    }
                    // fill up the rest with zeroes
                    if (precision > 0) {
                        buf.fill('0', precision);
                    }
                }
                if (width > 0) {
                    buf.fill(' ', width);
                }
                break;
            case 'E':
            case 'e':
                // intDigits isn't used here, but if it were, it would be 1
                        /* intDigits = 1; */
                decDigits = nDigits - 1;

                if (precision < decDigits) {
                    int n = round(digits, nDigits, precision, precision != 0);
                    if (n > nDigits) {
                        nDigits = n;
                    }
                    decDigits = Math.min(nDigits - 1, precision);
                }
                exponent += nDigits - 1;

                boolean isSharp = hasFSharpFlag;

                // deal with length/width

                len++; // first digit is always printed

                // MRI behavior: Be default, 2 digits
                // in the exponent. Use 3 digits
                // only when necessary.
                // See comment for writeExp method for more details.
                if (exponent > 99) {
                    len += 5; // 5 -> e+nnn / e-nnn
                } else {
                    len += 4; // 4 -> e+nn / e-nn
                }

                if (precision > 0) {
                    // '.' and all precision digits printed
                    len += 1 + precision;
                } else if (isSharp) {
                    len++;  // in this mode, '.' is always printed
                }

                width -= len;

                if (width > 0 && !hasZeroFlag && !hasMinusFlag) {
                    buf.fill(' ', width);
                    width = 0;
                }
                if (signChar != 0) {
                    buf.append(signChar);
                }
                if (width > 0 && !hasMinusFlag) {
                    buf.fill('0', width);
                    width = 0;
                }
                // now some data...
                buf.append(digits[0]);
                if (precision > 0) {
                    buf.append(decimalSeparator); // '.'
                    if (decDigits > 0) {
                        buf.append(digits, 1, decDigits);
                        precision -= decDigits;
                    }
                    if (precision > 0) buf.fill('0', precision);

                } else if (hasFSharpFlag) {
                    buf.append(decimalSeparator);
                }

                writeExp(buf, exponent, expChar);

                if (width > 0) buf.fill(' ', width);

        }
        return buf.bytes();
    }

    private static int round(byte[] bytes, int nDigits, int roundPos, boolean roundDown) {
        int next = roundPos + 1;
        if (next >= nDigits || bytes[next] < '5' ||
            // MRI rounds up on nnn5nnn, but not nnn5 --
            // except for when they do
            (roundDown && bytes[next] == '5' && next == nDigits - 1)) {
            return nDigits;
        }
        if (roundPos < 0) { // "%.0f" % 0.99
            System.arraycopy(bytes, 0, bytes, 1, nDigits);
            bytes[0] = '1';
            return nDigits + 1;
        }
        bytes[roundPos] += 1;
        while (bytes[roundPos] > '9') {
            bytes[roundPos] = '0';
            roundPos--;
            if (roundPos >= 0) {
                bytes[roundPos] += 1;
            } else {
                System.arraycopy(bytes, 0, bytes, 1, nDigits);
                bytes[0] = '1';
                return nDigits + 1;
            }
        }
        return nDigits;
    }

    private static void writeExp(ByteList buf, int exponent, byte expChar) {
        // Unfortunately, the number of digits in the exponent is
        // not clearly defined in Ruby documentation. This is a
        // platform/version-dependent behavior. On Linux/Mac/Cygwin/*nix,
        // two digits are used. On Windows, 3 digits are used.
        // It is desirable for JRuby to have consistent behavior, and
        // the two digits behavior was selected. This is also in sync
        // with "Java-native" sprintf behavior (java.util.Formatter).
        buf.append(expChar); // E or e
        buf.append(exponent >= 0 ? '+' : '-');
        if (exponent < 0) {
            exponent = -exponent;
        }
        if (exponent > 99) {
            buf.append(exponent / 100 + '0');
            buf.append(exponent % 100 / 10 + '0');
        } else {
            buf.append(exponent / 10 + '0');
        }
        buf.append(exponent % 10 + '0');
    }

}
