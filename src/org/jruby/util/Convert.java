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
 * Primitive conversions adapted from java.lang.Integer/Long (C) Sun Microsystems, Inc.
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
            buf[charPos--] = digits[-(i % radix)];
            i = i / radix;
        }
        buf[charPos] = digits[-i];
        if (negative) {
            buf[--charPos] = '-';
        }
        return new ByteList(buf, charPos, (33 - charPos));
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
            buf[charPos--] = digits[(int)(-(i % radix))];
            i = i / radix;
        }
        buf[charPos] = digits[(int)(-i)];
        if (negative) { 
            buf[--charPos] = '-';
        }
        return new ByteList(buf, charPos, (65 - charPos));
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
//        if (i == Long.MIN_VALUE)
//            return (byte[])MIN_LONG_BYTE_ARRAY.clone();
        int size = (i < 0) ? arraySize(-i) + 1 : arraySize(i);
        byte[] buf = new byte[size];
        getCharBytes(i, size, buf);
        return buf;
    }
    public static final char[] longToChars(long i) {
//        if (i == Long.MIN_VALUE)
//            return (byte[])MIN_LONG_BYTE_ARRAY.clone();
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
            buf [--charPos] = DigitOnes[r];
            buf [--charPos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) { 
            q = (i * 52429) >>> (16+3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf [--charPos] = digits[r];
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
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = digits[r];
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
            buf[--charPos] = cDigitOnes[r];
            buf[--charPos] = cDigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = cDigitOnes[r];
            buf[--charPos] = cDigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = cdigits[r];
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
            if (x <= sizeTable[i])
                return i+1;
    }
    // the following group of conversions to binary/octal/hex
    // is mostly for use by the new sprintf code
    public static final byte[] intToBinaryBytes(int i) {
        return intToUnsignedBytes(i, 1);
    }
    public static final byte[] intToOctalBytes(int i) {
        return intToUnsignedBytes(i, 3);
    }
    public static final byte[] intToHexBytes(int i) {
        return intToUnsignedBytes(i, 4);
    }

    public static final ByteList intToBinaryByteList(int i) {
        return new ByteList(intToUnsignedBytes(i, 1));
    }
    public static final ByteList intToOctalByteList(int i) {
        return new ByteList(intToUnsignedBytes(i, 3));
    }
    public static final ByteList intToHexByteList(int i) {
        return new ByteList(intToUnsignedBytes(i, 4));
    }

    public static final byte[] longToBinaryBytes(long i) {
        return longToUnsignedBytes(i, 1);
    }
    public static final byte[] longToOctalBytes(long i) {
        return longToUnsignedBytes(i, 3);
    }
    public static final byte[] longToHexBytes(long i) {
        return longToUnsignedBytes(i, 4);
    }

    public static final ByteList longToBinaryByteList(long i) {
        return new ByteList(longToUnsignedBytes(i, 1));
    }
    public static final ByteList longToOctalByteList(long i) {
        return new ByteList(longToUnsignedBytes(i, 3));
    }
    public static final ByteList longToHexByteList(long i) {
        return new ByteList(longToUnsignedBytes(i, 4));
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
            buf[--charPos] = digits[i & mask];
            i >>>= shift;
        } while (i != 0);
        return buf;
    }
    /**
     * Convert the integer to an unsigned number.
     * The result array is sized to fit the actual character length.
     */
    public static final byte[] intToUnsignedBytes(int i, int shift) {
        byte[] buf = new byte[32];
        int charPos = 32;
        int radix = 1 << shift;
        int mask = radix - 1;
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
            buf[--charPos] = digits[(int)(i & mask)];
            i >>>= shift;
        } while (i != 0);
        return buf;
    }
    /**
     * Convert the long to an unsigned number.
     * The result array is sized to fit the actual character length.
     */
    public static final byte[] longToUnsignedBytes(long i, int shift) {
        byte[] buf = new byte[64];
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
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
    private static final Long LONG_ZERO = new Long(0);
    
    private static final BigInteger BIG_INT_ZERO = BigInteger.valueOf(0L);
    
    private static final byte[] EMPTY_BYTES = {};
    
    private static final byte[] MIN_INT_BYTE_ARRAY = {
        '-','2','1','4','7','4','8','3','6','4','8'
        };
    private static final byte[] MIN_LONG_BYTE_ARRAY = {
        '-','9','2','2','3','3','7','2','0','3','6','8','5','4','7','7','5','8','0','8'
        };
    // Tables from java.lang.Integer, converted to byte (used in java.lang.Long as well)
    private static final int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999,
        99999999, 999999999, Integer.MAX_VALUE };

    private static final byte[] digits = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };

    private static final byte[] DigitTens = {
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

    private static final byte[] DigitOnes = { 
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

    
    private static final char[] cdigits = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };
    private static final char [] cDigitTens = {
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

    private static final char [] cDigitOnes = { 
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

}
