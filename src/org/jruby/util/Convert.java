/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 William N. Dortch <bill.dortch@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;
import java.math.BigInteger;

import org.jruby.RubyNumeric.InvalidIntegerException;
import org.jruby.RubyNumeric.NumberTooLargeException;

/**
 * @author Bill Dortch
 * 
 * Primitive conversions adapted from java.lang.Integer/Long/Double (C) Sun Microsystems, Inc.
 *
 */
public class Convert {

    /**
     * Returns a <code>ByteList</code> object representing the
     * specified integer. The argument is converted to signed decimal
     * representation and returned as a ByteList.
     *
     * @param   i   an integer to be converted.
     * @return  a ByteList representation of the argument in base&nbsp;10.
     */
    public static final ByteList intToByteList(int i) {
        if (i == Integer.MIN_VALUE)
            return new ByteList((byte[])MIN_INT_BYTE_ARRAY.clone(),false);
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        byte[] buf = new byte[size];
        getCharBytes(i, size, buf);
        return new ByteList(buf,false);
    }

    public static final byte[] intToByteArray(int i) {
        if (i == Integer.MIN_VALUE)
            return (byte[])MIN_INT_BYTE_ARRAY.clone();
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        byte[] buf = new byte[size];
        getCharBytes(i, size, buf);
        return buf;
    }

    /**
     * Returns a <code>ByteList</code> object representing the
     * specified integer, using the specified radix. The argument is 
     * converted to signed decimal representation and returned as a ByteList.
     *
     * @param   i   an integer to be converted.
     * @param   radix   the radix to use in the ByteList representation.
     * @return  a ByteList representation of the argument in the specified radix.
     */
    public static final ByteList intToByteList(int i, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;
        if (radix == 10)
            return intToByteList(i); // much faster for base 10
        byte buf[] = new byte[33];
        boolean negative = (i < 0);
        int charPos = 32;
        if (!negative) {
            i = -i;
        }
        while (i <= -radix) {
            buf[charPos--] = DIGITS[-(i % radix)];
            i = i / radix;
        }
        buf[charPos] = DIGITS[-i];
        if (negative) {
            buf[--charPos] = '-';
        }
        return new ByteList(buf, charPos, (33 - charPos));
    }

