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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Derek Berner <derek.berner@state.nm.us>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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

import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.util.ByteList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Pack {
    private static final byte[] sSp10 = "          ".getBytes();
    private static final byte[] sNil10 = "\000\000\000\000\000\000\000\000\000\000".getBytes();
    private static final int IS_STAR = -1;
    private static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    private static final USASCIIEncoding USASCII = USASCIIEncoding.INSTANCE;
    private static final UTF8Encoding UTF8 = UTF8Encoding.INSTANCE;
    /** Native pack type.
     **/
    private static final String NATIVE_CODES = "sSiIlL";
    private static final String MAPPED_CODES = "sSiIqQ";
    
    private static final char BE = '>' - 1; // 61, only 1 char "free" b/w q and s
    private static final char LE = '<'; // 60
    private static final String ENDIANESS_CODES = new String(new char[] {
            's' + BE, 'S' + BE/*n*/, 'i' + BE, 'I' + BE, 'l' + BE, 'L' + BE/*N*/, 'q' + BE, 'Q' + BE,
            's' + LE, 'S' + LE/*v*/, 'i' + LE, 'I' + LE, 'l' + LE, 'L' + LE/*V*/, 'q' + LE, 'Q' + LE});
    private static final String UNPACK_IGNORE_NULL_CODES = "cC";
    private static final String PACK_IGNORE_NULL_CODES = "cCiIlLnNqQsSvV";
    private static final String PACK_IGNORE_NULL_CODES_WITH_MODIFIERS = "lLsS";
    private static final String sTooFew = "too few arguments";
    private static final byte[] uu_table;
    private static final byte[] b64_table;
    public static final byte[] sHexDigits;
    public static final int[] b64_xtable = new int[256];

    static {
        uu_table =
                ByteList.plain("`!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_");
        b64_table =
                ByteList.plain("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
        sHexDigits = ByteList.plain("0123456789abcdef0123456789ABCDEFx");

        // b64_xtable for decoding Base 64
        for (int i = 0; i < 256; i++) {
            b64_xtable[i] = -1;
        }
        for (int i = 0; i < 64; i++) {
            b64_xtable[(int) b64_table[i]] = i;
        }
    }

    public static int unpackInt_i(ByteBuffer enc) {
        int value;
        if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {
            value = decodeIntBigEndian(enc);
        } else {
            value = decodeIntLittleEndian(enc);
        }
        return value;
    }

    public static ByteList packInt_i(ByteList result, int s) {
        if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {
            encodeIntBigEndian(result, s);
        } else {
            encodeIntLittleEndian(result, s);
        }
        return result;
    }

    public static void encodeUM(Object runtime, ByteList lCurElemString, int occurrences, boolean ignoreStar, char type, ByteList result) {
        if (occurrences == 0 && type == 'm' && !ignoreStar) {
            encodes(runtime, result, lCurElemString.getUnsafeBytes(),
                    lCurElemString.getBegin(), lCurElemString.length(),
                    lCurElemString.length(), (byte) type, false);
            return;
        }

        occurrences = occurrences <= 2 ? 45 : occurrences / 3 * 3;
        if (lCurElemString.length() == 0) return;

        byte[] charsToEncode = lCurElemString.getUnsafeBytes();
        for (int i = 0; i < lCurElemString.length(); i += occurrences) {
            encodes(runtime, result, charsToEncode,
                    i + lCurElemString.getBegin(), lCurElemString.length() - i,
                    occurrences, (byte)type, true);
        }
    }

    private static ByteList encodes(Object runtime, ByteList io2Append,byte[]charsToEncode, int startIndex, int length, int charCount, byte encodingType, boolean tailLf) {
        charCount = charCount < length ? charCount : length;

        io2Append.ensure(charCount * 4 / 3 + 6);
        int i = startIndex;
        byte[] lTranslationTable = encodingType == 'u' ? uu_table : b64_table;
        byte lPadding;
        if (encodingType == 'u') {
            if (charCount >= lTranslationTable.length) {
                //throw runtime.newArgumentError(charCount
                //    + " is not a correct value for the number of bytes per line in a u directive.  Correct values range from 0 to "
                //    + lTranslationTable.length);
                throw new UnsupportedOperationException();
            }
            io2Append.append(lTranslationTable[charCount]);
            lPadding = '`';
        } else {
            lPadding = '=';
        }
        while (charCount >= 3) {
            byte lCurChar = charsToEncode[i++];
            byte lNextChar = charsToEncode[i++];
            byte lNextNextChar = charsToEncode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >>> 4) & 017))]);
            io2Append.append(lTranslationTable[077 & (((lNextChar << 2) & 074) | ((lNextNextChar >>> 6) & 03))]);
            io2Append.append(lTranslationTable[077 & lNextNextChar]);
            charCount -= 3;
        }
        if (charCount == 2) {
            byte lCurChar = charsToEncode[i++];
            byte lNextChar = charsToEncode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >> 4) & 017))]);
            io2Append.append(lTranslationTable[077 & (((lNextChar << 2) & 074) | (('\0' >> 6) & 03))]);
            io2Append.append(lPadding);
        } else if (charCount == 1) {
            byte lCurChar = charsToEncode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | (('\0' >>> 4) & 017))]);
            io2Append.append(lPadding);
            io2Append.append(lPadding);
        }
        if (tailLf) {
            io2Append.append('\n');
        }
        return io2Append;
    }

    /** rb_uv_to_utf8
     *
     */
    public static int utf8Decode(Object runtime, byte[]to, int p, int code) {
        if (code <= 0x7f) {
            to[p] = (byte)code;
            return 1;
        }
        if (code <= 0x7ff) {
            to[p + 0] = (byte)(((code >>> 6) & 0xff) | 0xc0);
            to[p + 1] = (byte)((code & 0x3f) | 0x80);
            return 2;
        }
        if (code <= 0xffff) {
            to[p + 0] = (byte)(((code >>> 12) & 0xff) | 0xe0);
            to[p + 1] = (byte)(((code >>> 6) & 0x3f) | 0x80);
            to[p + 2] = (byte)((code & 0x3f) | 0x80);
            return 3;
        }
        if (code <= 0x1fffff) {
            to[p + 0] = (byte)(((code >>> 18) & 0xff) | 0xf0);
            to[p + 1] = (byte)(((code >>> 12) & 0x3f) | 0x80);
            to[p + 2] = (byte)(((code >>> 6) & 0x3f) | 0x80);
            to[p + 3] = (byte)((code & 0x3f) | 0x80);
            return 4;
        }
        if (code <= 0x3ffffff) {
            to[p + 0] = (byte)(((code >>> 24) & 0xff) | 0xf8);
            to[p + 1] = (byte)(((code >>> 18) & 0x3f) | 0x80);
            to[p + 2] = (byte)(((code >>> 12) & 0x3f) | 0x80);
            to[p + 3] = (byte)(((code >>> 6) & 0x3f) | 0x80);
            to[p + 4] = (byte)((code & 0x3f) | 0x80);
            return 5;
        }
        if (code <= 0x7fffffff) {
            to[p + 0] = (byte)(((code >>> 30) & 0xff) | 0xfc);
            to[p + 1] = (byte)(((code >>> 24) & 0x3f) | 0x80);
            to[p + 2] = (byte)(((code >>> 18) & 0x3f) | 0x80);
            to[p + 3] = (byte)(((code >>> 12) & 0x3f) | 0x80);
            to[p + 4] = (byte)(((code >>> 6) & 0x3f) | 0x80);
            to[p + 5] = (byte)((code & 0x3f) | 0x80);
            return 6;
        }
        throw new UnsupportedOperationException();
    }

    /** utf8_to_uv
     */
    private static int utf8Decode(ByteBuffer buffer) {
        int c = buffer.get() & 0xFF;
        int uv = c;
        int n;

        if ((c & 0x80) == 0) {
            return c;
        }

        if ((c & 0x40) == 0) {
            throw new IllegalArgumentException("malformed UTF-8 character");
        }
        
      if      ((uv & 0x20) == 0) { n = 2; uv &= 0x1f; }
      else if ((uv & 0x10) == 0) { n = 3; uv &= 0x0f; }
      else if ((uv & 0x08) == 0) { n = 4; uv &= 0x07; }
      else if ((uv & 0x04) == 0) { n = 5; uv &= 0x03; }
      else if ((uv & 0x02) == 0) { n = 6; uv &= 0x01; }
      else {
          throw new IllegalArgumentException("malformed UTF-8 character");
      }
      if (n > buffer.remaining() + 1) {
          throw new IllegalArgumentException(
                  "malformed UTF-8 character (expected " + n + " bytes, "
                  + "given " + (buffer.remaining() + 1)  + " bytes)");
      }

      int limit = n - 1;

      n--;

      if (n != 0) {
          while (n-- != 0) {
              c = buffer.get() & 0xff;
              if ((c & 0xc0) != 0x80) {
                  throw new IllegalArgumentException("malformed UTF-8 character");
              }
              else {
                  c &= 0x3f;
                  uv = uv << 6 | c;
              }
          }
      }

      if (uv < utf8_limits[limit]) {
          throw new IllegalArgumentException("redundant UTF-8 sequence");
      }

      return uv;
    }

    private static final long utf8_limits[] = {
        0x0,                        /* 1 */
        0x80,                       /* 2 */
        0x800,                      /* 3 */
        0x10000,                    /* 4 */
        0x200000,                   /* 5 */
        0x4000000,                  /* 6 */
        0x80000000,                 /* 7 */
    };

    public static int safeGet(ByteBuffer encode) {
        while (encode.hasRemaining()) {
            int got = encode.get() & 0xff;
            
            if (got != 0) return got;
        }
        
        return 0;
    }

    private static int safeGetIgnoreNull(ByteBuffer encode) {
        int next = 0;
        while (encode.hasRemaining() && next == 0) {
            next = safeGet(encode);
        }
        return next;
    }

    /**
     * shrinks a stringbuffer.
     * shrinks a stringbuffer by a number of characters.
     * @param i2Shrink the stringbuffer
     * @param iLength how much to shrink
     * @return the stringbuffer
     **/
    private static final ByteList shrink(ByteList i2Shrink, int iLength) {
        iLength = i2Shrink.length() - iLength;

        if (iLength < 0) {
            throw new IllegalArgumentException();
        }
        i2Shrink.length(iLength);
        return i2Shrink;
    }

    /**
     * grows a stringbuffer.
     * uses the Strings to pad the buffer for a certain length
     * @param i2Grow the buffer to grow
     * @param iPads the string used as padding
     * @param iLength how much padding is needed
     * @return the padded buffer
     **/
    private static final ByteList grow(ByteList i2Grow, byte[]iPads, int iLength) {
        int lPadLength = iPads.length;
        while (iLength >= lPadLength) {
            i2Grow.append(iPads);
            iLength -= lPadLength;
        }
        i2Grow.append(iPads, 0, iLength);
        return i2Grow;
    }

    /**
     * Retrieve an encoded int in little endian starting at index in the
     * string value.
     *
     * @param encode string to get int from
     * @return the decoded integer
     */
    private static int decodeIntLittleEndian(ByteBuffer encode) {
        encode.order(ByteOrder.LITTLE_ENDIAN);
        int value = encode.getInt();
        encode.order(ByteOrder.BIG_ENDIAN);
        return value;
    }

    /**
     * Retrieve an encoded int in little endian starting at index in the
     * string value.
     *
     * @param encode string to get int from
     * @return the decoded integer
     */
    private static int decodeIntBigEndian(ByteBuffer encode) {
        return encode.getInt();
    }

    /**
     * Retrieve an encoded int in big endian starting at index in the string
     * value.
     *
     * @param encode string to get int from
     * @return the decoded integer
     */
    private static long decodeIntUnsignedBigEndian(ByteBuffer encode) {
        return (long)encode.getInt() & 0xFFFFFFFFL;
    }

    /**
     * Retrieve an encoded int in little endian starting at index in the
     * string value.
     *
     * @param encode the encoded string
     * @return the decoded integer
     */
    private static long decodeIntUnsignedLittleEndian(ByteBuffer encode) {
        encode.order(ByteOrder.LITTLE_ENDIAN);
        long value = encode.getInt() & 0xFFFFFFFFL;
        encode.order(ByteOrder.BIG_ENDIAN);
        return value;
    }

    /**
     * Encode an int in little endian format into a packed representation.
     *
     * @param result to be appended to
     * @param s the integer to encode
     */
    private static void encodeIntLittleEndian(ByteList result, int s) {
        result.append((byte) (s & 0xff)).append((byte) ((s >> 8) & 0xff));
        result.append((byte) ((s>>16) & 0xff)).append((byte) ((s >> 24) & 0xff));
    }

    /**
     * Encode an int in big-endian format into a packed representation.
     *
     * @param result to be appended to
     * @param s the integer to encode
     */
    private static void encodeIntBigEndian(ByteList result, int s) {
        result.append((byte) ((s>>24) &0xff)).append((byte) ((s>>16) &0xff));
        result.append((byte) ((s >> 8) & 0xff)).append((byte) (s & 0xff));
    }

    /**
     * Decode a long in big-endian format from a packed value
     *
     * @param encode string to get int from
     * @return the long value
     */
    private static long decodeLongBigEndian(ByteBuffer encode) {
        int c1 = decodeIntBigEndian(encode);
        int c2 = decodeIntBigEndian(encode);

        return ((long) c1 << 32) + (c2 & 0xffffffffL);
    }

    /**
     * Decode a long in little-endian format from a packed value
     *
     * @param encode string to get int from
     * @return the long value
     */
    private static long decodeLongLittleEndian(ByteBuffer encode) {
        int c1 = decodeIntLittleEndian(encode);
        int c2 = decodeIntLittleEndian(encode);

        return ((long) c2 << 32) + (c1 & 0xffffffffL);
    }

    /**
     * Encode a long in little-endian format into a packed value
     *
     * @param result to pack long into
     * @param l is the long to encode
     */
    private static void encodeLongLittleEndian(ByteList result, long l) {
        encodeIntLittleEndian(result, (int) (l & 0xffffffff));
        encodeIntLittleEndian(result, (int) (l >>> 32));
    }

    /**
     * Encode a long in big-endian format into a packed value
     *
     * @param result to pack long into
     * @param l is the long to encode
     */
    private static void encodeLongBigEndian(ByteList result, long l) {
        encodeIntBigEndian(result, (int) (l >>> 32));
        encodeIntBigEndian(result, (int) (l & 0xffffffff));
    }

    /**
     * Decode a double from a packed value
     *
     * @param encode string to get int from
     * @return the double value
     */
    private static double decodeDoubleLittleEndian(ByteBuffer encode) {
        return Double.longBitsToDouble(decodeLongLittleEndian(encode));
    }

    /**
     * Decode a double in big-endian from a packed value
     *
     * @param encode string to get int from
     * @return the double value
     */
    private static double decodeDoubleBigEndian(ByteBuffer encode) {
        return Double.longBitsToDouble(decodeLongBigEndian(encode));
    }

    /**
     * Encode a double in little endian format into a packed value
     *
     * @param result to pack double into
     * @param d is the double to encode
     */
    private static void encodeDoubleLittleEndian(ByteList result, double d) {
        encodeLongLittleEndian(result, Double.doubleToRawLongBits(d));
    }

    /**
     * Encode a double in big-endian format into a packed value
     *
     * @param result to pack double into
     * @param d is the double to encode
     */
    private static void encodeDoubleBigEndian(ByteList result, double d) {
        encodeLongBigEndian(result, Double.doubleToRawLongBits(d));
    }

    /**
     * Decode a float in big-endian from a packed value
     *
     * @param encode string to get int from
     * @return the double value
     */
    private static float decodeFloatBigEndian(ByteBuffer encode) {
        return Float.intBitsToFloat(decodeIntBigEndian(encode));
    }

    /**
     * Decode a float in little-endian from a packed value
     *
     * @param encode string to get int from
     * @return the double value
     */
    private static float decodeFloatLittleEndian(ByteBuffer encode) {
        return Float.intBitsToFloat(decodeIntLittleEndian(encode));
    }

    /**
     * Encode a float in little endian format into a packed value
     * @param result to pack float into
     * @param f is the float to encode
     */
    private static void encodeFloatLittleEndian(ByteList result, float f) {
        encodeIntLittleEndian(result, Float.floatToRawIntBits(f));
    }

    /**
     * Encode a float in big-endian format into a packed value
     * @param result to pack float into
     * @param f is the float to encode
     */
    private static void encodeFloatBigEndian(ByteList result, float f) {
        encodeIntBigEndian(result, Float.floatToRawIntBits(f));
    }

    /**
     * Decode a short in little-endian from a packed value
     *
     * @param encode string to get int from
     * @return the short value
     */
    private static int decodeShortUnsignedLittleEndian(ByteBuffer encode) {
        encode.order(ByteOrder.LITTLE_ENDIAN);
        int value = encode.getShort() & 0xFFFF;
        encode.order(ByteOrder.BIG_ENDIAN);
        return value;
    }

    /**
     * Decode a short in big-endian from a packed value
     *
     * @param encode string to get int from
     * @return the short value
     */
    private static int decodeShortUnsignedBigEndian(ByteBuffer encode) {
        int value = encode.getShort() & 0xFFFF;
        return value;
    }

    /**
     * Decode a short in little-endian from a packed value
     *
     * @param encode string to get int from
     * @return the short value
     */
    private static int decodeShortLittleEndian(ByteBuffer encode) {
        encode.order(ByteOrder.LITTLE_ENDIAN);
        int value = encode.getShort();
        encode.order(ByteOrder.BIG_ENDIAN);
        return value;
    }

    /**
     * Decode a short in big-endian from a packed value
     *
     * @param encode string to get int from
     * @return the short value
     */
    private static short decodeShortBigEndian(ByteBuffer encode) {
        return encode.getShort();
    }

    /**
     * Encode an short in little endian format into a packed representation.
     *
     * @param result to be appended to
     * @param s the short to encode
     */
    private static void encodeShortLittleEndian(ByteList result, int s) {
        result.append((byte) (s & 0xff)).append((byte) ((s & 0xff00) >> 8));
    }

    /**
     * Encode an shortin big-endian format into a packed representation.
     *
     * @param result to be appended to
     * @param s the short to encode
     */
    private static void encodeShortBigEndian(ByteList result, int s) {
        result.append((byte) ((s & 0xff00) >> 8)).append((byte) (s & 0xff));
    }

}
