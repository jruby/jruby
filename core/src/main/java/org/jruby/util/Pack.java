/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

package org.jruby.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.*;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.EncodingUtils;

import static com.headius.backport9.buffer.Buffers.markBuffer;
import static com.headius.backport9.buffer.Buffers.positionBuffer;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.*;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.TypeConverter.toFloat;

public class Pack {
    private static final byte[] sSp10 = "          ".getBytes();
    private static final byte[] sNil10 = "\000\000\000\000\000\000\000\000\000\000".getBytes();
    private static final int IS_STAR = -1;
    private static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    private static final USASCIIEncoding USASCII = USASCIIEncoding.INSTANCE;
    private static final UTF8Encoding UTF8 = UTF8Encoding.INSTANCE;
    /** Native pack type.
     **/
    private static final String NATIVE_CODES = "sSiIlLjJ";
    private static final String MAPPED_CODES = "sSiIqQjJ";
    
    private static final char BE = '>' + 127; // 189, bumped up to avoid collisions with LE
    private static final char LE = '<'; // 60
    private static final String ENDIANESS_CODES = new String(new char[] {
            's' + BE, 'S' + BE/*n*/, 'i' + BE, 'I' + BE, 'l' + BE, 'L' + BE/*N*/, 'q' + BE, 'Q' + BE, 'j' + BE, 'J' + BE,
            's' + LE, 'S' + LE/*v*/, 'i' + LE, 'I' + LE, 'l' + LE, 'L' + LE/*V*/, 'q' + LE, 'Q' + LE, 'j' + LE, 'J' + LE});

    /** Unpack modes
    **/
    private static final int UNPACK_ARRAY = 0;
    private static final int UNPACK_BLOCK = 1;
    private static final int UNPACK_1 = 2;

    private static final String sTooFew = "too few arguments";
    private static final byte[] uu_table;
    private static final byte[] b64_table;
    public static final byte[] sHexDigits;
    public static final int[] b64_xtable = new int[256];
    private static final Converter[] converters = new Converter[512];

    private static long num2quad(IRubyObject arg) {
        if (arg.isNil()) return 0L;
        if (arg instanceof RubyBignum) {
            BigInteger big = ((RubyBignum) arg).getValue();
            return big.longValue();
        }
        return RubyNumeric.num2long(arg);
    }

    private static float obj2flt(ThreadContext context, IRubyObject o) {
        return (float) toFloat(context.runtime, o).asDouble(context);
    }