    public static final byte[] intToByteArray(int i, int radix, boolean upper) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;
        if (radix == 10)
            return intToByteArray(i); // much faster for base 10
        byte buf[] = new byte[33];
        byte[] digits = upper ? UCDIGITS : DIGITS;
        boolean negative = (i < 0);
        int charPos = 32;
        if (!negative) {
            i = -i;
        }
        while (i <= -radix) {
            buf[charPos--] = digits[-(i % radix)];
            i = i / radix;
        }
        buf[charPos] = digits[-i];
        if (negative) {
            buf[--charPos] = '-';
        }
        int len = 33 - charPos;
        byte[] out = new byte[len];
        System.arraycopy(buf,charPos,out,0,len);
        return out;
    }

    /**
     * Returns a <code>ByteList</code> object representing the specified
     * <code>long</code>.  The argument is converted to signed decimal
     * representation and returned as a ByteList.
     *
     * @param   i   a <code>long</code> to be converted.
     * @return  a ByteList representation of the argument in base&nbsp;10.
     */
    public static final ByteList longToByteList(long i) {
        if (i == Long.MIN_VALUE)
            return new ByteList((byte[])MIN_LONG_BYTE_ARRAY.clone(),false);
        // int version is slightly faster, use if possible
        if (i <= Integer.MAX_VALUE && i >= Integer.MIN_VALUE)
            return intToByteList((int)i);
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        byte[] buf = new byte[size];
        getCharBytes(i, size, buf);
        return new ByteList(buf,false);
    }
   
    public static final byte[] longToByteArray(long i) {
        if (i == Long.MIN_VALUE)
            return (byte[])MIN_LONG_BYTE_ARRAY.clone();
        // int version is slightly faster, use if possible
        if (i <= Integer.MAX_VALUE && i >= Integer.MIN_VALUE)
            return intToByteArray((int)i);
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        byte[] buf = new byte[size];
        getCharBytes(i, size, buf);
        return buf;
    }
   
    public static final ByteList longToByteList(long i, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;
        if (radix == 10)
            return longToByteList(i); // much faster for base 10
        byte[] buf = new byte[65];
        int charPos = 64;
        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }
        while (i <= -radix) {
            buf[charPos--] = DIGITS[(int)(-(i % radix))];
            i = i / radix;
        }
        buf[charPos] = DIGITS[(int)(-i)];
        if (negative) { 
            buf[--charPos] = '-';
        }
        return new ByteList(buf, charPos, (65 - charPos));
    }

    public static final byte[] longToByteArray(long i, int radix, boolean upper) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;
        if (radix == 10)
            return longToByteArray(i); // much faster for base 10
        byte[] buf = new byte[65];
        byte[] digits = upper ? UCDIGITS : DIGITS;
        int charPos = 64;
        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }
        while (i <= -radix) {
            buf[charPos--] = digits[(int)(-(i % radix))];
            i = i / radix;
        }
        buf[charPos] = digits[(int)(-i)];
        if (negative) { 
            buf[--charPos] = '-';
        }
        int len = 65 - charPos;
        byte[] out = new byte[len];
        System.arraycopy(buf,charPos,out,0,len);
        return out;
    }

    public static final byte[] intToCharBytes(int i) {
        if (i == Integer.MIN_VALUE)
            return (byte[])MIN_INT_BYTE_ARRAY.clone();
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        byte[] buf = new byte[size];
        getCharBytes(i, size, buf);
        return buf;
    }

    public static final byte[] longToCharBytes(long i) {
        if (i == Long.MIN_VALUE)
            return (byte[])MIN_LONG_BYTE_ARRAY.clone();
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        byte[] buf = new byte[size];
        getCharBytes(i, size, buf);
        return buf;
    }
    public static final char[] longToChars(long i) {
        if (i == Long.MIN_VALUE)
            return (char[])MIN_LONG_CHAR_ARRAY.clone();
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        char[] buf = new char[size];
        getChars(i, size, buf);
        return buf;
    }
    
    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Integer.MIN_VALUE
     */
    public static final void getCharBytes(int i, int index, byte[] buf) {
        int q, r;
        int charPos = index;
        byte sign = 0;

        if (i < 0) { 
            sign = '-';
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
        // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf [--charPos] = DIGIT_ONES[r];
            buf [--charPos] =DIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) { 
            q = (i * 52429) >>> (16+3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf [--charPos] = DIGITS[r];
            i = q;
            if (i == 0) break;
        }
        if (sign != 0) {
            buf [--charPos] = sign;
        }
    }

    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Long.MIN_VALUE
     */
    public static final void getCharBytes(long i, int index, byte[] buf) {
        long q;
        int r;
        int charPos = index;
        byte sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) { 
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int)(i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf[--charPos] = DIGIT_ONES[r];
            buf[--charPos] =DIGIT_TENS[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = DIGIT_ONES[r];
            buf[--charPos] =DIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = DIGITS[r];
            i2 = q2;
            if (i2 == 0) break;
        }
        if (sign != 0) {
            buf[--charPos] = sign;
        }
    }
    public static final void getChars(long i, int index, char[] buf) {
        long q;
        int r;
        int charPos = index;
        char sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) { 
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int)(i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf[--charPos] = CDIGIT_ONES[r];
            buf[--charPos] = CDIGIT_TENS[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = CDIGIT_ONES[r];
            buf[--charPos] = CDIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = CDIGITS[r];
            i2 = q2;
            if (i2 == 0) break;
        }
        if (sign != 0) {
            buf[--charPos] = sign;
        }
    }

    
    /**
     * Requires positive x.
     * For negative numbers, reverse the sign before calling and add one to
     * the result (for the '-' sign).
     */
    public static final int arraySize(long x) {
        long p = 10;
        for (int i=1; i<19; i++) {
            if (x < p)
                return i;
            p = 10*p;
        }
        return 19;
    }
    /**
     * Requires positive x.
     * For negative numbers, reverse the sign before calling and add one to
     * the result (for the '-' sign).
     */
    public static final int arraySize(int x) {
        for (int i=0; ; i++)
            if (x <= SIZE_TABLE[i])
                return i+1;
    }
    // the following group of conversions to binary/octal/hex
    // is mostly for use by the new sprintf code
    public static final byte[] intToBinaryBytes(int i) {
        return intToUnsignedBytes(i, 1, false);
    }
    public static final byte[] intToOctalBytes(int i) {
        return intToUnsignedBytes(i, 3, false);
    }
    public static final byte[] intToHexBytes(int i) {
        return intToUnsignedBytes(i, 4, false);
    }

    public static final byte[] intToHexBytes(int i, boolean upper) {
        return intToUnsignedBytes(i, 4, upper);
    }

    public static final ByteList intToBinaryByteList(int i) {
        return new ByteList(intToUnsignedBytes(i, 1, false));
    }
    public static final ByteList intToOctalByteList(int i) {
        return new ByteList(intToUnsignedBytes(i, 3, false));
    }
    public static final ByteList intToHexByteList(int i) {
        return new ByteList(intToUnsignedBytes(i, 4, false));
    }
    public static final ByteList intToHexByteList(int i, boolean upper) {
        return new ByteList(intToUnsignedBytes(i, 4, upper));
    }

    public static final byte[] longToBinaryBytes(long i) {
        return longToUnsignedBytes(i, 1, false);
    }
    public static final byte[] longToOctalBytes(long i) {
        return longToUnsignedBytes(i, 3, false);
    }
    public static final byte[] longToHexBytes(long i) {
        return longToUnsignedBytes(i, 4, false);
    }

    public static final byte[] longToHexBytes(long i, boolean upper) {
        return longToUnsignedBytes(i, 4, true);
    }

    public static final ByteList longToBinaryByteList(long i) {
        return new ByteList(longToUnsignedBytes(i, 1, false));
    }
    public static final ByteList longToOctalByteList(long i) {
        return new ByteList(longToUnsignedBytes(i, 3, false));
    }
    public static final ByteList longToHexByteList(long i) {
        return new ByteList(longToUnsignedBytes(i, 4, false));
    }
    public static final ByteList longToHexByteList(long i, boolean upper) {
        return new ByteList(longToUnsignedBytes(i, 4, upper));
    }
    /**
     * Convert the integer to an unsigned number.
     * The character bytes are right-aligned in the 32-byte result.
     */
    public static final byte[] intToRawUnsignedBytes(int i, int shift) {
        byte[] buf = new byte[32];
        int charPos = 32;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[i & mask];
            i >>>= shift;
        } while (i != 0);
        return buf;
    }
    /**
     * Convert the integer to an unsigned number.
     * The result array is sized to fit the actual character length.
     */
    public static final byte[] intToUnsignedBytes(int i, int shift, boolean upper) {
        byte[] buf = new byte[32];
        int charPos = 32;
        int radix = 1 << shift;
        int mask = radix - 1;
        byte[] digits = upper ? UCDIGITS : DIGITS;
        do {
            buf[--charPos] = digits[i & mask];
            i >>>= shift;
        } while (i != 0);
        int length = 32 - charPos;
        byte[] result = new byte[length];
        System.arraycopy(buf,charPos,result,0,length);
        return result;
    }

    /**
     * Convert the long to an unsigned number.
     * The character bytes are right-aligned in the 64-byte result.
     */
    public static final byte[] longToRawUnsignedBytes(long i, int shift) {
        byte[] buf = new byte[64];
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[(int)(i & mask)];
            i >>>= shift;
        } while (i != 0);
        return buf;
    }
    /**
     * Convert the long to an unsigned number.
     * The result array is sized to fit the actual character length.
     */
    public static final byte[] longToUnsignedBytes(long i, int shift, boolean upper) {
        byte[] buf = new byte[64];
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
        byte[] digits = upper ? UCDIGITS : DIGITS;
        do {
            buf[--charPos] = digits[(int)(i & mask)];
            i >>>= shift;
        } while (i != 0);
        int length = 64 - charPos;
        byte[] result = new byte[length];
        System.arraycopy(buf,charPos,result,0,length);
        return result;
    }

    /**
     * Converts a ByteList to a primitive long value, using the specified
     * base. If base is zero, defaults to 10 unless a base specifier is encountered
     * (e.g., '0x'). Follows Ruby rules for converting Strings to Integers. Will fail
     * with NumberFormatException if the number is too large for a long.  If the
     * raise flag is set, will also fail on certain formatting errors (zero-length
     * array; zero-length excluding sign; no valid digits).
     * 
     * @param bytes
     * @param buflen the effective length of the array (may be less than bytes.length) 
     * @param base
     * @param raise
     * @return
     * @throws NumberFormatException
     */
    public static final long byteListToLong(ByteList bytes, int base, boolean raise) {
        return byteArrayToLong(bytes.unsafeBytes(),bytes.length(),base,raise);
    }
    public static final long byteListToLong(ByteList bytes, int base) {
        return byteArrayToLong(bytes.unsafeBytes(),bytes.length(),base,false);
    }
    // for base 10 ByteList
    public static final long byteListToLong(ByteList bytes) {
        return byteArrayToLong(bytes.unsafeBytes(),bytes.length(),10,false);
    }
    /**
     * Converts a ByteList to a BigInteger value, using the specified  base.
     * If base is zero, defaults to 10 unless a base specifier is encountered
     * (e.g., '0x'). Follows Ruby rules for converting Strings to Integers. Will
     * fail with NumberFormatException on certain formatting errors (zero-length
     * array; zero-length excluding sign; no valid digits).
     * <p>
     * Intended to be called after byteListToLong if that method fails.
     * 
     * @param bytes
     * @param buflen the effective length of the array (may be less than bytes.length) 
     * @param base
     * @return
     * @throws NumberFormatException, IllegalArgumentException
     */
    public static final BigInteger byteListToBigInteger(ByteList bytes, int base, boolean raise) {
        return byteArrayToBigInteger(bytes.unsafeBytes(),bytes.length(),base,raise);
    }
    public static final BigInteger byteListToBigInteger(ByteList bytes, int base) {
        return byteArrayToBigInteger(bytes.unsafeBytes(),bytes.length(),base,false);
    }
    // for base 10 ByteList
    public static final BigInteger byteListToBigInteger(ByteList bytes) {
        return byteArrayToBigInteger(bytes.unsafeBytes(),bytes.length(),10,false);
    }
   /**
     * Converts a byte array to a primitive long value, using the specified
     * base. If base is zero, defaults to 10 unless a base specifier is encountered
     * (e.g., '0x'). Follows Ruby rules for converting Strings to Integers. Will fail
     * with NumberFormatException if the number is too large for a long.  If the
     * raise flag is set, will also fail on certain formatting errors (zero-length
     * array; zero-length excluding sign; no valid digits).
     * 
     * @param bytes
     * @param buflen the effective length of the array (may be less than bytes.length) 
     * @param base
     * @param strict
     * @return
     * @throws NumberFormatException, IllegalArgumentException
     */
    public static final long byteArrayToLong(byte[] bytes, int buflen, int base, boolean strict) {
        final int SCOMPLETE         = 0;
        final int SBEGIN            = 1;
        final int SSIGN             = 2;
        final int SZERO             = 3;
        final int SPOST_SIGN        = 4;
        final int SDIGITS           = 5;
        final int SDIGIT            = 6;
        final int SDIGIT_STRICT     = 7;
        final int SDIGIT_USC        = 8;
        final int SEOD_STRICT       = 13;
        final int SEOF              = 14;
        final int SERR_NOT_STRICT   = 17;
        final int SERR_TOO_BIG      = 18;
        final int FLAG_NEGATIVE    = 1 << 0;
        final int FLAG_DIGIT       = 1 << 1;
        final int FLAG_UNDERSCORE  = 1 << 2;
        final int FLAG_WHITESPACE  = 1 << 3;
      
        if (bytes == null) {
            throw new IllegalArgumentException("null bytes");
        }
        if (buflen < 0 || buflen > bytes.length) {
            throw new IllegalArgumentException("invalid buflen specified");
        }
        int radix = base == 0 ? 10 : base;
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new IllegalArgumentException("illegal radix " + radix);
        }
        if (buflen == 0) {
            throw new InvalidIntegerException();
        }
        int i = 0;
        byte ival;
        int flags = 0;
        long limit = -Long.MAX_VALUE;
        long result = 0;
        long multmin = 0;
        int digit;
        int state = SBEGIN;
        while (state != SCOMPLETE) {
            states:
            switch(state) {
            case SBEGIN:
                if (strict) {
                    for (; i < buflen && bytes[i] <= ' '; i++) ;
                } else {
                    for (; i < buflen && ((ival = bytes[i]) <= ' ' || ival == '_'); i++) ;
                }
                state = i < buflen ? SSIGN : SEOF;
                break;
            case SSIGN:
                switch(bytes[i]) {
                case '-':
                    flags |= FLAG_NEGATIVE;
                    limit = Long.MIN_VALUE;
                case '+':
                    if (++i >= buflen) {
                        state = SEOF;
                    } else if (bytes[i] == '0') {
                        state = SZERO;
                    } else {
                        state = SPOST_SIGN;
                    }
                    break states;
                case '0':
                    state = SZERO;
                    break states;
                default:
                    state = SDIGITS;
                    break states;
                }
            case SZERO:
                if (++i >= buflen) {
                    state = SCOMPLETE;
                    break;
                }
                switch (bytes[i]) {
                case 'x':
                case 'X':
                    if (base == 0 || base == 16) {
                        radix = 16;
                        state = ++i >= buflen ? SEOF : SPOST_SIGN;
                    } else {
                        state = SDIGITS;
                    }
                    break states;
                case 'b':
                case 'B':
                    if (base == 0 || base == 2) {
                        radix = 2;
                        state = ++i >= buflen ? SEOF : SPOST_SIGN;
                    } else {
                        state = SDIGITS;
                    }
                    break states;
                default:
                    if (base == 0 || base == 8) {
                        radix = 8;
                    }
                    flags |= FLAG_DIGIT;
                    state = SDIGITS;
                    break states;
                }
            case SPOST_SIGN:
                if (strict) {
                    int ibefore = i;
                    for (; i < buflen && bytes[i] <= ' '; i++) ;
                    if (ibefore != i) {
                        // set this to enforce no-underscore rule (though I think 
                        // it's an MRI bug)
                        flags |= FLAG_WHITESPACE;
                    }
                } else {
                    for ( ; i < buflen && ((ival = bytes[i]) <= ' ' || ival == '_'); i++) {
                        if (ival == '_') {
                            if ((flags & FLAG_WHITESPACE) != 0) {
                                throw new InvalidIntegerException();
                            }
                            flags |= FLAG_UNDERSCORE;
                        } else {
                            if ((flags & FLAG_UNDERSCORE) != 0) {
                                throw new InvalidIntegerException();
                            }
                            flags |= FLAG_WHITESPACE;
                        }
                    }
                }
                state = i < buflen ? SDIGITS : SEOF;
                break;
            case SDIGITS:
                digit = Character.digit((char) bytes[i],radix);
                if (digit < 0) {
                    state = strict ? SEOD_STRICT : SEOF;
                    break;
                }
                result = -digit;
                if (++i >= buflen) {
                    state = SCOMPLETE;
                    break;
                }
                multmin = limit / radix;
                flags = (flags | FLAG_DIGIT) & ~FLAG_UNDERSCORE;
                state = strict ? SDIGIT_STRICT : SDIGIT;
                break;

            case SDIGIT:
                while ((digit = Character.digit((char) bytes[i],radix)) >= 0) {
                    if (result < multmin || ((result *= radix) < limit + digit)) {
                        state = SERR_TOO_BIG;
                        break states;
                    }
                    result -= digit;
                    if (++i >= buflen) {
                        state = SCOMPLETE;
                        break states;
                    }
                }
                state = bytes[i++] == '_' ? SDIGIT_USC : SEOF;
                break;
            case SDIGIT_USC:
                for ( ; i < buflen && bytes[i] == '_'; i++) ;
                state = i < buflen ? SDIGIT : SEOF;
                break;
                
            case SDIGIT_STRICT:
                while ((digit = Character.digit((char) bytes[i],radix)) >= 0) {
                    if (result < multmin || ((result *= radix) < limit + digit)) {
                        state = SERR_TOO_BIG;
                        break states;
                    }
                    result -= digit;
                    if (++i >= buflen) {
                        state = SCOMPLETE;
                        break states;
                    }
                    flags &= ~FLAG_UNDERSCORE;
                }
                if (bytes[i] == '_') {
                    if ((flags & (FLAG_UNDERSCORE | FLAG_WHITESPACE)) != 0) {
                        state = SERR_NOT_STRICT;
                        break;
                    }
                    flags |= FLAG_UNDERSCORE;
                    state = ++i >= buflen ? SEOD_STRICT : SDIGIT_STRICT;
                } else {
                    state = SEOD_STRICT;
                }
                break;

            case SEOD_STRICT:
                if ((flags & FLAG_UNDERSCORE)!= 0) {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen && bytes[i] <= ' '; i++ );
                state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE;
                break;
                
            case SEOF:
                if ((flags & FLAG_DIGIT) == 0) {
                    throw new InvalidIntegerException("no digits supplied");
                }
                state = SCOMPLETE;
                break;

            case SERR_TOO_BIG:
                throw new NumberTooLargeException("can't convert to long");

            case SERR_NOT_STRICT:
                throw new InvalidIntegerException("does not meet strict criteria");
                
            } // switch
        } //while
        if ((flags & FLAG_NEGATIVE) == 0) {
            return -result;
        } else {
            return result;
        }
    }
    
    
    /**
     * Converts a byte array to a BigInteger value, using the specified  base.
     * If base is zero, defaults to 10 unless a base specifier is encountered
     * (e.g., '0x'). Follows Ruby rules for converting Strings to Integers. Will
     * fail with NumberFormatException on certain formatting errors (zero-length
     * array; zero-length excluding sign; no valid digits).
     * <p>
     * Intended to be called after byteArrayToLong if that method fails.
     * 
     * @param bytes
     * @param buflen the effective length of the array (may be less than bytes.length) 
     * @param base
     * @return
     * @throws NumberFormatException, IllegalArgumentException
     */
    public static final BigInteger byteArrayToBigInteger(byte[] bytes, int buflen, int base, boolean strict) {
        final int SCOMPLETE         = 0;
        final int SBEGIN            = 1;
        final int SSIGN             = 2;
        final int SZERO             = 3;
        final int SPOST_SIGN        = 4;
        final int SDIGITS           = 5;
        final int SDIGIT            = 6;
        final int SDIGIT_STRICT     = 7;
        final int SDIGIT_USC        = 8;
        final int SEOD_STRICT       = 13;
        final int SEOF              = 14;
        final int SERR_NOT_STRICT   = 17;
        final int FLAG_NEGATIVE    = 1 << 0;
        final int FLAG_DIGIT       = 1 << 1;
        final int FLAG_UNDERSCORE  = 1 << 2;
        final int FLAG_WHITESPACE  = 1 << 3;
      
        if (bytes == null) {
            throw new IllegalArgumentException("null bytes");
        }
        if (buflen < 0 || buflen > bytes.length) {
            throw new IllegalArgumentException("invalid buflen specified");
        }
        int radix = base == 0 ? 10 : base;
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new IllegalArgumentException("illegal radix " + radix);
        }
        if (buflen == 0) {
            throw new InvalidIntegerException();
        }
        int i = 0;
        byte ival;
        int flags = 0;
        int digit;
        int offset = 0;
        char[] chars = null;
        int state = SBEGIN;
        while (state != SCOMPLETE) {
            states:
            switch(state) {
            case SBEGIN:
                if (strict) {
                    for (; i < buflen && bytes[i] <= ' '; i++) ;
                } else {
                    for (; i < buflen && ((ival = bytes[i]) <= ' ' || ival == '_'); i++) ;
                }
                state = i < buflen ? SSIGN : SEOF;
                break;
            case SSIGN:
                switch(bytes[i]) {
                case '-':
                    flags |= FLAG_NEGATIVE;
                case '+':
                    if (++i >= buflen) {
                        state = SEOF;
                    } else if (bytes[i] == '0') {
                        state = SZERO;
                    } else {
                        state = SPOST_SIGN;
                    }
                    break states;
                case '0':
                    state = SZERO;
                    break states;
                default:
                    state = SDIGITS;
                    break states;
                }
            case SZERO:
                if (++i >= buflen) {
                    state = SCOMPLETE;
                    break;
                }
                switch (bytes[i]) {
                case 'x':
                case 'X':
                    if (base == 0 || base == 16) {
                        radix = 16;
                        state = ++i >= buflen ? SEOF : SPOST_SIGN;
                    } else {
                        state = SDIGITS;
                    }
                    break states;
                case 'b':
                case 'B':
                    if (base == 0 || base == 2) {
                        radix = 2;
                        state = ++i >= buflen ? SEOF : SPOST_SIGN;
                    } else {
                        state = SDIGITS;
                    }
                    break states;
                default:
                    if (base == 0 || base == 8) {
                        radix = 8;
                    }
                    flags |= FLAG_DIGIT;
                    state = SDIGITS;
                    break states;
                }
            case SPOST_SIGN:
                if (strict) {
                    int ibefore = i;
                    for (; i < buflen && bytes[i] <= ' '; i++) ;
                    if (ibefore != i) {
                        // set this to enforce no-underscore rule (though I think 
                        // it's an MRI bug)
                        flags |= FLAG_WHITESPACE;
                    }
                } else {
                    for ( ; i < buflen && ((ival = bytes[i]) <= ' ' || ival == '_'); i++) {
                        if (ival == '_') {
                            if ((flags & FLAG_WHITESPACE) != 0) {
                                throw new InvalidIntegerException();
                            }
                            flags |= FLAG_UNDERSCORE;
                        } else {
                            if ((flags & FLAG_UNDERSCORE) != 0) {
                                throw new InvalidIntegerException();
                            }
                            flags |= FLAG_WHITESPACE;
                        }
                    }
                }
                state = i < buflen ? SDIGITS : SEOF;
                break;
            case SDIGITS:
                digit = Character.digit((char) bytes[i],radix);
                if (digit < 0) {
                    state = strict ? SEOD_STRICT : SEOF;
                    break;
                }
                if ((flags & FLAG_NEGATIVE) == 0) {
                    chars = new char[buflen - i];
                    chars[0] = (char)bytes[i];
                    offset = 1;
                } else {
                    chars = new char[buflen - i + 1];
                    chars[0] = '-';
                    chars[1] = (char)bytes[i];
                    offset = 2;
                }
                if (++i >= buflen) {
                    state = SCOMPLETE;
                    break;
                }
                flags = (flags | FLAG_DIGIT) & ~FLAG_UNDERSCORE;
                state = strict ? SDIGIT_STRICT : SDIGIT;
                break;

            case SDIGIT:
                while ((digit = Character.digit((char) bytes[i],radix)) >= 0) {
                    chars[offset++] = (char)bytes[i];
                    if (++i >= buflen) {
                        state = SCOMPLETE;
                        break states;
                    }
                }
                state = bytes[i++] == '_' ? SDIGIT_USC : SEOF;
                break;
            case SDIGIT_USC:
                for ( ; i < buflen && bytes[i] == '_'; i++) ;
                state = i < buflen ? SDIGIT : SEOF;
                break;
                
            case SDIGIT_STRICT:
                while ((digit = Character.digit((char)bytes[i],radix)) >= 0) {
                    chars[offset++] = (char)bytes[i];
                    if (++i >= buflen) {
                        state = SCOMPLETE;
                        break states;
                    }
                    flags &= ~FLAG_UNDERSCORE;
                }
                if (bytes[i] == '_') {
                    if ((flags & (FLAG_UNDERSCORE | FLAG_WHITESPACE)) != 0) {
                        state = SERR_NOT_STRICT;
                        break;
                    }
                    flags |= FLAG_UNDERSCORE;
                    state = ++i >= buflen ? SEOD_STRICT : SDIGIT_STRICT;
                } else {
                    state = SEOD_STRICT;
                }
                break;

            case SEOD_STRICT:
                if ((flags & FLAG_UNDERSCORE)!= 0) {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen && bytes[i] <= ' '; i++ );
                state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE;
                break;
                
            case SEOF:
                if ((flags & FLAG_DIGIT) == 0) {
                    throw new InvalidIntegerException("no digits supplied");
                }
                state = SCOMPLETE;
                break;

            case SERR_NOT_STRICT:
                throw new InvalidIntegerException("does not meet strict criteria");
                
            } // switch
        } //while
        if (chars == null) { // 0, won't happen if byteArrayToLong called first
            return BIG_INT_ZERO;
        } else {
            return new BigInteger(new String(chars,0,offset),radix);
        }
    }
    /**
     * Converts a ByteList containing a RubyString representation of a double
     * value to a double.  Equivalent to Double.parseDouble(String s), but accounts for
     * embedded underscore characters, as permitted in Ruby strings (single underscores
     * allowed between digits in strict mode, multiple in non-strict mode).
     *  
     * @param bytes the ByteList containing the RubyString value to convert
     * @param strict if true, strict rules (as required by Float(str)) are enforced;
     *               otherwise, the laxer rules of str.to_f are employed.
     * @return the converted double value
     */
    public static final double byteListToDouble(ByteList bytes, boolean strict) {
        return byteArrayToDouble(bytes.unsafeBytes(),bytes.length(),strict);
    }
    public static final double byteListToDouble(ByteList bytes) {
        return byteArrayToDouble(bytes.unsafeBytes(),bytes.length(),false);
    }
    /**
     * Converts a byte array containing a RubyString representation of a double
     * value to a double.  Equivalent to Double.parseDouble(String s), but accounts for
     * embedded underscore characters, as permitted in Ruby strings (single underscores
     * allowed between digits in strict mode, multiple in non-strict mode).
     *  
     * @param bytes the array containing the RubyString value to convert
     * @param buflen the length of the array to be used
     * @param strict if true, strict rules (as required by Float(str)) are enforced;
     *               otherwise, the laxer rules of str.to_f are employed.
     * @return the converted double value
     */
    public static final double byteArrayToDouble(byte[] bytes, int buflen, boolean strict) {
        // Simple cases  ( abs(exponent) <= 22 [up to 37 depending on significand length])
        // are converted directly, which is considerably faster than creating a Java
        // String and passing it to Double.parseDouble() (which in turn passes it to
        // sun.misc.FloatingDecimal); the latter approach involves 5 object allocations
        // (3 arrays + String + FloatingDecimal) and 3 array copies, two of them one byte/char
        // at a time (here and in FloatingDecimal).
        // However, the latter approach is employed for more difficult cases (generally
        // speaking, those that require rounding). (The code for the difficult cases is 
        // quite involved; see sun.misc.FloatingDecimal.java if you're interested.)

        // states
        final int SCOMPLETE            =  0;
        final int SBEGIN               =  1; // remove leading whitespace (includes _ for lax)
        final int SSIGN                =  2; // get sign, if any
        
        // optimistic pass - calculate value as digits are processed
        final int SOPTDIGIT            =  3; // digits - lax rules
        final int SOPTDECDIGIT         =  4; // decimal digits - lax rules
        final int SOPTEXP              =  9; // exponent sign/digits - lax rules
        final int SOPTDIGIT_STRICT     =  6; // digits - strict rules
        final int SOPTDECDIGIT_STRICT  =  7; // decimal digits - strict rules
        final int SOPTEXP_STRICT       =  8; // exponent sign/digits - strict rules
        final int SOPTCALC             =  5; // complete calculation if possible

        // fallback pass - gather chars into array and pass to Double.parseDouble()
        final int SDIGIT               = 10; // digits - lax rules
        final int SDECDIGIT            = 11; // decimal digits - lax rules
        final int SEXP                 = 12; // exponent sign/digits - lax rules
        final int SDIGIT_STRICT        = 13; // digits - strict rules
        final int SDECDIGIT_STRICT     = 14; // decimal digits - strict rules
        final int SEXP_STRICT          = 15; // exponent sign/digits - strict rules

        final int SERR_NOT_STRICT      = 16;

        // largest abs(exponent) we can (potentially) handle without
        // calling Double.parseDouble() (aka sun.misc.FloatingDecimal)
        final int MAX_EXP = MAX_DECIMAL_DIGITS + MAX_SMALL_10; // (37)
      
        if (bytes == null) {
            throw new IllegalArgumentException("null bytes");
        }
        if (buflen < 0 || buflen > bytes.length) {
            throw new IllegalArgumentException("invalid buflen specified");
        }
        // TODO: get rid of this (lax returns 0.0, strict will throw)
        if (buflen == 0) {
            throw new NumberFormatException();
        }
        int i = 0;
        byte ival;
        boolean negative = false;

        // fields used for direct (optimistic) calculation
        int nDigits = 0;         // number of significant digits, updated as parsed
        int nTrailingZeroes = 0; // zeroes that may go to significand or exponent
        int decPos = -1;         // offset of decimal pt from start (-1 -> no decimal)
        long significand = 0;    // significand, updated as parsed
        int exponent = 0;        // exponent, updated as parsed
        
        // fields used for fallback (send to Double.parseDouble())
        int startPos = 0;        // start of digits (or . if no leading digits)
        char[] chars = null;
        int offset = 0;
        int lastValidOffset = 0;

        int state = SBEGIN;
        while (state != SCOMPLETE) {
        states:
            switch(state) {
            case SBEGIN:
                if (strict) {
                    for (; i < buflen && bytes[i] <= ' '; i++) ;
                } else {
                    for (; i < buflen && ((ival = bytes[i]) <= ' ' || ival == '_'); i++) ;
                }
                if ( i >= buflen) {
                    state = strict ? SERR_NOT_STRICT : SCOMPLETE;
                    break;
                }
                // drop through for sign
            case SSIGN:
                switch(bytes[i]) {
                case '-':
                    negative = true;
                case '+':
                    if (++i >= buflen) {
                        // TODO: turn off the negative? will return -0.0 in lax mode
                        state = strict ? SERR_NOT_STRICT : SCOMPLETE;
                        break states;
                    }
                } // switch
                startPos = i; // will use this if we have to go back the slow way
                if (strict) {
                    state = SOPTDIGIT_STRICT;
                    break;
                }
                // drop through for non-strict digits
            case SOPTDIGIT:
                // first char must be digit or decimal point
                switch(ival = bytes[i++]) {
                case '0':
                    // ignore leading zeroes
                    break; // switch
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    significand = (long)((int)ival-(int)'0');
                    nDigits = 1;
                    break; // switch
                case '.':
                    state = SOPTDECDIGIT;
                    break states;
                default:
                    // no digits, go calc (will return +/- 0.0 for lax)
                    state = SOPTCALC;
                    break states;
                } // switch
                for ( ; i < buflen ;  ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        // ignore leading zeroes
                        if (nDigits > 0) {
                            // just save a count of zeroes for now; if no digit
                            // ends up following them, they'll be applied to the
                            // exponent rather than the significand (and our max
                            // length for optimistic calc).
                            nTrailingZeroes++;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                         // ok, got a non-zero, have to own up to our horded zeroes
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            // oh, well, it was worth a try. go let
                            // Double/FloatingDecimal handle it 
                            state = SDIGIT;
                            break states;
                        }
                    case '.':
                        state = SOPTDECDIGIT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SOPTEXP;
                        break states;
                    case '_':
                        // ignore
                        break; // switch
                    default:
                        // end of parseable data, go to calc
                        state = SOPTCALC;
                        break states;
                        
                    } // switch
                } // for
                state = SOPTCALC;
                break;

            case SOPTDECDIGIT:
                decPos = nDigits + nTrailingZeroes;
                for ( ; i < buflen && bytes[i] == '_'; i++ ) ;
                // first non_underscore char must be digit
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    default:
                        // no dec digits, end of parseable data, go to calc
                        state = SOPTCALC;
                        break states;
                        
                    } // switch
                } // if
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    case 'e':
                    case 'E':
                        state = SOPTEXP;
                        break states;
                    case '_':
                        // ignore
                        break; // switch
                    default:
                        // end of parseable data, go to calc
                        state = SOPTCALC;
                        break states;
                    } // switch
                } // for
                // no exponent, so drop through for calculation
            case SOPTCALC:
                // calculation for simple (and typical) case,
                // adapted from sun.misc.FloatingDecimal
                if (nDigits == 0) {
                    return negative ? -0.0d : 0.0d;
                }
                if (decPos < 0) {
                    exponent += nTrailingZeroes;
                } else {
                    exponent += decPos - nDigits;
                }
                double dValue = (double)significand;
                if (exponent == 0 || dValue == 0.0) {
                    return negative ? -dValue : dValue;
                } else if ( exponent >= 0 ){
                    if ( exponent <= MAX_SMALL_10 ){
                        dValue *= SMALL_10_POWERS[exponent];
                        return negative ? -dValue : dValue;
                    }
                    int slop = MAX_DECIMAL_DIGITS - nDigits;
                    if ( exponent <= MAX_SMALL_10+slop ){
                        dValue = (dValue * SMALL_10_POWERS[slop]) * SMALL_10_POWERS[exponent-slop];
                        return negative ? -dValue : dValue;
                    }
                } else {
                    // TODO: it's not clear to me why, in FloatingDecimal, the
                    // "slop" calculation performed above for positive exponents
                    // isn't used for negative exponents as well. Will find out...
                    if ( exponent >= -MAX_SMALL_10 ){
                        dValue = dValue / SMALL_10_POWERS[-exponent];
                        return negative ? -dValue : dValue;
                    }
                }
                // difficult case, send to Double/FloatingDecimal
                state = SDIGIT;
                break;
                
            case SOPTEXP:
            {
                // lax (str.to_f) allows underscores between e/E and sign
                for ( ; i < buflen && bytes[i] == '_' ; i++ ) ;
                if (i >= buflen) {
                    state = SOPTCALC;
                    break;
                }
                int expSign = 1;
                int expSpec = 0;
                switch (bytes[i]) {
                case '-':
                    expSign = -1;
                case '+':
                    if (++i >= buflen) {
                        state = SOPTCALC;
                        break states;
                    }
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if ((expSpec = expSpec * 10 + ((int)ival-(int)'0')) >= MAX_EXP) {
                            // too big for us
                            state = SDIGIT;
                            break states;
                        }
                        break; //switch
                    case '_':
                        break; //switch
                    default:
                        exponent += expSign * expSpec;
                        state = SOPTCALC;
                        break states;
                    }
                }
                exponent += expSign * expSpec;
                state = SOPTCALC;
                break;
            } // block

            case SOPTDIGIT_STRICT:
                // first char must be digit or decimal point
                switch(ival = bytes[i++]) {
                case '0':
                    break; // switch
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    significand = (long)((int)ival-(int)'0');
                    nDigits = 1;
                    break; // switch
                case '.':
                    state = SOPTDECDIGIT_STRICT;
                    break states;
                default:
                    // no digits, error
                    state = SERR_NOT_STRICT;
                    break states;
                }
                for ( ; i < buflen ;  ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    case '.':
                        state = SOPTDECDIGIT_STRICT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SOPTEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; // switch
                    default:
                        // only whitespace allowed after value for strict
                        for ( ; i < buflen && bytes[i] <= ' '; i++ );
                        state = i < buflen ? SERR_NOT_STRICT : SOPTCALC; 
                        break states;
                    } // switch
                } // for
                // no more data, OK for strict to go calc
                state = SOPTCALC;
                break;

            case SOPTDECDIGIT_STRICT:
                decPos = nDigits + nTrailingZeroes;
                // first char must be digit
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    default:
                        // no dec digits after '.', error for strict
                        state = SERR_NOT_STRICT;
                        break states;
                        
                    } // switch
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        if (nDigits > 0) {
                            nTrailingZeroes++;
                        } else {
                            exponent--;
                        }
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (nTrailingZeroes > 0) {
                            if ((nDigits += nTrailingZeroes) < MAX_DECIMAL_DIGITS) {
                                significand *= LONG_10_POWERS[nTrailingZeroes];
                                nTrailingZeroes = 0;
                            } // else catch oversize below
                        }
                        if (nDigits++ < MAX_DECIMAL_DIGITS) {
                            significand = significand*10L + (long)((int)ival-(int)'0');
                            break; // switch
                        } else {
                            state = SDIGIT;
                            break states;
                        }
                    case 'e':
                    case 'E':
                        state = SOPTEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; // switch
                    default:
                        // only whitespace allowed after value for strict
                        for ( ; i < buflen && bytes[i] <= ' '; i++);
                        state = i < buflen ? SERR_NOT_STRICT : SOPTCALC; 
                        break states;
                    } // switch
                } // for
                // no more data, OK for strict to go calc
                state = SOPTCALC;
                break;
            
            case SOPTEXP_STRICT:
            {
                int expSign = 1;
                int expSpec = 0;

                if ( i < buflen) {
                    switch (bytes[i]) {
                    case '-':
                        expSign = -1;
                    case '+':
                        if (++i >= buflen) {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                // must be at least one digit for strict
                if ( i < buflen ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        expSpec = (int)ival-(int)'0';
                        break; //switch
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if ((expSpec = expSpec * 10 + ((int)ival-(int)'0')) >= MAX_EXP) {
                            // too big for us
                            state = SDIGIT;
                            break states;
                        }
                        break; //switch
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        exponent += expSign * expSpec;
                        // only whitespace allowed after value for strict
                        for ( ; i < buflen && bytes[i] <= ' ';  i++);
                        state = i < buflen ? SERR_NOT_STRICT : SOPTCALC; 
                        break states;
                    } // switch
                } // for
                exponent += expSign * expSpec;
                state = SOPTCALC;
                break;
            } // block
                
            // fallback, copy non-whitespace chars to char buffer and send
            // to Double.parseDouble() (front for sun.misc.FloatingDecimal)
            case SDIGIT:
                i = startPos;
                if (negative) {
                    chars = new char[buflen - i + 1];
                    chars[0] = '-';
                    offset = 1;
                } else {
                    chars = new char[buflen - i];
                }
                if (strict) {
                    state = SDIGIT_STRICT;
                    break;
                }
                // first char must be digit or decimal point
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        // ignore leading zeroes
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT;
                        break states;
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // if
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SEXP;
                        break states;
                    case '_':
                        break; // switch
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;
                
            case SDECDIGIT:
                chars[offset++] = '.';
                for ( ; i < buflen && bytes[i] == '_'; i++) ;
                if ( i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // if
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case 'e':
                    case 'E':
                        state = SEXP;
                        break states;
                    case '_':
                        break; // switch
                    default:
                        state = SCOMPLETE;
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;

            case SEXP:
                chars[offset++] = 'E';
                for ( ; i < buflen && bytes[i] == '_'; i++) ;
                if (i >= buflen) {
                    state = SCOMPLETE;
                    break;
                }
                switch(bytes[i]) {
                case '-':
                case '+':
                    chars[offset++] = (char)bytes[i];
                    if (++i >= buflen) {
                        state = SCOMPLETE;
                        break states;
                    }
                }
                for ( ; i < buflen; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break;
                    case '_':
                        break;
                    default:
                        state = SCOMPLETE;
                        break states;
                    }
                }
                state = SCOMPLETE;
                break;
            
            case SDIGIT_STRICT:
                // first char must be digit or decimal point
                if (i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0':
                        // ignore leading zeroes
                        break; // switch
                    case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT_STRICT;
                        break states;
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    } // switch
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case '.':
                        state = SDECDIGIT_STRICT;
                        break states;
                    case 'e':
                    case 'E':
                        state = SEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        // only whitespace allowed after value for strict
                        for ( ; i < buflen && bytes[i] <= ' ';  i++) ;
                        state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE; 
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;
                
            case SDECDIGIT_STRICT:
                chars[offset++] = '.';
                if ( i < buflen) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    } // switch
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; // switch
                    case 'e':
                    case 'E':
                        state = SEXP_STRICT;
                        break states;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        for ( ; i < buflen && bytes[i] <= ' ';  i++) ;
                        state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE; 
                        break states;
                    } // switch
                } // for
                state = SCOMPLETE;
                break;

            case SEXP_STRICT:
                chars[offset++] = 'E';
                if ( i < buflen ) {
                    switch (bytes[i]) {
                    case '-':
                    case '+':
                        chars[offset++] = (char)bytes[i];
                        if (++i >= buflen) {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                // must be at least one digit for strict
                if ( i < buflen ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break; //switch
                    default:
                        state = SERR_NOT_STRICT;
                        break states;
                    }
                } else {
                    state = SERR_NOT_STRICT;
                    break;
                }
                for ( ; i < buflen ; ) {
                    switch(ival = bytes[i++]) {
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        chars[offset++] = (char)ival;
                        lastValidOffset = offset;
                        break;
                    case '_':
                        if (i >= buflen || bytes[i] < '0' || bytes[i] > '9') {
                            state = SERR_NOT_STRICT;
                            break states;
                        }
                        break; //switch
                    default:
                        for ( ; i < buflen && bytes[i] <= ' ';  i++) ;
                        state = i < buflen ? SERR_NOT_STRICT : SCOMPLETE; 
                        break states;
                    }
                }
                state = SCOMPLETE;
                break;
            
            case SERR_NOT_STRICT:
                throw new NumberFormatException("does not meet strict criteria");
                
            } // switch
        } //while
        if (chars == null || lastValidOffset == 0) {
            return 0.0;
        } else {
            return Double.parseDouble(new String(chars,0,lastValidOffset));
        }
    }

    public static final byte[] doubleToByteArray(double d) {
        // TODO: develop an efficient method to do this directly and avoid all 
        // the excess array allocating/copying (since we'll need to parse this
        // result anyway to format it for output). See sun.misc.FloatingDecimal.
        return ByteList.plain(Double.toString(d));
    }
    

    public static final byte[] twosComplementToBinaryBytes(byte[] in) {
        return twosComplementToUnsignedBytes(in,1,false);
    }
    public static final byte[] twosComplementToOctalBytes(byte[] in) {
        return twosComplementToUnsignedBytes(in,3,false);
    }
    public static final byte[] twosComplementToHexBytes(byte[] in, boolean upper) {
        return twosComplementToUnsignedBytes(in,4,upper);
    }
    // shift is power of 2, so 1 = binary, 3 - octal, 4 = hex, anyhing
    // larger will error.
    public static final byte[] twosComplementToUnsignedBytes(byte[] in, int shift, boolean upper) {
        if (shift < 1 || shift > 4) {
            throw new IllegalArgumentException("shift value must be 1-4");
        }
        int ilen = in.length;
        int olen = (ilen * 8 + shift - 1 ) / shift;
        byte[] out = new byte[olen];
        int mask = (1 << shift) - 1;
        byte[] digits = upper ? UCDIGITS : DIGITS;
        int bitbuf = 0;
        int bitcnt = 0;
        for (int i = ilen, o = olen; --o >= 0; ) {
            if (bitcnt < shift) {
                bitbuf |= ((int)in[--i] & (int)0xff) << bitcnt;
                bitcnt += 8;
            }
            out[o] = digits[bitbuf & mask];
            bitbuf >>= shift;
            bitcnt -= shift;
        }
        return out;
    }
    
    // The following two methods, used in conjunction, provide the
    // equivalent of String#trim()
    public static final int skipLeadingWhitespace(byte[] bytes){
        int length = bytes.length;
        int start = 0;
        for ( ; start < length && bytes[start] <= ' '; start++) ;
        return start;
    }
    public static final int skipTrailingWhitespace(byte[] bytes) {
        int stop = bytes.length - 1;
        for ( ; stop >= 0 && bytes[stop] <= ' '; stop-- ) ;
        return stop + 1;
    }
    /**
     * Trims whitespace (any bytes <= 0x20) from the beginning and end
     * of the array. This is equivalent to String#trim for byte arrays. If 
     * no bytes are trimmed, the original array is returned.
     * 
     * @param bytes the array to be trimmed
     * @return the trimmed array if trimming performed, otherwise the original array
     */
    public static final byte[] trim (byte[] bytes) {
        if (bytes.length == 0)
            return bytes;
        int start = skipLeadingWhitespace(bytes);
        if (start >= bytes.length) {
            return EMPTY_BYTES;
        }
        int stop = skipTrailingWhitespace(bytes);
        int length = stop - start;
        if (length == bytes.length)
            return bytes;
        byte[] trimmed = new byte[length];
        System.arraycopy(bytes,0,trimmed,0,length);
        return trimmed;
    }
    /**
     * Deletes the byte at the specified position, shifting all bytes
     * to the right of it left by one byte. If the copy flag is set,
     * a new array (one byte shorter) will be created and the original
     * will remain unchanged; otherwise, the last byte of the array is
     * set to zero.
     * 
     * @param bytes the array to 'delete' a byte from
     * @param pos the offset of the byte to delete
     * @param copy if true, a new copy of the array will be created, with 
     *        the original preserved
     */
    public static final byte[] delete(byte[] bytes, int pos, boolean copy) {
        int buflen = bytes.length;
        int newlen = buflen - 1;
        if (pos < 0 || pos > newlen) {
            throw new IllegalArgumentException("illegal position for delete");
        }
        int src = pos + 1;
        if (copy) {
            if (newlen == 0) {
                return EMPTY_BYTES;
            }
            byte[] newbytes = new byte[newlen];
            if (pos == 0) {
                System.arraycopy(bytes,1,newbytes,0,newlen);
            } else {
                System.arraycopy(bytes,0,newbytes,0,pos);
                System.arraycopy(bytes,src,newbytes,pos,newlen-pos);
            }
            return newbytes;
        } else {
            if (newlen > 0) {
                System.arraycopy(bytes,src,bytes,pos,buflen-src);
                bytes[newlen] = 0;
            } else {
                bytes[newlen-1] = 0;
            }
            return bytes;
        }
    }
    public static final byte[] delete(byte[] bytes, int pos, int length, boolean copy) {
        if (length < 0) {
            throw new IllegalArgumentException("illegal length for delete");
        }
        int buflen = bytes.length;
        if (length == 0 || buflen == 0 ) {
            return bytes;
        }
        int newlen = buflen - length;
        int newpos = pos + length;
        if (pos < 0 || newpos > buflen) {
            throw new IllegalArgumentException("illegal position for delete");
        }
        if (copy) {
            if (newlen == 0) {
                return EMPTY_BYTES;
            }
            byte[] newbytes = new byte[newlen];
            if (pos == 0) {
                System.arraycopy(bytes,length,newbytes,0,newlen);
            } else if (pos == newlen) {
                System.arraycopy(bytes,0,newbytes,0,newlen);
            } else {
                System.arraycopy(bytes,0,newbytes,0,pos);
                System.arraycopy(bytes,newpos,newbytes,pos,buflen-newpos);
            }
            return newbytes;
        } else {
            if (newlen > 0) {
                System.arraycopy(bytes,newpos,bytes,pos,buflen-newpos);
            }
            fill(bytes,newlen,buflen-newlen,(byte)0);
            return bytes;
        }
    }
    /**
     * Inserts a single byte at the specified position.  If copy is specified, creates
     * a new array one byte longer; otherwise shifts bytes in the existing array by one,
     * dropping the last byte.
     * 
     * @param bytes
     * @param pos
     * @param value
     * @param copy
     * @return new array if copy was specified, otherwise the original array
     */
    public static final byte[] insert(byte[] bytes, int pos, byte value, boolean copy) {
        int buflen = bytes.length;
        if (pos < 0 || pos > buflen) {
            throw new IllegalArgumentException("illegal position for insert");
        }
        if (copy) {
            byte[] newbytes = new  byte[buflen+1];
            if (pos == 0) {
                System.arraycopy(bytes,0,newbytes,1,buflen);
                newbytes[0] = value;
            } else if (pos == buflen) {
                System.arraycopy(bytes,0,newbytes,0,buflen);
                newbytes[buflen] = value;
            } else {
                System.arraycopy(bytes,0,newbytes,0,pos);
                System.arraycopy(bytes,pos,newbytes,pos+1,buflen-pos);
                newbytes[pos] = value;
            }
            return newbytes;
        } else {
            if (pos == buflen) {
                throw new IllegalArgumentException("illegal position for insert with no copy");
            }
            if (pos > buflen - 1) {
                System.arraycopy(bytes,pos,bytes,pos+1,buflen-pos-1);
            }
            bytes[pos] = value;
            return bytes;
        }
    }
    /**
     * Inserts the value array at the specified position. If copy is specified, creates a
     * new array, length == bytes.length + value.length.  Otherwise, displaces bytes in
     * the exisiting array, shifting them right by value.length and dropping value.length
     * bytes from the end of the array.
     * 
     * @param bytes
     * @param pos
     * @param value
     * @param copy
     * @return new array if copy was specified, otherwise the original array
     */
    public static final byte[] insert(byte[] bytes, int pos, byte[] value, boolean copy) {
        int buflen = bytes.length;
        if (pos < 0 || pos > buflen) {
            throw new IllegalArgumentException("illegal position for insert");
        }
        int vlen = value.length;
        if (copy) {
            int newlen = buflen + vlen;
            byte[] newbytes = new byte[newlen];
            if (pos == 0) {
                System.arraycopy(value,0,newbytes,0,vlen);
                System.arraycopy(bytes,0,newbytes,vlen,buflen);
            } else if (pos == buflen) { 
                System.arraycopy(bytes,0,newbytes,0,buflen);
                System.arraycopy(value,0,newbytes,buflen,vlen);
            } else {
                System.arraycopy(bytes,0,newbytes,0,pos);
                System.arraycopy(value,0,newbytes,pos,vlen);
                System.arraycopy(bytes,pos,newbytes,pos+vlen,buflen-pos);
            }
            return newbytes;
        } else {
            int displace = pos + vlen;
            if (displace > buflen) {
                throw new IllegalArgumentException("inserted array won't fit in target array");
            }
            if (pos == 0) {
                System.arraycopy(bytes,0,bytes,vlen,buflen-vlen);
                System.arraycopy(value,0,bytes,0,vlen);
            } else if (displace == buflen) {
                System.arraycopy(value,0,bytes,pos,vlen);
            } else {
                System.arraycopy(bytes,pos,bytes,displace,buflen-displace);
                System.arraycopy(value,0,bytes,pos,vlen);
            }
            return bytes;
        }
        
    }
    public static final byte[] append(byte[] bytes, byte value) {
        int buflen = bytes.length;
        byte[] newbytes = new byte[buflen + 1];
        System.arraycopy(bytes,0,newbytes,0,buflen);
        bytes[buflen] = value;
        return bytes;
    }
    /**
     * Fills the array with the specified value, starting at the specified position,
     * for the specified length. No exception is thrown if length is too big; in that 
     * case the array will be filled to the end.
     *  
     * @param bytes
     * @param pos
     * @param length
     * @param value
     * @return
     */
    public static final byte[] fill(byte[] bytes, int pos, int length, byte value) {
        if (length < 0) {
            throw new IllegalArgumentException("illegal length for fill");
        }
        int buflen = bytes.length;
        int stop = pos + length;
        if (stop > buflen)
            stop = buflen;
        for ( ; pos < stop; pos++) {
            bytes[pos] = value;
        }
        return bytes;
    }
    /**
     * Returns a copy of the array, or the array itelf if its length == 0.
     * 
     * @param bytes
     * @return
     */
    public static final byte[] copy(byte[] bytes) {
        int buflen = bytes.length;
        if (buflen == 0)
            return bytes;
        byte[] newbytes = new byte[buflen];
        System.arraycopy(bytes,0,newbytes,0,buflen);
        return newbytes;
    }

    private static final long[] LONG_10_POWERS = {
      1L,
      10L,
      100L,
      1000L,
      10000L,
      100000L,
      1000000L,
      10000000L,
      100000000L,
      1000000000L,
      10000000000L,
      100000000000L,
      1000000000000L,
      10000000000000L,
      100000000000000L,
      1000000000000000L,
      10000000000000000L,
      100000000000000000L
    };
    
    private static final Long LONG_ZERO = new Long(0);
    
    private static final BigInteger BIG_INT_ZERO = BigInteger.valueOf(0L);
    
    private static final byte[] EMPTY_BYTES = {};
    
    private static final byte[] MIN_INT_BYTE_ARRAY = {
        '-','2','1','4','7','4','8','3','6','4','8'
        };
    private static final byte[] MIN_LONG_BYTE_ARRAY = {
        '-','9','2','2','3','3','7','2','0','3','6','8','5','4','7','7','5','8','0','8'
        };
    private static final char[] MIN_LONG_CHAR_ARRAY = {
        '-','9','2','2','3','3','7','2','0','3','6','8','5','4','7','7','5','8','0','8'
        };
    // Tables from java.lang.Integer, converted to byte (used in java.lang.Long as well)
    private static final int[] SIZE_TABLE = { 9, 99, 999, 9999, 99999, 999999, 9999999,
        99999999, 999999999, Integer.MAX_VALUE };

    private static final byte[] DIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };

    private static final byte[] UCDIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'A' , 'B' ,
        'C' , 'D' , 'E' , 'F' , 'G' , 'H' ,
        'I' , 'J' , 'K' , 'L' , 'M' , 'N' ,
        'O' , 'P' , 'Q' , 'R' , 'S' , 'T' ,
        'U' , 'V' , 'W' , 'X' , 'Y' , 'Z'
        };
    private static final byte[] DIGIT_TENS = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        } ; 

    private static final byte[] DIGIT_ONES = { 
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        } ;

    
    private static final char[] CDIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };
    private static final char [] CDIGIT_TENS = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        } ; 

    private static final char [] CDIGIT_ONES = { 
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        } ;
    /*
     * All the positive powers of 10 that can be
     * represented exactly in double/float.
     * (From sun.misc.FloatingDecimal.java)
     */
    private static final double[] SMALL_10_POWERS = {
        1.0e0,
        1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5,
        1.0e6, 1.0e7, 1.0e8, 1.0e9, 1.0e10,
        1.0e11, 1.0e12, 1.0e13, 1.0e14, 1.0e15,
        1.0e16, 1.0e17, 1.0e18, 1.0e19, 1.0e20,
        1.0e21, 1.0e22
    };
    private static final int MAX_SMALL_10 = SMALL_10_POWERS.length - 1;
    private static final int  MAX_DECIMAL_DIGITS = 15;
    private static final int  BIG_DECIMAL_EXPONENT = 324;
    private static final int  INT_DECIMAL_DIGITS = 9;

}
