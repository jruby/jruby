/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.truffle.util;

import org.jruby.truffle.util.ByteList;

import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * @author Bill Dortch
 *
 */
public class Sprintf {
    private static final int FLAG_NONE        = 0;
    private static final int FLAG_SPACE       = 1;
    private static final int FLAG_ZERO        = 1 << 1;
    private static final int FLAG_PLUS        = 1 << 2;
    private static final int FLAG_MINUS       = 1 << 3;
    private static final int FLAG_SHARP       = 1 << 4;
    private static final int FLAG_WIDTH       = 1 << 5;
    private static final int FLAG_PRECISION   = 1 << 6;

    private static final byte[] PREFIX_OCTAL     = {'0'};
    private static final byte[] PREFIX_HEX_LC    = {'0','x'};
    private static final byte[] PREFIX_HEX_UC    = {'0','X'};
    private static final byte[] PREFIX_BINARY_LC = {'0','b'};
    private static final byte[] PREFIX_BINARY_UC = {'0','B'};

    private static final byte[] PREFIX_NEGATIVE = {'.','.'};

    private static final byte[] NAN_VALUE       = {'N','a','N'};
    private static final byte[] INFINITY_VALUE  = {'I','n','f'};

    private static final BigInteger BIG_32 = BigInteger.valueOf(((long)Integer.MAX_VALUE + 1L) << 1);
    private static final BigInteger BIG_64 = BIG_32.shiftLeft(32);
    private static final BigInteger BIG_MINUS_32 = BigInteger.valueOf((long)Integer.MIN_VALUE << 1);
    private static final BigInteger BIG_MINUS_64 = BIG_MINUS_32.shiftLeft(32);

    private static final String ERR_MALFORMED_FORMAT = "malformed format string";
    private static final String ERR_MALFORMED_NUM = "malformed format string - %[0-9]";
    private static final String ERR_MALFORMED_DOT_NUM = "malformed format string - %.[0-9]";
    private static final String ERR_MALFORMED_STAR_NUM = "malformed format string - %*[0-9]";
    private static final String ERR_ILLEGAL_FORMAT_CHAR = "illegal format character - %";
    private static final String ERR_MALFORMED_NAME = "malformed name - unmatched parenthesis";

    private static final ThreadLocal<Map<Locale, NumberFormat>> LOCALE_NUMBER_FORMATS = new ThreadLocal<Map<Locale, NumberFormat>>();
    private static final ThreadLocal<Map<Locale, DecimalFormatSymbols>> LOCALE_DECIMAL_FORMATS = new ThreadLocal<Map<Locale, DecimalFormatSymbols>>();

    // static methods only
    private Sprintf () {}

    public static NumberFormat getNumberFormat(Locale locale) {
        Map<Locale, NumberFormat> numberFormats = LOCALE_NUMBER_FORMATS.get();
        if (numberFormats == null) {
            numberFormats = new HashMap<Locale, NumberFormat>(4);
            LOCALE_NUMBER_FORMATS.set(numberFormats);
        }
        NumberFormat format = numberFormats.get(locale);
        if ( format == null ) {
            format = NumberFormat.getNumberInstance(locale);
            numberFormats.put(locale, format);
        }
        return format;
    }

    public static DecimalFormatSymbols getDecimalFormat(Locale locale) {
        Map<Locale, DecimalFormatSymbols> decimalFormats = LOCALE_DECIMAL_FORMATS.get();
        if (decimalFormats == null) {
            decimalFormats = new HashMap<Locale, DecimalFormatSymbols>(4);
            LOCALE_DECIMAL_FORMATS.set(decimalFormats);
        }
        DecimalFormatSymbols format = decimalFormats.get(locale);
        if (format == null) {
            format = new DecimalFormatSymbols(locale);
            decimalFormats.put(locale, format);
        }
        return format;
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

    // debugging code, keeping for now
    /*
    private static final void showLiteral(byte[] format, int start, int offset) {
        System.out.println("literal: ["+ new String(format,start,offset-start)+ "], " +
                " s="+ start + " o="+ offset);
    }

    // debugging code, keeping for now
    private static final void showVals(byte[] format,int start,int offset, byte fchar,
            int flags, int width, int precision, Object arg) {
        System.out.println(new StringBuffer()
        .append("value: ").append(new String(format,start,offset-start+1)).append('\n')
        .append("type: ").append((char)fchar).append('\n')
        .append("start: ").append(start).append('\n')
        .append("length: ").append(offset-start).append('\n')
        .append("flags: ").append(Integer.toBinaryString(flags)).append('\n')
        .append("width: ").append(width).append('\n')
        .append("precision: ").append(precision).append('\n')
        .append("arg: ").append(arg).append('\n')
        .toString());

    }
    */

    private static boolean isDigit(byte aChar) {
        return (aChar >= '0' && aChar <= '9');
    }

    private static boolean isPrintable(byte aChar) {
        return (aChar > 32 && aChar < 127);
    }

    private static int skipSignBits(byte[] bytes, int base) {
        int skip = 0;
        int length = bytes.length;
        byte b;
        switch(base) {
        case 2:
            for ( ; skip < length && bytes[skip] == '1'; skip++ ) {}
            break;
        case 8:
            if (length > 0 && bytes[0] == '3') skip++;
            for ( ; skip < length && bytes[skip] == '7'; skip++ ) {}
            break;
        case 10:
            if (length > 0 && bytes[0] == '-') skip++;
            break;
        case 16:
            for ( ; skip < length && ((b = bytes[skip]) == 'f' || b == 'F'); skip++ ) {}
        }
        return skip;
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
            System.arraycopy(bytes,0,bytes,1,nDigits);
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
                System.arraycopy(bytes,0,bytes,1,nDigits);
                bytes[0] = '1';
                return nDigits + 1;
            }
        }
        return nDigits;
    }

    private static byte[] stringToBytes(CharSequence s, boolean upper) {
        int len = s.length();
        byte[] bytes = new byte[len];
        if (upper) {
            for (int i = len; --i >= 0; ) {
                int b = (byte)((int)s.charAt(i) & 0xff);
                if (b >= 'a' && b <= 'z') {
                    bytes[i] = (byte)(b & ~0x20);
                } else {
                    bytes[i] = (byte)b;
                }
            }
        } else {
            for (int i = len; --i >= 0; ) {
                bytes[i] = (byte)((int)s.charAt(i) & 0xff);
            }
        }
        return bytes;
    }
}