    private static double obj2dbl(ThreadContext context, IRubyObject o) {
        return toFloat(context.runtime, o).asDouble(context);
    }

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
            b64_xtable[(int)b64_table[i]] = i;
        }

        // single precision, little-endian
        converters['e'] = new Converter(4) {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFloat(context, decodeFloatLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeFloatLittleEndian(result, obj2flt(context, o));
            }
        };
        // single precision, big-endian
        converters['g'] = new Converter(4) {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFloat(context, decodeFloatBigEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeFloatBigEndian(result, obj2flt(context, o));
            }
        };
        // single precision, native
        Converter tmp = new Converter(4) {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFloat(context, Platform.BYTE_ORDER == Platform.BIG_ENDIAN ?
                        decodeFloatBigEndian(enc) : decodeFloatLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result) {
                var value = obj2flt(context, o);
                if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {                
                    encodeFloatBigEndian(result, value);
                } else {
                    encodeFloatLittleEndian(result, value);
                }
            }
        };
        converters['F'] = tmp; // single precision, native
        converters['f'] = tmp; // single precision, native

        // double precision, little-endian
        converters['E'] = new Converter(8) {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFloat(context, decodeDoubleLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeDoubleLittleEndian(result, obj2dbl(context, o));
            }               
        };
        // double precision, big-endian
        converters['G'] = new Converter(8) {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFloat(context, decodeDoubleBigEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeDoubleBigEndian(result, obj2dbl(context, o));
            }
        };
        // double precision, native
        tmp = new Converter(8) {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFloat(context, Platform.BYTE_ORDER == Platform.BIG_ENDIAN ?
                    decodeDoubleBigEndian(enc) : decodeDoubleLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeDoubleLittleEndian(result, obj2dbl(context, o));
            }
        };
        converters['D'] = tmp; // double precision, native
        converters['d'] = tmp; // double precision, native

        // signed short, little-endian
        tmp = new QuadConverter(2, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeShortUnsignedLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeShortLittleEndian(result, overflowQuad(num2quad(o)));
            }            
        };
        converters['v'] = tmp;
        converters['S' + LE] = tmp;
        // signed short, big-endian
        tmp = new QuadConverter(2, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeShortUnsignedBigEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result) {
                encodeShortBigEndian(result, overflowQuad(num2quad(o)));
            }
        };
        converters['n'] = tmp;
        converters['S' + BE] = tmp;
        // signed short, native
        converters['s'] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, Platform.BYTE_ORDER == Platform.BIG_ENDIAN ?
                        decodeShortBigEndian(enc) : decodeShortLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result) {
                encodeShortByByteOrder(result, overflowQuad(num2quad(o))); // XXX: 0xffff0000 on BE?
            }
        };
        // unsigned short, native
        converters['S'] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, Platform.BYTE_ORDER == Platform.BIG_ENDIAN ?
                    decodeShortUnsignedBigEndian(enc) : decodeShortUnsignedLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeShortByByteOrder(result, overflowQuad(num2quad(o)));
            }
        };
        // signed short, little endian
        converters['s' + LE] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeShortLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result) {
                encodeShortLittleEndian(result, overflowQuad(num2quad(o))); // XXX: 0xffff0000 on BE?
            }
        };
        // signed short, big endian
        converters['s' + BE] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeShortBigEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result) {
                encodeShortBigEndian(result, overflowQuad(num2quad(o))); // XXX: 0xffff0000 on BE?
            }
        };

        // signed char
        converters['c'] = new Converter(1, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                int c = enc.get();
                return asFixnum(context, c > (char) 127 ? c-256 : c);
            }

            public void encode(ThreadContext context, IRubyObject o, ByteList result) {
                byte c = (byte) (num2quad(o) & 0xff);
                result.append(c);
            }
        };
        // unsigned char
        converters['C'] = new Converter(1, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, enc.get() & 0xFF);
            }

            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                byte c = o == context.nil ? 0 : (byte) (num2quad(o) & 0xff);
                result.append(c);
            }
        };

        // unsigned long, little-endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeIntUnsignedLittleEndian(enc));
            }
            
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeIntLittleEndian(result, (int) RubyNumeric.num2long(o));
            }
        };
        converters['V'] = tmp;
        converters['L' + LE] = tmp;
        converters['I' + LE] = tmp;
        if (Platform.BIT_WIDTH == 32) converters['J' + LE] = tmp;

        // unsigned long, big-endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeIntUnsignedBigEndian(enc));
            }
            
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeIntBigEndian(result, (int) RubyNumeric.num2long(o));
            }
        };
        converters['N'] = tmp;
        converters['L' + BE] = tmp;
        converters['I' + BE] = tmp;
        if (Platform.BIT_WIDTH == 32) converters['J' + BE] = tmp;

        // unsigned int, native
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, Platform.BYTE_ORDER == Platform.BIG_ENDIAN ?
                        decodeIntUnsignedBigEndian(enc) : decodeIntUnsignedLittleEndian(enc));
            }
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                int s = o == context.nil ? 0 : (int) RubyNumeric.num2long(o);
                packInt_i(result, s);
            }
        };
        converters['I'] = tmp; // unsigned int, native
        converters['L'] = tmp; // unsigned long, native
        if (Platform.BIT_WIDTH == 32) converters['J'] = tmp; // unsigned long, native

        // int, native
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, unpackInt_i(enc));
            }
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                int s = o == context.nil ? 0 : (int)RubyNumeric.num2long(o);
                packInt_i(result, s);
            }
        };
        converters['i'] = tmp; // int, native
        converters['l'] = tmp; // long, native
        if (Platform.BIT_WIDTH == 32) converters['j'] = tmp; // long, native

        // int, little endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeIntLittleEndian(enc));
            }
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                int s = o == context.nil ? 0 : (int)RubyNumeric.num2long(o);
                encodeIntLittleEndian(result, s);
            }
        };
        converters['i' + LE] = tmp; // int, native
        converters['l' + LE] = tmp; // long, native
        if (Platform.BIT_WIDTH == 32) converters['j' + LE] = tmp; // long, native

        // int, big endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeIntBigEndian(enc));
            }
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                int s = o == context.nil ? 0 : (int)RubyNumeric.num2long(o);
                encodeIntBigEndian(result, s);
            }
        };
        converters['i' + BE] = tmp; // int, native
        converters['l' + BE] = tmp; // long, native
        if (Platform.BIT_WIDTH == 32) converters['j' + BE] = tmp; // long, native

        // 64-bit number, native (as bignum)
        tmp = new QuadConverter(8, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                long l = Platform.BYTE_ORDER == Platform.BIG_ENDIAN ? decodeLongBigEndian(enc) : decodeLongLittleEndian(enc);

                return RubyBignum.bignorm(context.runtime,BigInteger.valueOf(l).and(new BigInteger("FFFFFFFFFFFFFFFF", 16)));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeLongByByteOrder(result, num2quad(o));
            }
        };
        converters['Q'] = tmp;
        if (Platform.BIT_WIDTH == 64) converters['J'] = tmp;

        // 64-bit number, little endian (as bignum)
        tmp = new QuadConverter(8, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                long l = decodeLongLittleEndian(enc);
                return RubyBignum.bignorm(context.runtime,BigInteger.valueOf(l).and(new BigInteger("FFFFFFFFFFFFFFFF", 16)));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeLongLittleEndian(result, num2quad(o));
            }
        };
        converters['Q' + LE] = tmp;
        if (Platform.BIT_WIDTH == 64) converters['J' + LE] = tmp;

        // 64-bit number, big endian (as bignum)
        tmp = new QuadConverter(8, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                long l = decodeLongBigEndian(enc);
                return RubyBignum.bignorm(context.runtime,BigInteger.valueOf(l).and(new BigInteger("FFFFFFFFFFFFFFFF", 16)));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeLongBigEndian(result, num2quad(o));
            }
        };
        converters['Q' + BE] = tmp;
        if (Platform.BIT_WIDTH == 64) converters['J' + BE] = tmp;

        // 64-bit number, native (as fixnum)
        tmp = new QuadConverter(8, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, Platform.BYTE_ORDER == Platform.BIG_ENDIAN ?
                        decodeLongBigEndian(enc) : decodeLongLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeLongByByteOrder(result, num2quad(o));
            }
        };
        converters['q'] = tmp;
        if (Platform.BIT_WIDTH == 64) converters['j'] = tmp;

        // 64-bit number, little-endian (as fixnum)
        tmp = new QuadConverter(8, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeLongLittleEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeLongLittleEndian(result, num2quad(o));
            }
        };
        converters['q' + LE] = tmp;
        if (Platform.BIT_WIDTH == 64) converters['j' + LE] = tmp;

        // 64-bit number, big-endian (as fixnum)
        tmp = new QuadConverter(8, "Integer") {
            public IRubyObject decode(ThreadContext context, ByteBuffer enc) {
                return asFixnum(context, decodeLongBigEndian(enc));
            }

            @Override
            public void encode(ThreadContext context, IRubyObject o, ByteList result){
                encodeLongBigEndian(result, num2quad(o));
            }
        };
        converters['q' + BE] = tmp;
        if (Platform.BIT_WIDTH == 64) converters['j' + BE] = tmp;

        // pointer; we can't provide a real pointer, so we just use identity hashcode
        tmp = new QuadConverter(8) {
            @Override
            public IRubyObject decode(ThreadContext context, ByteBuffer format) {
                return context.nil;
            }

            @Override
            public void encode(ThreadContext context, IRubyObject from, ByteList result) {
                if (from.isNil()) {
                    encodeLongBigEndian(result, 0);
                } else {
                    encodeLongBigEndian(result, System.identityHashCode(from));
                }
            }
        };

        converters['p'] = tmp;

        // pointer; we can't provide a real pointer, so we just use identity hashcode
        tmp = new QuadConverter(8) {
            @Override
            public IRubyObject decode(ThreadContext context, ByteBuffer format) {
                return context.nil;
            }

            @Override
            public void encode(ThreadContext context, IRubyObject from, ByteList result) {
                if (from.isNil()) {
                    encodeLongBigEndian(result, 0);
                } else {
                    encodeLongBigEndian(result, System.identityHashCode(from.convertToString()));
                }
            }
        };

        converters['P'] = tmp;
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

    private static void encodeUM(ThreadContext context, ByteList lCurElemString, int occurrences, boolean ignoreStar, char type, ByteList result) {
        if (occurrences == 0 && type == 'm' && !ignoreStar) {
            encodes(context, result, lCurElemString.getUnsafeBytes(),
                    lCurElemString.getBegin(), lCurElemString.length(),
                    lCurElemString.length(), (byte) type, false);
            return;
        }

        occurrences = occurrences <= 2 ? 45 : occurrences / 3 * 3;
        if (lCurElemString.isEmpty()) return;

        byte[] charsToEncode = lCurElemString.getUnsafeBytes();
        for (int i = 0; i < lCurElemString.length(); i += occurrences) {
            encodes(context, result, charsToEncode,
                    i + lCurElemString.getBegin(), lCurElemString.length() - i,
                    occurrences, (byte)type, true);
        }
    }

    /**
     * encodes a String in base64 or its uuencode variant.
     * appends the result of the encoding in a StringBuffer
     * @param io2Append The StringBuffer which should receive the result
     * @param charsToEncode The String to encode
     * @param startIndex
     * @param length The max number of characters to encode
     * @param charCount
     * @param encodingType the type of encoding required (this is the same type as used by the pack method)
     * @param tailLf true if the traililng "\n" is needed
     * @return the io2Append buffer
     **/
    private static ByteList encodes(ThreadContext context, ByteList io2Append,byte[] charsToEncode, int startIndex,
                                    int length, int charCount, byte encodingType, boolean tailLf) {
        charCount = Math.min(charCount, length);

        io2Append.ensure(charCount * 4 / 3 + 6);
        int i = startIndex;
        byte[] lTranslationTable = encodingType == 'u' ? uu_table : b64_table;
        byte lPadding;
        if (encodingType == 'u') {
            if (charCount >= lTranslationTable.length) {
                throw argumentError(context, charCount
                    + " is not a correct value for the number of bytes per line in a u directive.  Correct values range from 0 to "
                    + lTranslationTable.length);
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

    public static RubyArray unpack(ThreadContext context, ByteList encodedString, ByteList formatString) {
        return unpackWithBlock(context, RubyString.newStringLight(context.runtime, encodedString), formatString, Block.NULL_BLOCK);
    }

    /**
     * @see Pack#unpackWithBlock(ThreadContext, RubyString, ByteList, Block)
     * @param context
     * @param encoded
     * @param formatString
     * @return unpacked array
     */
    public static RubyArray unpack(ThreadContext context, RubyString encoded, ByteList formatString) {
        return unpackWithBlock(context, encoded, formatString, Block.NULL_BLOCK);
    }

    /**
     *    Decodes <i>str</i> (which may contain binary data) according to the format
     *       string, returning an array of each value extracted.
     *       The format string consists of a sequence of single-character directives.<br>
     *       Each directive may be followed by a number, indicating the number of times to repeat with this directive.  An asterisk (``<code>*</code>'') will use up all
     *       remaining elements.  <br>
     *       Note that if passed a block, this method will return null and instead yield results to the block.
     *       The directives <code>sSiIlL</code> may each be followed by an underscore (``<code>_</code>'') to use the underlying platform's native size for the specified type; otherwise, it uses a platform-independent consistent size.  <br>
     *       Spaces are ignored in the format string.
     * 
     *       <table border="1"><caption style="display:none">layout table</caption>
     *           <tr>
     *             <td>
     * <P></P>
     *         <b>Directives for <a href="ref_c_string.html#String.unpack">
     *                   <code>String#unpack</code>
     *                 </a>
     *               </b>        <table class="codebox"><caption style="display:none">layout table</caption>
     * <tr>
     *   <td>
     *                     <b>Format</b>
     *                   </td>
     *   <td>
     *                     <b>Function</b>
     *                   </td>
     *   <td>
     *                     <b>Returns</b>
     *                   </td>
     * </tr>
     * <tr>
     *   <td>A</td>
     *   <td>String with trailing nulls and spaces removed.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>a</td>
     *   <td>String.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>B</td>
     *   <td>Extract bits from each character (msb first).</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>b</td>
     *   <td>Extract bits from each character (lsb first).</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>C</td>
     *   <td>Extract a character as an unsigned integer.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>c</td>
     *   <td>Extract a character as an integer.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>d</td>
     *   <td>Treat <em>sizeof(double)</em> characters as a native
     *           double.</td>
     *   <td>Float</td>
     * </tr>
     * <tr>
     *   <td>E</td>
     *   <td>Treat <em>sizeof(double)</em> characters as a double in
     *           little-endian byte order.</td>
     *   <td>Float</td>
     * </tr>
     * <tr>
     *   <td>e</td>
     *   <td>Treat <em>sizeof(float)</em> characters as a float in
     *           little-endian byte order.</td>
     *   <td>Float</td>
     * </tr>
     * <tr>
     *   <td>f</td>
     *   <td>Treat <em>sizeof(float)</em> characters as a native float.</td>
     *   <td>Float</td>
     * </tr>
     * <tr>
     *   <td>G</td>
     *   <td>Treat <em>sizeof(double)</em> characters as a double in
     *           network byte order.</td>
     *   <td>Float</td>
     * </tr>
     * <tr>
     *   <td>g</td>
     *   <td>Treat <em>sizeof(float)</em> characters as a float in
     *           network byte order.</td>
     *   <td>Float</td>
     * </tr>
     * <tr>
     *   <td>H</td>
     *   <td>Extract hex nibbles from each character (most
     *           significant first).</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>h</td>
     *   <td>Extract hex nibbles from each character (least
     *           significant first).</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>I</td>
     *   <td>Treat <em>sizeof(int)</em>
     *                     <sup>1</sup> successive
     *           characters as an unsigned native integer.</td>
     *   <td>Integer</td>
     * </tr>
     * <tr>
     *   <td>i</td>
     *   <td>Treat <em>sizeof(int)</em>
     *                     <sup>1</sup> successive
     *           characters as a signed native integer.</td>
     *   <td>Integer</td>
     * </tr>
     * <tr>
     *   <td>L</td>
     *   <td>Treat four<sup>1</sup> successive
     *           characters as an unsigned native
     *           long integer.</td>
     *   <td>Integer</td>
     * </tr>
     * <tr>
     *   <td>l</td>
     *   <td>Treat four<sup>1</sup> successive
     *           characters as a signed native
     *           long integer.</td>
     *   <td>Integer</td>
     * </tr>
     * <tr>
     *   <td>M</td>
     *   <td>Extract a quoted-printable string.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>m</td>
     *   <td>Extract a base64 encoded string.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>N</td>
     *   <td>Treat four characters as an unsigned long in network
     *           byte order.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>n</td>
     *   <td>Treat two characters as an unsigned short in network
     *           byte order.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>P</td>
     *   <td>Treat <em>sizeof(char *)</em> characters as a pointer, and
     *           return <em>len</em> characters from the referenced location.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>p</td>
     *   <td>Treat <em>sizeof(char *)</em> characters as a pointer to a
     *           null-terminated string.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>S</td>
     *   <td>Treat two<sup>1</sup> successive characters as an unsigned
     *           short in
     *           native byte order.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>s</td>
     *   <td>Treat two<sup>1</sup> successive
     *           characters as a signed short in
     *           native byte order.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>U</td>
     *   <td>Extract UTF-8 characters as unsigned integers.</td>
     *   <td>Integer</td>
     * </tr>
     * <tr>
     *   <td>u</td>
     *   <td>Extract a UU-encoded string.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>V</td>
     *   <td>Treat four characters as an unsigned long in little-endian
     *           byte order.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>v</td>
     *   <td>Treat two characters as an unsigned short in little-endian
     *           byte order.</td>
     *   <td>Fixnum</td>
     * </tr>
     * <tr>
     *   <td>X</td>
     *   <td>Skip backward one character.</td>
     *   <td>---</td>
     * </tr>
     * <tr>
     *   <td>x</td>
     *   <td>Skip forward one character.</td>
     *   <td>---</td>
     * </tr>
     * <tr>
     *   <td>Z</td>
     *   <td>String with trailing nulls removed.</td>
     *   <td>String</td>
     * </tr>
     * <tr>
     *   <td>@</td>
     *   <td>Skip to the offset given by the length argument.</td>
     *   <td>---</td>
     * </tr>
     * <tr>
     *                   <td colspan="9"><img alt="bullet" src="dot.gif" width="1" height="1"></td>
     *                 </tr>
     *               </table>
     * <P></P>
     *         <sup>1</sup>&nbsp;May be modified by appending ``_'' to the directive.
     * <P></P>
     *       </td>
     *           </tr>
     *         </table>
     *
     * @see RubyArray#pack
     **/
    public static RubyArray unpackWithBlock(ThreadContext context, RubyString encoded, ByteList formatString, Block block) {
        return (RubyArray) unpackInternal(context, encoded, formatString, block.isGiven() ? UNPACK_BLOCK : UNPACK_ARRAY, 0, block);
    }

    public static RubyArray unpackWithBlock(ThreadContext context, RubyString encoded, ByteList formatString, long offset, Block block) {
        return (RubyArray) unpackInternal(context, encoded, formatString, block.isGiven() ? UNPACK_BLOCK : UNPACK_ARRAY, offset, block);
    }

    private static RubyString unpackBase46Strict(ThreadContext context, ByteList input) {
        int index = 0; // current index of out
        int s = -1;
        int a = -1;
        int b = -1;
        int c = 0;

        byte[] buf = input.unsafeBytes();
        int begin = input.begin();
        int length = input.realSize();
        int end = begin + length;

        if (length % 4 != 0) throw argumentError(context, "invalid base64");

        int p = begin;
        byte[] out = new byte[3 * ((length + 3) / 4)];

        while (p < end && s != '=') {
            // obtain a
            s = buf[p++];
            a = b64_xtable[s];
            if (a == -1) throw argumentError(context, "invalid base64");

            // obtain b
            s = buf[p++];
            b = b64_xtable[s];
            if (b == -1) throw argumentError(context, "invalid base64");

            // obtain c
            s = buf[p++];
            c = b64_xtable[s];
            if (s == '=') {
                if (buf[p++] != '=') throw argumentError(context, "invalid base64");
                break;
            }
            if (c == -1) throw argumentError(context, "invalid base64");

            // obtain d
            s = buf[p++];
            int d = b64_xtable[s];
            if (s == '=') break;
            if (d == -1) throw argumentError(context, "invalid base64");

            // calculate based on a, b, c and d
            out[index++] = (byte) (a << 2 | b >> 4);
            out[index++] = (byte) (b << 4 | c >> 2);
            out[index++] = (byte) (c << 6 | d);
        }

        if (p < end) throw argumentError(context, "invalid base64");

        if (a != -1 && b != -1) {
            if (c == -1 && s == '=') {
                if ((b & 15) > 0) throw argumentError(context, "invalid base64");
                out[index++] = (byte)((a << 2 | b >> 4) & 255);
            } else if(c != -1 && s == '=') {
                if ((c & 3) > 0) throw argumentError(context, "invalid base64");
                out[index++] = (byte)((a << 2 | b >> 4) & 255);
                out[index++] = (byte)((b << 4 | c >> 2) & 255);
            }
        }
        return newString(context, new ByteList(out, 0, index));
    }

    public static IRubyObject unpack1WithBlock(ThreadContext context, RubyString encoded, ByteList formatString, Block block) {
        return unpack1WithBlock(context, encoded, formatString, 0, block);
    }

    public static IRubyObject unpack1WithBlock(ThreadContext context, RubyString encoded, ByteList formatString, long offset, Block block) {
        int formatLength = formatString.realSize();

        // Strict m0 is commmonly used in cookie handling so it has a fast path.
        if (formatLength >= 1) {
            byte first = (byte) (formatString.get(0) & 0xff);

            if (first == 'm') {
                if (formatLength == 2) {
                    byte second = (byte) (formatString.get(1) & 0xff);

                    if (second == '0') return unpackBase46Strict(context, encoded.getByteList());
                }
            }
        }

        return unpackInternal(context, encoded, formatString, UNPACK_1, offset, block);
    }

    private static IRubyObject unpackInternal(ThreadContext context, RubyString encoded, ByteList formatString, int mode, long offset, Block block) {
        final var result = mode == UNPACK_BLOCK || mode == UNPACK_1 ? null : newArray(context);
        final ByteList encodedString = encoded.getByteList();
        int len = encodedString.realSize();
        int beg = encodedString.begin();

        if (offset < 0) throw argumentError(context, "offset can't be negative");
        if (offset > 0) {
            if (offset > len) throw argumentError(context, "offset outside of string");
            beg += offset;
            len -= offset;
        }


        // FIXME: potentially could just use ByteList here?
        ByteBuffer format = ByteBuffer.wrap(formatString.getUnsafeBytes(), formatString.begin(), formatString.length());
        ByteBuffer encode = ByteBuffer.wrap(encodedString.getUnsafeBytes(), beg, len);
        int next = getDirective(context, "unpack", formatString, format);
        IRubyObject value = null; // UNPACK_1

        mainLoop: while (next != 0) {
            int type = next;
            next = getDirective(context, "unpack", formatString, format);

            if (isSpace(type)) continue;

            if (type == '#') {
                for (type = safeGet(format); type != '\n' && type != 0; type = safeGet(format)) {}
                type = safeGet(format);
                if (type == 0) break;
                next = getDirective(context, "unpack", formatString, format);
            }

            // Next indicates to decode using native encoding format
            if (next == '_' || next == '!') {
                int index = NATIVE_CODES.indexOf(type);
                if (index == -1) {
                    throw argumentError(context, "'" + next + "' allowed only after types " + NATIVE_CODES);
                }
                type = MAPPED_CODES.charAt(index);
                
                next = getDirective(context, "unpack", formatString, format);
            }
            
            if (next == '>' || next == '<') {
                next = next == '>' ? BE : LE;
                int index = ENDIANESS_CODES.indexOf(type + next);
                if (index == -1) {
                    throw argumentError(context, "'" + (char)next + "' allowed only after types sSiIlLqQjJ");
                }
                type = ENDIANESS_CODES.charAt(index);
                next = getDirective(context, "unpack", formatString, format);
                
                if (next == '_' || next == '!') next = getDirective(context, "unpack", formatString, format);
            }

            // How many occurrences of 'type' we want
            int occurrences;
            if (next == 0) {
                occurrences = 1;
            } else {
                if (next == '*') {
                    occurrences = IS_STAR;
                    next = getDirective(context, "unpack", formatString, format);
                } else if (ASCII.isDigit(next)) {
                    occurrences = 0;
                    do {
                        occurrences = occurrences * 10 + Character.digit((char)(next & 0xFF), 10);
                        next = getDirective(context, "unpack", formatString, format);
                        if (occurrences < 0) throw rangeError(context, "pack length too big");
                    } while (next != 0 && ASCII.isDigit(next));
                } else {
                    occurrences = type == '@' ? 0 : 1;
                }
            }

            // See if we have a converter for the job...
            Converter converter = converters[type];
            if (converter != null) {
                value = decode(context, encode, occurrences, result, block, converter, mode);
                if (mode == UNPACK_1 && value != null) {
                    return value;
                } else {
                    continue;
                }
            }

            // Otherwise the unpack should be here...
            switch (type) {
                case '@':
                    unpack_at(context, encodedString, encode, occurrences);
                    break;
                case '%':
                    throw argumentError(context, "% is not supported");
                case 'A':
                    value = unpack_A(context, block, result, encode, occurrences, mode);
                    break;
                case 'Z':
                    value = unpack_Z(context, block, result, encode, occurrences, mode);
                    break;
                case 'a':
                    value = unpack_a(context, block, result, encode, occurrences, mode);
                    break;
                case 'b':
                    value = unpack_b(context, block, result, encode, occurrences, mode);
                    break;
                case 'B':
                    value = unpack_B(context, block, result, encode, occurrences, mode);
                    break;
                case 'h':
                    value = unpack_h(context, block, result, encode, occurrences, mode);
                    break;
                case 'H':
                    value = unpack_H(context, block, result, encode, occurrences, mode);
                    break;
                case 'u':
                    value = unpack_u(context, block, result, encode, mode);
                    break;
                case 'm':
                    value = unpack_m(context, block, result, encode, occurrences, mode);
                    break;
                case 'M':
                    value = unpack_M(context, block, result, encode, mode);
                    break;
                case 'U':
                    value = unpack_U(context, block, result, encode, occurrences, mode);
                    break;
                case 'X':
                    unpack_X(context, encode, occurrences);
                    break;
                case 'x':
                    unpack_x(context, encode, occurrences);
                    break;
                case 'w':
                    value = unpack_w(context, block, result, encode, occurrences, mode);
                    break;
                default:
                    unknownDirective(context, "unpack", type, formatString);
                    break;

            }
            if (mode == UNPACK_1 && value != null) return value;
        }
        return result;
    }

    private static boolean isSpace(int type) {
        boolean isSpace = switch (type) {
            case ' ', '\011', '\n', '\013', '\014', '\015' -> true;
            default -> false;
        };
        if (isSpace) return true;
        return false;
    }

    private static IRubyObject unpack_w(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining()) {
            occurrences = encode.remaining();
        }

        long ul = 0;
        long ulmask = (0xfeL << 56) & 0xffffffff;
        RubyBignum big128 = RubyBignum.newBignum(context.runtime, 128);
        int pos = encode.position();

        while (occurrences > 0 && pos < encode.limit()) {
            ul <<= 7;
            ul |= encode.get(pos) & 0x7f;
            if((encode.get(pos++) & 0x80) == 0) {
                IRubyObject value = asFixnum(context, ul);
                if (mode == UNPACK_1) return value;

                appendOrYield(context, block, result, value, mode);
                occurrences--;
                ul = 0;
            } else if((ul & ulmask) == 0) {
                RubyBignum big = RubyBignum.newBignum(context.runtime, ul);
                while(occurrences > 0 && pos < encode.limit()) {
                    IRubyObject mulResult = big.op_mul(context, big128);
                    IRubyObject v = mulResult.callMethod(context, "+",
                            RubyBignum.newBignum(context.runtime, encode.get(pos) & 0x7f));
                    if(v instanceof RubyFixnum) {
                        big = RubyBignum.newBignum(context.runtime, RubyNumeric.fix2long(v));
                    } else if (v instanceof RubyBignum) {
                        big = (RubyBignum)v;
                    }
                    if((encode.get(pos++) & 0x80) == 0) {
                        IRubyObject value = RubyBignum.bignorm(context.runtime, big.getValue());
                        if (mode == UNPACK_1) {
                            return value;
                        }
                        appendOrYield(context, block, result, value, mode);
                        occurrences--;
                        ul = 0;
                        break;
                    }
                }
            }
        }
        try {
            positionBuffer(encode, pos);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, "in 'unpack': poorly encoded input");
        }
        return context.nil;
    }

    private static void unpack_x(ThreadContext context, ByteBuffer encode, int occurrences) {
        if (occurrences == IS_STAR) {
             occurrences = encode.remaining();
        }

        try {
            positionBuffer(encode, encode.position() + occurrences);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, "in 'unpack': x outside of string");
        }
    }

    private static void unpack_X(ThreadContext context, ByteBuffer encode, int occurrences) {
        if (occurrences == IS_STAR) {
            // MRI behavior: Contrary to what seems to be logical,
            // when '*' is given, MRI calculates the distance
            // to the end, in order to go backwards.
            occurrences = /*encode.limit() - */encode.remaining();
        }

        try {
            positionBuffer(encode, encode.position() - occurrences);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, "in 'unpack': X outside of string");
        }
    }

    private static IRubyObject unpack_U(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining()) {
            occurrences = encode.remaining();
        }

        while (occurrences-- > 0 && encode.remaining() > 0) {
            try {
                // TODO: for now, we use a faithful
                // reimplementation of MRI's algorithm,
                // but should use UTF8Encoding facilities
                // from Joni, once it starts prefroming
                // UTF-8 content validation.
                RubyFixnum item = asFixnum(context, utf8Decode(encode));
                if (mode == UNPACK_1) {
                    return item;
                }
                appendOrYield(context, block, result, item, mode);
            } catch (IllegalArgumentException e) {
                throw argumentError(context, e.getMessage());
            }
        }
        return context.nil;
    }

    private static IRubyObject unpack_M(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int mode) {
        byte[] lElem = new byte[Math.max(encode.remaining(),0)];
        int index = 0;
        for(;;) {
            if (!encode.hasRemaining()) break;
            int c = safeGet(encode);
            if (c != '=') {
                lElem[index++] = (byte)c;
            } else {
                if (!encode.hasRemaining()) break;
                markBuffer(encode);
                int c1 = safeGet(encode);
                if (c1 == '\n' || (c1 == '\r' && (c1 = safeGet(encode)) == '\n')) continue;
                int d1 = Character.digit(c1, 16);
                if (d1 == -1) {
                    encode.reset();
                    break;
                }
                markBuffer(encode);
                if (!encode.hasRemaining()) break;
                int c2 = safeGet(encode);
                int d2 = Character.digit(c2, 16);
                if (d2 == -1) {
                    encode.reset();
                    break;
                }
                byte value = (byte)(d1 << 4 | d2);
                lElem[index++] = value;
            }
        }
        return appendOrYield(context, block, result, new ByteList(lElem, 0, index, ASCII, false), mode);
    }

    private static IRubyObject unpack_m(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        int length = encode.remaining()*3/4;
        byte[] lElem = new byte[length];
        int a = -1, b = -1, c = 0, d;
        int index = 0;
        int s = -1;

        if (occurrences == 0){
            index = unpack_m_zeroOccurrences(context, encode, lElem, a, b, c, index, s);
        } else {
            index = unpack_m_nonzeroOccurrences(encode, lElem, a, b, c, index);
        }
        return appendOrYield(context, block, result, new ByteList(lElem, 0, index, ASCII, false), mode);
    }

    private static int unpack_m_nonzeroOccurrences(ByteBuffer encode, byte[] lElem, int a, int b, int c, int index) {
        int d;
        int s;
        while (encode.hasRemaining()) {
            a = b = c = d = -1;

            // obtain a
            s = safeGet(encode);
            while (((a = b64_xtable[s]) == -1) && encode.hasRemaining()) {
                s = safeGet(encode);
            }
            if (a == -1) break;

            // obtain b
            s = safeGet(encode);
            while (((b = b64_xtable[s]) == -1) && encode.hasRemaining()) {
                s = safeGet(encode);
            }
            if (b == -1) break;

            // obtain c
            s = safeGet(encode);
            while (((c = b64_xtable[s]) == -1) && encode.hasRemaining()) {
                if (s == '=') break;
                s = safeGet(encode);
            }
            if ((s == '=') || c == -1) {
                if (s == '=') {
                    positionBuffer(encode, encode.position() - 1);
                }
                break;
            }

            // obtain d
            s = safeGet(encode);
            while (((d = b64_xtable[s]) == -1) && encode.hasRemaining()) {
                if (s == '=') break;
                s = safeGet(encode);
            }
            if ((s == '=') || d == -1) {
                if (s == '=') {
                    positionBuffer(encode, encode.position() - 1);
                }
                break;
            }

            // calculate based on a, b, c and d
            lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
            lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
            lElem[index++] = (byte)((c << 6 | d) & 255);
            a = -1;
        }

        if (a != -1 && b != -1) {
            if (c == -1) {
                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
            } else {
                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
            }
        }
        return index;
    }

    private static int unpack_m_zeroOccurrences(ThreadContext context, ByteBuffer encode, byte[] lElem, int a, int b, int c, int index, int s) {
        int d;
        if (encode.remaining()%4 != 0) {
            throw argumentError(context, "invalid base64");
        }
        while (encode.hasRemaining() && s != '=') {
            a = b = c = -1;
            d = -2;

            // obtain a
            s = safeGet(encode);
            a = b64_xtable[s];
            if (a == -1) throw argumentError(context, "invalid base64");

            // obtain b
            s = safeGet(encode);
            b = b64_xtable[s];
            if (b == -1) throw argumentError(context, "invalid base64");

            // obtain c
            s = safeGet(encode);
            c = b64_xtable[s];
            if (s == '=') {
                if (safeGet(encode) != '=') throw argumentError(context, "invalid base64");
                break;
            }
            if (c == -1) throw argumentError(context, "invalid base64");

            // obtain d
            s = safeGet(encode);
            d = b64_xtable[s];
            if (s == '=') break;
            if (d == -1) throw argumentError(context, "invalid base64");

            // calculate based on a, b, c and d
            lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
            lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
            lElem[index++] = (byte)((c << 6 | d) & 255);
        }

        if (encode.hasRemaining()) throw argumentError(context, "invalid base64");

        if (a != -1 && b != -1) {
            if (c == -1 && s == '=') {
                if ((b & 15) > 0) throw argumentError(context, "invalid base64");
                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
            } else if(c != -1 && s == '=') {
                if ((c & 3) > 0) throw argumentError(context, "invalid base64");
                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
            }
        }
        return index;
    }

    private static IRubyObject unpack_u(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int mode) {
        int length = encode.remaining() * 3 / 4;
        byte[] lElem = new byte[length];
        int index = 0;
        int s = 0;
        int total = 0;
        if (length > 0) s = encode.get();
        while (encode.hasRemaining() && s > ' ' && s < 'a') {
            int a, b, c, d;
            byte[] hunk = new byte[3];

            int len = (s - ' ') & 077;
            s = safeGet(encode);
            total += len;
            if (total > length) {
                len -= total - length;
                total = length;
            }

            while (len > 0) {
                int mlen = len > 3 ? 3 : len;

                if (encode.hasRemaining() && s >= ' ') {
                    a = (s - ' ') & 077;
                    s = safeGet(encode);
                } else
                    a = 0;
                if (encode.hasRemaining() && s >= ' ') {
                    b = (s - ' ') & 077;
                    s = safeGet(encode);
                } else
                    b = 0;
                if (encode.hasRemaining() && s >= ' ') {
                    c = (s - ' ') & 077;
                    s = safeGet(encode);
                } else
                    c = 0;
                if (encode.hasRemaining() && s >= ' ') {
                    d = (s - ' ') & 077;
                    s = safeGet(encode);
                } else
                    d = 0;
                hunk[0] = (byte)((a << 2 | b >> 4) & 255);
                hunk[1] = (byte)((b << 4 | c >> 2) & 255);
                hunk[2] = (byte)((c << 6 | d) & 255);

                for (int i = 0; i < mlen; i++) lElem[index++] = hunk[i];
                len -= mlen;
            }
            if (s == '\r') {
                s = safeGet(encode);
            }
            if (s == '\n') {
                s = safeGet(encode);
            }
            else if (encode.hasRemaining()) {
                if (safeGet(encode) == '\n') {
                    safeGet(encode); // Possible Checksum Byte
                } else if (encode.hasRemaining()) {
                    positionBuffer(encode, encode.position() - 1);
                }
            }
        }
        return appendOrYield(context, block, result, new ByteList(lElem, 0, index, ASCII, false), mode);
    }

    private static IRubyObject unpack_H(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining() * 2) {
            occurrences = encode.remaining() * 2;
        }
        int bits = 0;
        byte[] lElem = new byte[occurrences];
        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
            if ((lCurByte & 1) != 0) {
                bits <<= 4;
            } else {
                bits = encode.get();
            }
            lElem[lCurByte] = sHexDigits[(bits >>> 4) & 15];
        }
        return appendOrYield(context, block, result, new ByteList(lElem, USASCII, false), mode);
    }

    private static IRubyObject unpack_h(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining() * 2) {
            occurrences = encode.remaining() * 2;
        }
        int bits = 0;
        byte[] lElem = new byte[occurrences];
        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
            if ((lCurByte & 1) != 0) {
                bits >>>= 4;
            } else {
                bits = encode.get();
            }
            lElem[lCurByte] = sHexDigits[bits & 15];
        }
        return appendOrYield(context, block, result, new ByteList(lElem, USASCII, false), mode);
    }

    private static IRubyObject unpack_B(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining() * 8) {
            occurrences = encode.remaining() * 8;
        }
        int bits = 0;
        byte[] lElem = new byte[occurrences];
        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
            if ((lCurByte & 7) != 0) {
                bits <<= 1;
            } else {
                bits = encode.get();
            }
            lElem[lCurByte] = (bits & 128) != 0 ? (byte)'1' : (byte)'0';
        }

        return appendOrYield(context, block, result, new ByteList(lElem, ASCII, false), mode);
    }

    private static IRubyObject unpack_b(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining() * 8) {
            occurrences = encode.remaining() * 8;
        }
        int bits = 0;
        byte[] lElem = new byte[occurrences];
        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
            if ((lCurByte & 7) != 0) {
                bits >>>= 1;
            } else {
                bits = encode.get();
            }
            lElem[lCurByte] = (bits & 1) != 0 ? (byte)'1' : (byte)'0';
        }
        return appendOrYield(context, block, result, new ByteList(lElem, USASCII, false), mode);
    }

    private static IRubyObject unpack_a(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining()) {
            occurrences = encode.remaining();
        }
        byte[] potential = new byte[occurrences];
        encode.get(potential);
        return appendOrYield(context, block, result, new ByteList(potential, ASCII, false), mode);
    }

    private static IRubyObject unpack_Z(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        boolean isStar = (occurrences == IS_STAR);

        if (occurrences == IS_STAR || occurrences > encode.remaining()) {
            occurrences = encode.remaining();
        }

        byte[] potential = new byte[occurrences];
        int t = 0;

        while (t < occurrences) {
            byte b = encode.get();
            if (b == 0) {
                break;
            }
            potential[t] = b;
            t++;
        }

        IRubyObject value = appendOrYield(context, block, result, new ByteList(potential, 0, t, ASCII, false), mode);
        if (mode == UNPACK_1) {
             return value;
        }

        // When the number of occurrences is
        // explicitly specified, we have to read up
        // the remaining garbage after the '\0' to
        // satisfy the requested pattern.
        if (!isStar) {
            if (t < occurrences) {
                // We encountered '\0' when
                // reading the buffer above,
                // increment the number of read bytes.
                t++;
            }

            while (t < occurrences) {
                encode.get();
                t++;
            }
        }
        return context.nil;
    }

    private static IRubyObject unpack_A(ThreadContext context, Block block, RubyArray result, ByteBuffer encode, int occurrences, int mode) {
        if (occurrences == IS_STAR || occurrences > encode.remaining()) {
            occurrences = encode.remaining();
        }

        byte[] potential = new byte[occurrences];
        encode.get(potential);

        for (int t = occurrences - 1; occurrences > 0; occurrences--, t--) {
            byte c = potential[t];
            if (c != '\0' && c != ' ') {
               break;
            }
        }

        return appendOrYield(context, block, result, new ByteList(potential, 0, occurrences, ASCII, false), mode);
    }

    private static void unpack_at(ThreadContext context, ByteList encodedString, ByteBuffer encode, int occurrences) {
        int limit = encodedString.begin() + (occurrences == IS_STAR ? encode.remaining() : occurrences);

        if (limit > encode.limit() || limit < 0) throw argumentError(context, "@ outside of string");

        positionBuffer(encode, limit);
    }

    private static void appendOrYield(ThreadContext context, Block block, RubyArray result, IRubyObject item, int mode) {
        if (mode == UNPACK_BLOCK) {
            block.yield(context, item);
        } else if (mode == UNPACK_ARRAY) {
            result.append(context, item);
        }
    }

    private static IRubyObject appendOrYield(ThreadContext context, Block block, RubyArray result, ByteList item, int mode) {
        RubyString itemStr = newString(context, item);
        if (mode == UNPACK_1) {
            return itemStr;
        } else {
            appendOrYield(context, block, result, itemStr, mode);
            return context.nil;
        }
    }

    /** rb_uv_to_utf8
     *
     */
    public static int utf8Decode(Ruby runtime, byte[]to, int p, int code) {
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
        throw runtime.newRangeError("pack(U): value out of range");
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

    public static int getDirective(ThreadContext context, String mode, ByteList formatString, ByteBuffer encode) {
        if (!encode.hasRemaining()) return 0;

        int got = encode.get() & 0xff;

        if (got == 0) unknownDirective(context, mode, 0, formatString);

        return got;
    }

    public static IRubyObject decode(ThreadContext context, ByteBuffer encode, int occurrences,
            RubyArray result, Block block, Converter converter, int mode) {
        int lPadLength = 0;

        if (occurrences == IS_STAR) {
            occurrences = encode.remaining() / converter.size;
        } else if (occurrences > encode.remaining() / converter.size) {
            lPadLength = occurrences - encode.remaining() / converter.size;
            occurrences = encode.remaining() / converter.size;
        }
        for (; occurrences-- > 0;) {
            IRubyObject value = converter.decode(context, encode);
            if (mode == UNPACK_1) return value;

            appendOrYield(context, block, result, value, mode);
        }

        for (; lPadLength-- > 0;) {
            if (mode == UNPACK_1) return context.nil;

            appendOrYield(context, block, result, context.nil, mode);
        }
        return context.nil;
    }

    private static int encode(ThreadContext context, int occurrences, ByteList result,
            RubyArray list, int index, ConverterExecutor converter) {
        int listSize = list.size();

        while (occurrences-- > 0) {
            if (listSize-- <= 0 || index >= list.size()) {
                throw argumentError(context, sTooFew);
            }

            IRubyObject from = list.eltInternal(index++);

            converter.encode(context, from, result);
        }

        return index;
    }

    private abstract static class ConverterExecutor {
        protected Converter converter;
        public void setConverter(Converter converter) {
            this.converter = converter;
        }

        public abstract IRubyObject decode(ThreadContext context, ByteBuffer format);
        public abstract void encode(ThreadContext context, IRubyObject from, ByteList result);
    }

    private static ConverterExecutor executor() {
        return new ConverterExecutor() {
            @Override
            public IRubyObject decode(ThreadContext context, ByteBuffer format) {
                return converter.decode(context, format);
            }

            @Override
            public void encode(ThreadContext context, IRubyObject from, ByteList result) {
                if (from == context.nil && converter.getType() != null) {
                    throw typeError(context, from, converter.getType());
                }
                converter.encode(context, from, result);
            }
        };
    }

    public abstract static class Converter {
        public final int size;
        public final String type;

        public Converter(int size) {
            this(size, null);
        }
        
        public Converter(int size, String type) {
            this.size = size;
            this.type = type;
        }
        
        public String getType() {
            return type;
        }

        public abstract IRubyObject decode(ThreadContext context, ByteBuffer format);
        public abstract void encode(ThreadContext context, IRubyObject from, ByteList result);
    }

    private abstract static class QuadConverter extends Converter {
        public QuadConverter(int size, String type) {
            super(size, type);
        }
        
        public QuadConverter(int size) {
            super(size);
        }

        protected int overflowQuad(long quad) {
            return (int) (quad & 0xffff);
        }

        protected void encodeShortByByteOrder(ByteList result, int s) {
            if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {
                encodeShortBigEndian(result, s);
            } else {
                encodeShortLittleEndian(result, s);
            }
        }

        protected void encodeLongByByteOrder(ByteList result, long l) {
            if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {
                encodeLongBigEndian(result, l);
            } else {
                encodeLongLittleEndian(result, l);
            }
        }
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

    public static RubyString pack(ThreadContext context, RubyArray list, RubyString formatString, RubyString buffer) {
        return packCommon(context, list, formatString.getByteList(), executor(), buffer);
    }

    /**
     * Introduced to allow outlining cases in #packCommon that update both of these values.
     */
    private static class PackInts {
        PackInts(int listSize, int idx) {
            this.listSize = listSize;
            this.idx = idx;
        }
        int listSize;
        int idx;
    }

    private static RubyString packCommon(ThreadContext context, RubyArray list, ByteList formatString,
                                         ConverterExecutor executor, RubyString buffer) {
        Ruby runtime = context.runtime;
        ByteBuffer format = ByteBuffer.wrap(formatString.getUnsafeBytes(), formatString.begin(), formatString.length());

        buffer.modify();
        ByteList result = buffer.getByteList();
        PackInts packInts = new PackInts(list.size(), 0);
        int type;
        int next = getDirective(context, "pack", formatString, format);

        int enc_info = 1;

        mainLoop: while (next != 0) {
            type = next;
            next = getDirective(context, "pack", formatString, format);

            // Skip all whitespace in pack format string
            while (ASCII.isSpace(type)) {
                if (next == 0) break mainLoop;
                type = next;
                next = getDirective(context, "pack", formatString, format);
            }

            // Skip embedded comments in pack format string
            if (type == '#') {
                while (type != '\n') {
                    if (next == 0) break mainLoop;
                    type = next;
                    next = getDirective(context, "pack", formatString, format);
                }
            }

            if (next == '!' || next == '_') {
                int index = NATIVE_CODES.indexOf(type);
                if (index == -1) {
                    throw argumentError(context, "'" + next + "' allowed only after types " + NATIVE_CODES);
                }
                int typeBeforeMap = type;
                type = MAPPED_CODES.charAt(index);

                next = getDirective(context, "pack", formatString, format);
            }
            
            if (next == '>' || next == '<') {
                next = next == '>' ? BE : LE;
                int index = ENDIANESS_CODES.indexOf(type + next);
                if (index == -1) {
                    throw argumentError(context, "'" + (char) next + "' allowed only after types sSiIlLqQ");
                }
                type = ENDIANESS_CODES.charAt(index);
                next = getDirective(context, "pack", formatString, format);
            }

            // Determine how many of type are needed (default: 1)
            int occurrences = 1;
            boolean isStar = false;
            boolean ignoreStar = false;
            if (next != 0) {
                if (next == '*') {
                    if ("@XxumM".indexOf(type) != -1) {
                        occurrences = 0;
                        ignoreStar = true;
                    } else {
                        occurrences = list.size() - packInts.idx;
                        isStar = true;
                    }
                    next = getDirective(context, "pack", formatString, format);
                } else if (ASCII.isDigit(next)) {
                    occurrences = 0;
                    do {
                        occurrences = occurrences * 10 + Character.digit((char)(next & 0xFF), 10);
                        next = getDirective(context, "pack", formatString, format);
                    } while (next != 0 && ASCII.isDigit(next));
                }
            }

            enc_info = adjustEncInfo(type, enc_info);

            Converter converter = converters[type];

            if (converter != null) {
                executor.setConverter(converter);
                packInts.idx = encode(context, occurrences, result, list, packInts.idx, executor);
                continue;
            }

            switch (type) {
                case '%':
                    throw argumentError(context, "% is not supported");
                case 'A':
                case 'a':
                case 'Z':
                case 'B':
                case 'b':
                case 'H':
                case 'h':
                    pack_h(context, list, result, packInts, type, occurrences, isStar);
                    break;
                case 'x':
                    grow(result, sNil10, occurrences);
                    break;
                case 'X':
                    pack_X(context, result, occurrences);
                    break;
                case '@':
                    pack_at(result, occurrences);
                    break;
                case 'u':
                case 'm':
                    pack_m(context, list, result, packInts, (char) type, occurrences, ignoreStar);
                    break;
                case 'M':
                    pack_M(context, list, result, packInts, occurrences);
                    break;
                case 'U':
                    pack_U(context, list, result, packInts, occurrences);
                    break;
                case 'w':
                    pack_w(context, list, result, packInts, occurrences);
                    break;
                case ' ':       // various "ok" whitespace
                case '\011':
                case '\n':
                case '\013':
                case '\014':
                case '\015':
                    break;
                default:
                    unknownDirective(context, "pack", type, formatString);
                    break;
            }
        }

        switch (enc_info) {
            case 1:
                buffer.setEncodingAndCodeRange(USASCII, StringSupport.CR_7BIT);
                break;
            case 2:
                buffer.associateEncoding(UTF8);
                break;
            default:
                /* do nothing, keep ASCII-8BIT */
        }

        return buffer;
    }

    private static void unknownDirective(ThreadContext context, String mode, int type, ByteList formatString) {
        ByteList unknown;
        if (EncodingUtils.isPrint(type)) {
            unknown = new ByteList(new byte[]{(byte) type});
        } else {
            unknown = new ByteList();
            Sprintf.sprintf(context.runtime, unknown, "\\x%02X", type & 0377);
        }

        throw argumentError(context, str(context.runtime, "unknown " + mode + " directive '", newString(context, unknown), "' in '", newString(context,formatString), "'"));
    }

    private static void pack_w(ThreadContext context, RubyArray list, ByteList result, PackInts packInts, int occurrences) {
        while (occurrences-- > 0) {
            if (packInts.listSize-- <= 0) throw argumentError(context, sTooFew);

            IRubyObject from = list.eltInternal(packInts.idx++);
            if (from == context.nil) throw typeError(context, "pack('w') does not take nil");

            final ByteList buf = new ByteList();

            if (from instanceof RubyBignum) {
                RubyBignum big128 = RubyBignum.newBignum(context.runtime, 128);
                while (from instanceof RubyBignum) {
                    RubyArray ary = (RubyArray) ((RubyBignum) from).divmod(context, big128);
                    buf.append((byte) (RubyNumeric.fix2int(ary.eltInternal(1)) | 0x80) & 0xff);
                    from = ary.eltInternal(0);
                }
            }

            long l = RubyNumeric.num2long(from);

            // we don't deal with negatives.
            if (l >= 0) {

                while(l != 0) {
                    buf.append((byte)(((l & 0x7f) | 0x80) & 0xff));
                    l >>= 7;
                }

                int left = 0;
                int right = buf.getRealSize() - 1;

                if (right >= 0) {
                    buf.getUnsafeBytes()[0] &= 0x7F;
                } else {
                    buf.append(0);
                }

                while (left < right) {
                    byte tmp = buf.getUnsafeBytes()[left];
                    buf.getUnsafeBytes()[left] = buf.getUnsafeBytes()[right];
                    buf.getUnsafeBytes()[right] = tmp;

                    left++;
                    right--;
                }

                result.append(buf);
            } else {
                throw argumentError(context, "can't compress negative numbers");
            }
        }
    }

    private static void pack_U(ThreadContext context, RubyArray list, ByteList result, PackInts packInts, int occurrences) {
        while (occurrences-- > 0) {
            if (packInts.listSize-- <= 0) throw argumentError(context, sTooFew);

            IRubyObject from = list.eltInternal(packInts.idx++);
            int code = from == context.nil ? 0 : RubyNumeric.num2int(from);

            if (code < 0) throw rangeError(context, "pack(U): value out of range");

            int len = result.getRealSize();
            result.ensure(len + 6);
            result.setRealSize(len + utf8Decode(context.runtime, result.getUnsafeBytes(), result.getBegin() + len, code));
        }
    }

    private static void pack_M(ThreadContext context, RubyArray list, ByteList result, PackInts packInts, int occurrences) {
        ByteList lCurElemString;
        if (packInts.listSize-- <= 0) throw argumentError(context, sTooFew);

        IRubyObject from = list.eltInternal(packInts.idx++);
        lCurElemString = from == context.nil ? ByteList.EMPTY_BYTELIST : from.asString().getByteList();

        if (occurrences <= 1) {
            occurrences = 72;
        }

        PackUtils.qpencode(result, lCurElemString, occurrences);
    }

    private static void pack_h(ThreadContext context, RubyArray list, ByteList result, PackInts packInts, int type, int occurrences, boolean isStar) {
        ByteList lCurElemString;
        if (packInts.listSize-- <= 0) {
            throw argumentError(context, sTooFew);
        }

        IRubyObject from = list.eltInternal(packInts.idx++);
        lCurElemString = from == context.nil ? ByteList.EMPTY_BYTELIST : from.convertToString().getByteList();

        if (isStar) {
            occurrences = lCurElemString.length();
            // 'Z' adds extra null pad (versus 'a')
            if (type == 'Z') occurrences++;
        }

        pack_h_inner(result, type, lCurElemString, occurrences);
    }

    private static void pack_m(ThreadContext context, RubyArray list, ByteList result, PackInts packInts, char type, int occurrences, boolean ignoreStar) {
        ByteList lCurElemString;
        if (packInts.listSize-- <= 0) throw argumentError(context, sTooFew);

        IRubyObject from = list.eltInternal(packInts.idx++);
        if (from == context.nil) throw typeError(context, from, "Integer");
        lCurElemString = from.convertToString().getByteList();
        encodeUM(context, lCurElemString, occurrences, ignoreStar, type, result);
    }

    private static void pack_at(ByteList result, int occurrences) {
        occurrences -= result.length();
        if (occurrences > 0) {
            grow(result, sNil10, occurrences);
        }
        occurrences = -occurrences;
        if (occurrences > 0) {
            shrink(result, occurrences);
        }
    }

    private static void pack_X(ThreadContext context, ByteList result, int occurrences) {
        try {
            shrink(result, occurrences);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, "in 'pack': X outside of string");
        }
    }

    private static void pack_h_inner(ByteList result, int type, ByteList lCurElemString, int occurrences) {
        switch (type) {
            case 'a' :
            case 'A' :
            case 'Z' :
                pack_h_aAZ(result, type, lCurElemString, occurrences);
                break;
            case 'b' :
                    pack_h_b(result, lCurElemString, occurrences);
            break;
            case 'B' :
                    pack_h_B(result, lCurElemString, occurrences);
            break;
            case 'h' :
                    pack_h_h(result, lCurElemString, occurrences);
            break;
            case 'H' :
                    pack_h_H(result, lCurElemString, occurrences);
            break;
        }
    }

    private static void pack_h_H(ByteList result, ByteList lCurElemString, int occurrences) {
        int currentByte = 0;
        int padLength = 0;

        if (occurrences > lCurElemString.length()) {
            padLength = occurrences - lCurElemString.length() + 1;
            occurrences = lCurElemString.length();
        }

        for (int i = 0; i < occurrences;) {
            byte currentChar = (byte)lCurElemString.charAt(i++);

            if (Character.isJavaIdentifierStart(currentChar)) {
                //this test may be too lax but it is the same as in MRI
                currentByte |= ((currentChar & 15) + 9) & 15;
            } else {
                currentByte |= currentChar & 15;
            }

            if ((i & 1) != 0) {
                currentByte <<= 4;
            } else {
                result.append((byte) (currentByte & 0xff));
                currentByte = 0;
            }
        }

        if ((occurrences & 1) != 0) {
            result.append((byte) (currentByte & 0xff));
            if (padLength > 0) padLength--;
        }

        result.length(result.length() + padLength / 2);
    }

    private static void pack_h_h(ByteList result, ByteList lCurElemString, int occurrences) {
        int currentByte = 0;
        int padLength = 0;

        if (occurrences > lCurElemString.length()) {
            padLength = occurrences - lCurElemString.length() + 1;
            occurrences = lCurElemString.length();
        }

        for (int i = 0; i < occurrences;) {
            byte currentChar = (byte)lCurElemString.charAt(i++);

            if (Character.isJavaIdentifierStart(currentChar)) {
                //this test may be too lax but it is the same as in MRI
                currentByte |= (((currentChar & 15) + 9) & 15) << 4;
            } else {
                currentByte |= (currentChar & 15) << 4;
            }

            if ((i & 1) != 0) {
                currentByte >>= 4;
            } else {
                result.append((byte) (currentByte & 0xff));
                currentByte = 0;
            }
        }

        if ((occurrences & 1) != 0) {
            result.append((byte) (currentByte & 0xff));
            if (padLength > 0) padLength--;
        }

        result.length(result.length() + padLength / 2);
    }

    private static void pack_h_B(ByteList result, ByteList lCurElemString, int occurrences) {
        int currentByte = 0;
        int padLength = 0;

        if (occurrences > lCurElemString.length()) {
            padLength = (occurrences - lCurElemString.length()) / 2 + (occurrences + lCurElemString.length()) % 2;
            occurrences = lCurElemString.length();
        }

        for (int i = 0; i < occurrences;) {
            currentByte |= lCurElemString.charAt(i++) & 1;

            // we filled up current byte; append it and create next one
            if ((i & 7) == 0) {
                result.append((byte) (currentByte & 0xff));
                currentByte = 0;
                continue;
            }

            //if the index is not a multiple of 8, we are not on a byte boundary
            currentByte <<= 1;
        }

        if ((occurrences & 7) != 0) { //if the length is not a multiple of 8
            currentByte <<= 7 - (occurrences & 7); //we need to pad the last byte
            result.append((byte) (currentByte & 0xff));
        }

        result.length(result.length() + padLength);
    }

    private static void pack_h_b(ByteList result, ByteList lCurElemString, int occurrences) {
        int currentByte = 0;
        int padLength = 0;

        if (occurrences > lCurElemString.length()) {
            padLength = (occurrences - lCurElemString.length()) / 2 + (occurrences + lCurElemString.length()) % 2;
            occurrences = lCurElemString.length();
        }

        for (int i = 0; i < occurrences;) {
            if ((lCurElemString.charAt(i++) & 1) != 0) {//if the low bit is set
                currentByte |= 128; //set the high bit of the result
            }

            if ((i & 7) == 0) {
                result.append((byte) (currentByte & 0xff));
                currentByte = 0;
                continue;
            }

               //if the index is not a multiple of 8, we are not on a byte boundary
               currentByte >>= 1; //shift the byte
        }

        if ((occurrences & 7) != 0) { //if the length is not a multiple of 8
            currentByte >>= 7 - (occurrences & 7); //we need to pad the last byte
            result.append((byte) (currentByte & 0xff));
        }

        //do some padding, I don't understand the padding strategy
        result.length(result.length() + padLength);
    }

    private static void pack_h_aAZ(ByteList result, int type, ByteList lCurElemString, int occurrences) {
        if (lCurElemString.length() >= occurrences) {
            result.append(lCurElemString.getUnsafeBytes(), lCurElemString.getBegin(), occurrences);
        } else {//need padding
            //I'm fairly sure there is a library call to create a
            //string filled with a given char with a given length but I couldn't find it
            result.append(lCurElemString);
            occurrences -= lCurElemString.length();

            switch (type) {
              case 'a':
              case 'Z':
                  grow(result, sNil10, occurrences);
                  break;
              default:
                  grow(result, sSp10, occurrences);
                  break;
            }
        }
    }

    private static int adjustEncInfo(int type, int enc_info) {
        switch (type) {
            case 'U':
                if (enc_info == 1) enc_info = 2;
                break;
            case 'm':
            case 'M':
            case 'u':
                break;
            default:
                enc_info = 0;
                break;
        }
        return enc_info;
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

    @Deprecated
    public static RubyArray unpack(Ruby runtime, ByteList encodedString, ByteList formatString) {
        return unpackWithBlock(runtime.getCurrentContext(), runtime, encodedString, formatString, Block.NULL_BLOCK);
    }

    @Deprecated
    public static RubyString pack(Ruby runtime, RubyArray list, ByteList formatString) {
        RubyString buffer = runtime.newString();
        return packCommon(runtime.getCurrentContext(), list, formatString, executor(), buffer);
    }

    @Deprecated
    public static RubyString pack(ThreadContext context, Ruby runtime, RubyArray list, RubyString formatString) {
        RubyString buffer = runtime.newString();
        return pack(context, list, formatString, buffer);
    }

    @Deprecated
    public static void decode(ThreadContext context, Ruby runtime, ByteBuffer encode, int occurrences,
                              RubyArray result, Block block, Converter converter) {
        decode(context, encode, occurrences, result, block, converter, block.isGiven() ? UNPACK_BLOCK : UNPACK_ARRAY);
    }

    @Deprecated
    public static RubyArray unpackWithBlock(ThreadContext context, Ruby runtime, ByteList encodedString, ByteList formatString, Block block) {
        return unpackWithBlock(context, RubyString.newStringLight(runtime, encodedString), formatString, block);
    }
}
