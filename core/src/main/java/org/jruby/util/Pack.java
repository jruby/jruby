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
package org.jruby.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.jcodings.Encoding;

import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.platform.Platform;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
    private static final byte[] hex_table;
    private static final byte[] uu_table;
    private static final byte[] b64_table;
    private static final byte[] sHexDigits;
    private static final int[] b64_xtable = new int[256];
    private static final Converter[] converters = new Converter[256];

    private static long num2quad(IRubyObject arg) {
        if (arg == arg.getRuntime().getNil()) {
            return 0L;
        }
        else if (arg instanceof RubyBignum) {
            BigInteger big = ((RubyBignum)arg).getValue();
            return big.longValue();
        }
        return RubyNumeric.num2long(arg);
    }

    private static float obj2flt(Ruby runtime, IRubyObject o) {
        return (float) TypeConverter.toFloat(runtime, o).getDoubleValue();        
    }

    private static double obj2dbl(Ruby runtime, IRubyObject o) {
        return TypeConverter.toFloat(runtime, o).getDoubleValue();        
    }    

    static {
        hex_table = ByteList.plain("0123456789ABCDEF");
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
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return RubyFloat.newFloat(runtime, decodeFloatLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeFloatLittleEndian(result, obj2flt(runtime, o));
            }
        };
        // single precision, big-endian
        converters['g'] = new Converter(4) {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return RubyFloat.newFloat(runtime, decodeFloatBigEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeFloatBigEndian(result, obj2flt(runtime, o));
            }
        };
        // single precision, native
        Converter tmp = new Converter(4) {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return RubyFloat.newFloat(runtime, 
                        Platform.BYTE_ORDER == Platform.BIG_ENDIAN ? 
                        decodeFloatBigEndian(enc) : decodeFloatLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result) {
                if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {                
                    encodeFloatBigEndian(result, obj2flt(runtime, o));
                } else {
                    encodeFloatLittleEndian(result, obj2flt(runtime, o));
                }
            }
        };
        converters['F'] = tmp; // single precision, native
        converters['f'] = tmp; // single precision, native

        // double precision, little-endian
        converters['E'] = new Converter(8) {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return RubyFloat.newFloat(runtime, decodeDoubleLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeDoubleLittleEndian(result, obj2dbl(runtime, o));
            }               
        };
        // double precision, big-endian
        converters['G'] = new Converter(8) {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return RubyFloat.newFloat(runtime, decodeDoubleBigEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeDoubleBigEndian(result, obj2dbl(runtime, o));
            }
        };
        // double precision, native
        tmp = new Converter(8) {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {
                    return RubyFloat.newFloat(runtime, decodeDoubleBigEndian(enc));
                } else {
                    return RubyFloat.newFloat(runtime, decodeDoubleLittleEndian(enc));
                }
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeDoubleLittleEndian(result, obj2dbl(runtime, o));
            }     
        };
        converters['D'] = tmp; // double precision, native
        converters['d'] = tmp; // double precision, native

        // signed short, little-endian
        tmp = new QuadConverter(2, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeShortUnsignedLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeShortLittleEndian(result, overflowQuad(num2quad(o)));
            }            
        };
        converters['v'] = tmp;
        converters['S' + LE] = tmp;
        // signed short, big-endian
        tmp = new QuadConverter(2, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeShortUnsignedBigEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result) {
                encodeShortBigEndian(result, overflowQuad(num2quad(o)));
            }
        };
        converters['n'] = tmp;
        converters['S' + BE] = tmp;
        // signed short, native
        converters['s'] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(Platform.BYTE_ORDER == Platform.BIG_ENDIAN ? 
                        decodeShortBigEndian(enc) : decodeShortLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result) {
                encodeShortByByteOrder(result, overflowQuad(num2quad(o))); // XXX: 0xffff0000 on BE?
            }
        };
        // unsigned short, native
        converters['S'] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(Platform.BYTE_ORDER == Platform.BIG_ENDIAN ?
                    decodeShortUnsignedBigEndian(enc) : decodeShortUnsignedLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeShortByByteOrder(result, overflowQuad(num2quad(o)));
            }
        };
        // signed short, little endian
        converters['s' + LE] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeShortLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result) {
                encodeShortLittleEndian(result, overflowQuad(num2quad(o))); // XXX: 0xffff0000 on BE?
            }
        };
        // signed short, big endian
        converters['s' + BE] = new QuadConverter(2, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeShortBigEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result) {
                encodeShortBigEndian(result, overflowQuad(num2quad(o))); // XXX: 0xffff0000 on BE?
            }
        };

        // signed char
        converters['c'] = new Converter(1, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                int c = enc.get();
                return runtime.newFixnum(c > (char) 127 ? c-256 : c);
            }

            public void encode(Ruby runtime, IRubyObject o, ByteList result) {
                byte c = (byte) (num2quad(o) & 0xff);
                result.append(c);
            }
        };
        // unsigned char
        converters['C'] = new Converter(1, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(enc.get() & 0xFF);
            }

            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                byte c = o == runtime.getNil() ? 0 : (byte) (num2quad(o) & 0xff);
                result.append(c);
            }
        };

        // unsigned long, little-endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeIntUnsignedLittleEndian(enc));
            }
            
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeIntLittleEndian(result, (int) RubyNumeric.num2long(o));
            }
        };
        converters['V'] = tmp;
        converters['L' + LE] = tmp;
        converters['I' + LE] = tmp;
        
        // unsigned long, big-endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeIntUnsignedBigEndian(enc));
            }
            
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeIntBigEndian(result, (int) RubyNumeric.num2long(o));
            }
        };
        converters['N'] = tmp;
        converters['L' + BE] = tmp;
        converters['I' + BE] = tmp;

        // unsigned int, native
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                if (Platform.BYTE_ORDER == Platform.BIG_ENDIAN) {
                    return runtime.newFixnum(decodeIntUnsignedBigEndian(enc));
                } else {
                    return runtime.newFixnum(decodeIntUnsignedLittleEndian(enc));
                }
            }
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                int s = o == runtime.getNil() ? 0 : (int) RubyNumeric.num2long(o);
                packInt_i(result, s);
            }
        };
        converters['I'] = tmp; // unsigned int, native
        converters['L'] = tmp; // unsigned long, native

        // int, native
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                int value = unpackInt_i(enc);
                return runtime.newFixnum(value);
            }
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                int s = o == runtime.getNil() ? 0 : (int)RubyNumeric.num2long(o);
                packInt_i(result, s);
            }
        };
        converters['i'] = tmp; // int, native
        converters['l'] = tmp; // long, native
        
        // int, little endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeIntLittleEndian(enc));
            }
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                int s = o == runtime.getNil() ? 0 : (int)RubyNumeric.num2long(o);
                encodeIntLittleEndian(result, s);
            }
        };
        converters['i' + LE] = tmp; // int, native
        converters['l' + LE] = tmp; // long, native
        
        // int, big endian
        tmp = new Converter(4, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeIntBigEndian(enc));
            }
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                int s = o == runtime.getNil() ? 0 : (int)RubyNumeric.num2long(o);
                encodeIntBigEndian(result, s);
            }
        };
        converters['i' + BE] = tmp; // int, native
        converters['l' + BE] = tmp; // long, native

        // 64-bit number, native (as bignum)
        converters['Q'] = new QuadConverter(8, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                long l = Platform.BYTE_ORDER == Platform.BIG_ENDIAN ? decodeLongBigEndian(enc) : decodeLongLittleEndian(enc);

                return RubyBignum.bignorm(runtime,BigInteger.valueOf(l).and(new BigInteger("FFFFFFFFFFFFFFFF", 16)));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeLongByByteOrder(result, num2quad(o));
            }
        };
        // 64-bit number, little endian (as bignum)
        converters['Q' + LE] = new QuadConverter(8, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                long l = decodeLongLittleEndian(enc);
                return RubyBignum.bignorm(runtime,BigInteger.valueOf(l).and(new BigInteger("FFFFFFFFFFFFFFFF", 16)));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeLongLittleEndian(result, num2quad(o));
            }
        };
        // 64-bit number, big endian (as bignum)
        converters['Q' + BE] = new QuadConverter(8, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                long l = decodeLongBigEndian(enc);
                return RubyBignum.bignorm(runtime,BigInteger.valueOf(l).and(new BigInteger("FFFFFFFFFFFFFFFF", 16)));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeLongBigEndian(result, num2quad(o));
            }
        };
        // 64-bit number, native (as fixnum)
        converters['q'] = new QuadConverter(8, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(Platform.BYTE_ORDER == Platform.BIG_ENDIAN ? 
                        decodeLongBigEndian(enc) : decodeLongLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeLongByByteOrder(result, num2quad(o));
            }
        };
        // 64-bit number, little-endian (as fixnum)
        converters['q' + LE] = new QuadConverter(8, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeLongLittleEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeLongLittleEndian(result, num2quad(o));
            }
        };
        // 64-bit number, big-endian (as fixnum)
        converters['q' + BE] = new QuadConverter(8, "Integer") {
            public IRubyObject decode(Ruby runtime, ByteBuffer enc) {
                return runtime.newFixnum(decodeLongBigEndian(enc));
            }

            @Override
            public void encode(Ruby runtime, IRubyObject o, ByteList result){
                encodeLongBigEndian(result, num2quad(o));
            }
        };
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

    /**
     * encodes a String in base64 or its uuencode variant.
     * appends the result of the encoding in a StringBuffer
     * @param io2Append The StringBuffer which should receive the result
     * @param i2Encode The String to encode
     * @param iLength The max number of characters to encode
     * @param iType the type of encoding required (this is the same type as used by the pack method)
     * @param tailLf true if the traililng "\n" is needed
     * @return the io2Append buffer
     **/
    private static ByteList encodes(Ruby runtime, ByteList io2Append,byte[]charsToEncode, int startIndex, int length, int charCount, byte encodingType, boolean tailLf) {
        charCount = charCount < length ? charCount : length;

        io2Append.ensure(charCount * 4 / 3 + 6);
        int i = startIndex;
        byte[] lTranslationTable = encodingType == 'u' ? uu_table : b64_table;
        byte lPadding;
        if (encodingType == 'u') {
            if (charCount >= lTranslationTable.length) {
                throw runtime.newArgumentError(
                    ""
                        + charCount
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

    /**
     * encodes a String with the Quoted printable, MIME encoding (see RFC2045).
     * appends the result of the encoding in a StringBuffer
     * @param io2Append The StringBuffer which should receive the result
     * @param i2Encode The String to encode
     * @param iLength The max number of characters to encode
     * @return the io2Append buffer
     **/
    private static ByteList qpencode(ByteList io2Append, ByteList i2Encode, int iLength) {
        io2Append.ensure(1024);
        int lCurLineLength = 0;
        int lPrevChar = -1;
        byte[] l2Encode = i2Encode.getUnsafeBytes();
        try {
            int end = i2Encode.getBegin() + i2Encode.getRealSize();
            for (int i = i2Encode.getBegin(); i < end; i++) {
                int lCurChar = l2Encode[i] & 0xff;
                if (lCurChar > 126 || (lCurChar < 32 && lCurChar != '\n' && lCurChar != '\t') || lCurChar == '=') {
                    io2Append.append('=');
                    io2Append.append(hex_table[lCurChar >>> 4]);
                    io2Append.append(hex_table[lCurChar & 0x0f]);
                    lCurLineLength += 3;
                    lPrevChar = -1;
                } else if (lCurChar == '\n') {
                    if (lPrevChar == ' ' || lPrevChar == '\t') {
                        io2Append.append('=');
                        io2Append.append(lCurChar);
                    }
                    io2Append.append(lCurChar);
                    lCurLineLength = 0;
                    lPrevChar = lCurChar;
                } else {
                    io2Append.append(lCurChar);
                    lCurLineLength++;
                    lPrevChar = lCurChar;
                }
                if (lCurLineLength > iLength) {
                    io2Append.append('=');
                    io2Append.append('\n');
                    lCurLineLength = 0;
                    lPrevChar = '\n';
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //normal exit, this should be faster than a test at each iterations for string with more than
            //about 40 char
        }

        if (lCurLineLength > 0) {
            io2Append.append('=');
            io2Append.append('\n');
        }
        return io2Append;
    }

    /**
     *    Decodes <i>str</i> (which may contain binary data) according to the format
     *       string, returning an array of each value extracted.
     *       The format string consists of a sequence of single-character directives.<br/>
     *       Each directive may be followed by a number, indicating the number of times to repeat with this directive.  An asterisk (``<code>*</code>'') will use up all
     *       remaining elements.  <br/>
     *       The directives <code>sSiIlL</code> may each be followed by an underscore (``<code>_</code>'') to use the underlying platform's native size for the specified type; otherwise, it uses a platform-independent consistent size.  <br/>
     *       Spaces are ignored in the format string.
     * 
     *       <table border="2" width="500" bgcolor="#ffe0e0">
     *           <tr>
     *             <td>
     * <P></P>
     *         <b>Directives for <a href="ref_c_string.html#String.unpack">
     *                   <code>String#unpack</code>
     *                 </a>
     *               </b>        <table class="codebox" cellspacing="0" border="0" cellpadding="3">
     * <tr bgcolor="#ff9999">
     *   <td valign="top">
     *                     <b>Format</b>
     *                   </td>
     *   <td valign="top">
     *                     <b>Function</b>
     *                   </td>
     *   <td valign="top">
     *                     <b>Returns</b>
     *                   </td>
     * </tr>
     * <tr>
     *   <td valign="top">A</td>
     *   <td valign="top">String with trailing nulls and spaces removed.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">a</td>
     *   <td valign="top">String.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">B</td>
     *   <td valign="top">Extract bits from each character (msb first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">b</td>
     *   <td valign="top">Extract bits from each character (lsb first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">C</td>
     *   <td valign="top">Extract a character as an unsigned integer.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">c</td>
     *   <td valign="top">Extract a character as an integer.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">d</td>
     *   <td valign="top">Treat <em>sizeof(double)</em> characters as a native
     *           double.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">E</td>
     *   <td valign="top">Treat <em>sizeof(double)</em> characters as a double in
     *           little-endian byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">e</td>
     *   <td valign="top">Treat <em>sizeof(float)</em> characters as a float in
     *           little-endian byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">f</td>
     *   <td valign="top">Treat <em>sizeof(float)</em> characters as a native float.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">G</td>
     *   <td valign="top">Treat <em>sizeof(double)</em> characters as a double in
     *           network byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">g</td>
     *   <td valign="top">Treat <em>sizeof(float)</em> characters as a float in
     *           network byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">H</td>
     *   <td valign="top">Extract hex nibbles from each character (most
     *           significant first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">h</td>
     *   <td valign="top">Extract hex nibbles from each character (least
     *           significant first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">I</td>
     *   <td valign="top">Treat <em>sizeof(int)</em>
     *                     <sup>1</sup> successive
     *           characters as an unsigned native integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">i</td>
     *   <td valign="top">Treat <em>sizeof(int)</em>
     *                     <sup>1</sup> successive
     *           characters as a signed native integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">L</td>
     *   <td valign="top">Treat four<sup>1</sup> successive
     *           characters as an unsigned native
     *           long integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">l</td>
     *   <td valign="top">Treat four<sup>1</sup> successive
     *           characters as a signed native
     *           long integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">M</td>
     *   <td valign="top">Extract a quoted-printable string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">m</td>
     *   <td valign="top">Extract a base64 encoded string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">N</td>
     *   <td valign="top">Treat four characters as an unsigned long in network
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">n</td>
     *   <td valign="top">Treat two characters as an unsigned short in network
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">P</td>
     *   <td valign="top">Treat <em>sizeof(char *)</em> characters as a pointer, and
     *           return <em>len</em> characters from the referenced location.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">p</td>
     *   <td valign="top">Treat <em>sizeof(char *)</em> characters as a pointer to a
     *           null-terminated string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">S</td>
     *   <td valign="top">Treat two<sup>1</sup> successive characters as an unsigned
     *           short in
     *           native byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">s</td>
     *   <td valign="top">Treat two<sup>1</sup> successive
     *           characters as a signed short in
     *           native byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">U</td>
     *   <td valign="top">Extract UTF-8 characters as unsigned integers.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">u</td>
     *   <td valign="top">Extract a UU-encoded string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">V</td>
     *   <td valign="top">Treat four characters as an unsigned long in little-endian
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">v</td>
     *   <td valign="top">Treat two characters as an unsigned short in little-endian
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">X</td>
     *   <td valign="top">Skip backward one character.</td>
     *   <td valign="top">---</td>
     * </tr>
     * <tr>
     *   <td valign="top">x</td>
     *   <td valign="top">Skip forward one character.</td>
     *   <td valign="top">---</td>
     * </tr>
     * <tr>
     *   <td valign="top">Z</td>
     *   <td valign="top">String with trailing nulls removed.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">@</td>
     *   <td valign="top">Skip to the offset given by the length argument.</td>
     *   <td valign="top">---</td>
     * </tr>
     * <tr>
     *                   <td colspan="9" bgcolor="#ff9999" height="2"><img src="dot.gif" width="1" height="1"></td>
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
    public static RubyArray unpack(Ruby runtime, ByteList encodedString, ByteList formatString) {
        Encoding encoding = encodedString.getEncoding();
        RubyArray result = runtime.newArray();
        // FIXME: potentially could just use ByteList here?
        ByteBuffer format = ByteBuffer.wrap(formatString.getUnsafeBytes(), formatString.begin(), formatString.length());
        ByteBuffer encode = ByteBuffer.wrap(encodedString.getUnsafeBytes(), encodedString.begin(), encodedString.length());
        int type = 0;
        int next = safeGet(format);

        mainLoop: while (next != 0) {
            type = next;
            next = safeGet(format);
            if (UNPACK_IGNORE_NULL_CODES.indexOf(type) != -1 && next == 0) {
                next = safeGetIgnoreNull(format);
            }
            
            if (type == '#') {
                while (type != '\n') {
                    if (next == 0) break mainLoop;
                    type = next;
                    next = safeGet(format);
                }
            }

            // Next indicates to decode using native encoding format
            if (next == '_' || next == '!') {
                int index = NATIVE_CODES.indexOf(type);
                if (index == -1) {
                    throw runtime.newArgumentError("'" + next +
                            "' allowed only after types " + NATIVE_CODES);
                }
                type = MAPPED_CODES.charAt(index);
                
                next = safeGet(format);
            }
            
            if (next == '>' || next == '<') {
                next = next == '>' ? BE : LE;
                int index = ENDIANESS_CODES.indexOf(type + next);
                if (index == -1) {
                    throw runtime.newArgumentError("'" + (char)next +
                            "' allowed only after types sSiIlLqQ");
                }
                type = ENDIANESS_CODES.charAt(index);
                next = safeGet(format);
                
                if (next == '_' || next == '!') next = safeGet(format);
            }

            // How many occurrences of 'type' we want
            int occurrences = 0;
            if (next == 0) {
                occurrences = 1;
            } else {
                if (next == '*') {
                    occurrences = IS_STAR;
                    next = safeGet(format);
                } else if (ASCII.isDigit(next)) {
                    occurrences = 0;
                    do {
                        occurrences = occurrences * 10 + Character.digit((char)(next & 0xFF), 10);
                        next = safeGet(format);
                    } while (next != 0 && ASCII.isDigit(next));
                } else {
                    occurrences = type == '@' ? 0 : 1;
                }
            }

            // See if we have a converter for the job...
            Converter converter = converters[type];
            if (converter != null) {
                decode(runtime, encode, occurrences, result, converter);
                type = next;
                continue;
            }

            // Otherwise the unpack should be here...
            switch (type) {
                case '@' :
                    try {
                        if (occurrences == IS_STAR) {
                            encode.position(encodedString.begin() + encode.remaining());
                        } else {
                            encode.position(encodedString.begin() + occurrences);
                        }
                    } catch (IllegalArgumentException iae) {
                        throw runtime.newArgumentError("@ outside of string");
                    }
                    break;
                case '%' :
                    throw runtime.newArgumentError("% is not supported");
                case 'A' :
                    {
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

                    result.append(RubyString.newString(runtime, new ByteList(potential, 0, occurrences, encoding, false)));
                    }
                    break;
                case 'Z' :
                    {
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

                        result.append(RubyString.newString(runtime, new ByteList(potential, 0, t, encoding, false)));

                        // In case when the number of occurences is
                        // explicitly specified, we have to read up
                        // the remaining garbage after the '\0' to
                        // satisfy the requested pattern.
                        if (!isStar) {
                            if (t < occurrences) {
                                // We encountered '\0' when
                                // were reading the buffer above,
                                // increment the number of read bytes.
                                t++;
                            }

                            while (t < occurrences) {
                                encode.get();
                                t++;
                            }
                        }
                    }
                    break;
                case 'a' :
                    if (occurrences == IS_STAR || occurrences > encode.remaining()) {
                        occurrences = encode.remaining();
                    }
                    byte[] potential = new byte[occurrences];
                    encode.get(potential);
                    result.append(RubyString.newString(runtime, new ByteList(potential, encoding, false)));
                    break;
                case 'b' :
                    {
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
                        result.append(RubyString.newString(runtime, new ByteList(lElem, encoding, false)));
                    }
                    break;
                case 'B' :
                    {
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

                        result.append(RubyString.newString(runtime, new ByteList(lElem, encoding, false)));
                    }
                    break;
                case 'h' :
                    {
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
                        result.append(RubyString.newString(runtime, new ByteList(lElem, encoding, false)));
                    }
                    break;
                case 'H' :
                    {
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
                        result.append(RubyString.newString(runtime, new ByteList(lElem, encoding, false)));
                    }
                    break;

                case 'u':
                {
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
                                encode.position(encode.position() - 1);
                            }
                        }
                    }
                    result.append(RubyString.newString(runtime, new ByteList(lElem, 0, index, encoding, false)));
                }
                break;

                case 'm':
                {
                    int length = encode.remaining()*3/4;
                    byte[] lElem = new byte[length];
                    int a = -1, b = -1, c = 0, d;
                    int index = 0;
                    int s = -1;

                    if (occurrences == 0){
                        if (encode.remaining()%4 != 0) {
                            throw runtime.newArgumentError("invalid base64");
                        }
                        while (encode.hasRemaining() && s != '=') {
                            a = b = c = -1;
                            d = -2;

                            // obtain a
                            s = safeGet(encode);
                            a = b64_xtable[s];
                            if (a == -1) throw runtime.newArgumentError("invalid base64");

                            // obtain b
                            s = safeGet(encode);
                            b = b64_xtable[s];
                            if (b == -1) throw runtime.newArgumentError("invalid base64");

                            // obtain c
                            s = safeGet(encode);
                            c = b64_xtable[s];
                            if (s == '=') {
                                if (safeGet(encode) != '=') throw runtime.newArgumentError("invalid base64");
                                break;
                            }
                            if (c == -1) throw runtime.newArgumentError("invalid base64");

                            // obtain d
                            s = safeGet(encode);
                            d = b64_xtable[s];
                            if (s == '=') break;
                            if (d == -1) throw runtime.newArgumentError("invalid base64");

                            // calculate based on a, b, c and d
                            lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                            lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                            lElem[index++] = (byte)((c << 6 | d) & 255);
                        }

                        if (encode.hasRemaining()) throw runtime.newArgumentError("invalid base64");

                        if (a != -1 && b != -1) {
                            if (c == -1 && s == '=') {
                                if ((b & 15) > 0) throw runtime.newArgumentError("invalid base64");
                                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                            } else if(c != -1 && s == '=') {
                                if ((c & 3) > 0) throw runtime.newArgumentError("invalid base64");
                                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                                lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                            }
                        }
                    }
                    else {

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
                                    encode.position(encode.position() - 1);
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
                                    encode.position(encode.position() - 1);
                                }
                                break;
                            }

                            // calculate based on a, b, c and d
                            lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                            lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                            lElem[index++] = (byte)((c << 6 | d) & 255);
                        }

                        if (a != -1 && b != -1) {
                            if (c == -1 && s == '=') {
                                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                            } else if(c != -1 && s == '=') {
                                lElem[index++] = (byte)((a << 2 | b >> 4) & 255);
                                lElem[index++] = (byte)((b << 4 | c >> 2) & 255);
                            }
                        }
                    }
                    result.append(RubyString.newString(runtime, new ByteList(lElem, 0, index,
                            ASCIIEncoding.INSTANCE, false)));
                }
                break;

                case 'M' :
                    {
                        byte[] lElem = new byte[Math.max(encode.remaining(),0)];
                        int index = 0;
                        for(;;) {
                            if (!encode.hasRemaining()) break;
                            int c = safeGet(encode);
                            if (c != '=') {
                                lElem[index++] = (byte)c;
                            } else {
                                if (!encode.hasRemaining()) break;
                                encode.mark();
                                int c1 = safeGet(encode);
                                if (c1 == '\n' || (c1 == '\r' && (c1 = safeGet(encode)) == '\n')) continue;
                                int d1 = Character.digit(c1, 16);
                                if (d1 == -1) {
                                    encode.reset();
                                    break;
                                }
                                encode.mark();
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
                        result.append(RubyString.newString(runtime, new ByteList(lElem, 0, index,
                                ASCIIEncoding.INSTANCE, false)));
                    }
                    break;
                case 'U' :
                    {
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
                                result.append(
                                        runtime.newFixnum(utf8Decode(encode)));
                            } catch (IllegalArgumentException e) {
                                throw runtime.newArgumentError(e.getMessage());
                            }
                        }
                    }
                    break;
                 case 'X':
                     if (occurrences == IS_STAR) {
                         // MRI behavior: Contrary to what seems to be logical,
                         // when '*' is given, MRI calculates the distance
                         // to the end, in order to go backwards.
                         occurrences = /*encode.limit() - */encode.remaining();
                     }

                     try {
                         encode.position(encode.position() - occurrences);
                     } catch (IllegalArgumentException e) {
                         throw runtime.newArgumentError("in `unpack': X outside of string");
                     }
                     break;
                 case 'x':
                      if (occurrences == IS_STAR) {
                           occurrences = encode.remaining();
                      }

                      try {
                          encode.position(encode.position() + occurrences);
                      } catch (IllegalArgumentException e) {
                          throw runtime.newArgumentError("in `unpack': x outside of string");
                      }

                     break;
                case 'w':
                    if (occurrences == IS_STAR || occurrences > encode.remaining()) {
                        occurrences = encode.remaining();
                    }

                    long ul = 0;
                    long ulmask = (0xfe << 56) & 0xffffffff;
                    RubyBignum big128 = RubyBignum.newBignum(runtime, 128);
                    int pos = encode.position();

                    while (occurrences > 0 && pos < encode.limit()) {
                        ul <<= 7;
                        ul |= encode.get(pos) & 0x7f;
                        if((encode.get(pos++) & 0x80) == 0) {
                            result.append(RubyFixnum.newFixnum(runtime, ul));
                            occurrences--;
                            ul = 0;
                        } else if((ul & ulmask) == 0) {
                            RubyBignum big = RubyBignum.newBignum(runtime, ul);
                            while(occurrences > 0 && pos < encode.limit()) {
                                IRubyObject mulResult = big.op_mul(runtime.getCurrentContext(), big128);
                                IRubyObject v = mulResult.callMethod(runtime.getCurrentContext(), "+",
                                        RubyBignum.newBignum(runtime, encode.get(pos) & 0x7f));
                                if(v instanceof RubyFixnum) {
                                    big = RubyBignum.newBignum(runtime, RubyNumeric.fix2long(v));
                                } else if (v instanceof RubyBignum) {
                                    big = (RubyBignum)v;
                                }
                                if((encode.get(pos++) & 0x80) == 0) {
                                    result.add(RubyBignum.bignorm(runtime, big.getValue()));
                                    occurrences--;
                                    ul = 0;
                                    break;
                                }
                            }
                        }
                    }
                    try {
                        encode.position(pos);
                    } catch (IllegalArgumentException e) {
                        throw runtime.newArgumentError("in `unpack': poorly encoded input");
                    }
                    break;
            }
        }
        return result;
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

    private static int safeGet(ByteBuffer encode) {
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

    public static void decode(Ruby runtime, ByteBuffer encode, int occurrences,
            RubyArray result, Converter converter) {
        int lPadLength = 0;

        if (occurrences == IS_STAR) {
            occurrences = encode.remaining() / converter.size;
        } else if (occurrences > encode.remaining() / converter.size) {
            lPadLength = occurrences - encode.remaining() / converter.size;
            occurrences = encode.remaining() / converter.size;
        }
        for (; occurrences-- > 0;) {
            result.append(converter.decode(runtime, encode));
        }

        for (; lPadLength-- > 0;)
            result.append(runtime.getNil());
    }

    public static int encode(Ruby runtime, int occurrences, ByteList result,
            RubyArray list, int index, ConverterExecutor converter) {
        int listSize = list.size();

        while (occurrences-- > 0) {
            if (listSize-- <= 0 || index >= list.size()) {
                throw runtime.newArgumentError(sTooFew);
            }

            IRubyObject from = list.eltInternal(index++);

            converter.encode(runtime, from, result);
        }

        return index;
    }

    private abstract static class ConverterExecutor {
        protected Converter converter;
        public void setConverter(Converter converter) {
            this.converter = converter;
        }

        public abstract IRubyObject decode(Ruby runtime, ByteBuffer format);
        public abstract void encode(Ruby runtime, IRubyObject from, ByteList result);
    }

    private static ConverterExecutor executor() {
        return new ConverterExecutor() {
            @Override
            public IRubyObject decode(Ruby runtime, ByteBuffer format) {
                return converter.decode(runtime, format);
            }

            @Override
            public void encode(Ruby runtime, IRubyObject from, ByteList result) {
                if (from == runtime.getNil() && converter.getType() != null) throw runtime.newTypeError(from, converter.getType());                
                converter.encode(runtime, from, result);
            }
        };
    }

    public abstract static class Converter {
        public int size;
        public String type;

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

        public abstract IRubyObject decode(Ruby runtime, ByteBuffer format);
        public abstract void encode(Ruby runtime, IRubyObject from, ByteList result);
    }

    private abstract static class QuadConverter extends Converter{
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

    /**
     * Same as pack but defaults tainting of output to false.
     */
    public static RubyString pack(Ruby runtime, RubyArray list, ByteList formatString) {
        return packCommon(runtime, list, formatString, false, executor());
    }

    public static RubyString pack(ThreadContext context, Ruby runtime, RubyArray list, RubyString formatString) {
        RubyString pack = packCommon(runtime, list, formatString.getByteList(), formatString.isTaint(), executor());
        return (RubyString) pack.infectBy(formatString);
    }

    private static RubyString packCommon(Ruby runtime, RubyArray list, ByteList formatString, boolean tainted, ConverterExecutor executor) {
        ByteBuffer format = ByteBuffer.wrap(formatString.getUnsafeBytes(), formatString.begin(), formatString.length());
        ByteList result = new ByteList();
        boolean taintOutput = tainted;
        int listSize = list.size();
        int type = 0;
        int next = safeGet(format);

        int idx = 0;
        ByteList lCurElemString;

        int enc_info = 1;

        mainLoop: while (next != 0) {
            type = next;
            next = safeGet(format);
            if (PACK_IGNORE_NULL_CODES.indexOf(type) != -1 && next == 0) {
                next = safeGetIgnoreNull(format);
            }

            // Skip all whitespace in pack format string
            while (ASCII.isSpace(type)) {
                if (next == 0) break mainLoop;
                type = next;
                next = safeGet(format);
            }

            // Skip embedded comments in pack format string
            if (type == '#') {
                while (type != '\n') {
                    if (next == 0) break mainLoop;
                    type = next;
                    next = safeGet(format);
                }
            }

            if (next == '!' || next == '_') {
                int index = NATIVE_CODES.indexOf(type);
                if (index == -1) {
                    throw runtime.newArgumentError("'" + next +
                            "' allowed only after types " + NATIVE_CODES);
                }
                int typeBeforeMap = type;
                type = MAPPED_CODES.charAt(index);

                next = safeGet(format);
                if (PACK_IGNORE_NULL_CODES_WITH_MODIFIERS.indexOf(typeBeforeMap) != -1 && next == 0) {
                    next = safeGetIgnoreNull(format);
                }
            }
            
            if (next == '>' || next == '<') {
                next = next == '>' ? BE : LE;
                int index = ENDIANESS_CODES.indexOf(type + next);
                if (index == -1) {
                    throw runtime.newArgumentError("'" + (char)next +
                            "' allowed only after types sSiIlLqQ");
                }
                type = ENDIANESS_CODES.charAt(index);
                next = safeGet(format);
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
                        occurrences = list.size() - idx;
                        isStar = true;
                    }
                    next = safeGet(format);
                } else if (ASCII.isDigit(next)) {
                    occurrences = 0;
                    do {
                        occurrences = occurrences * 10 + Character.digit((char)(next & 0xFF), 10);
                        next = safeGet(format);
                    } while (next != 0 && ASCII.isDigit(next));
                }
            }

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

            Converter converter = converters[type];

            if (converter != null) {
                executor.setConverter(converter);
                idx = encode(runtime, occurrences, result, list, idx, executor);
                continue;
            }

            switch (type) {
                case '%' :
                    throw runtime.newArgumentError("% is not supported");
                case 'A' :
                case 'a' :
                case 'Z' :
                case 'B' :
                case 'b' :
                case 'H' :
                case 'h' :
                    {
                        if (listSize-- <= 0) {
                            throw runtime.newArgumentError(sTooFew);
                        }

                        IRubyObject from = list.eltInternal(idx++);
                        if(from.isTaint()) taintOutput = true;

                        lCurElemString = from == runtime.getNil() ? ByteList.EMPTY_BYTELIST : from.convertToString().getByteList();

                        if (isStar) {
                            occurrences = lCurElemString.length();
                            // 'Z' adds extra null pad (versus 'a')
                            if (type == 'Z') occurrences++;
                        }

                        switch (type) {
                            case 'a' :
                            case 'A' :
                            case 'Z' :
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
                            break;
                            case 'b' :
                                {
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
                            break;
                            case 'B' :
                                {
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
                            break;
                            case 'h' :
                                {
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
                                        if(padLength > 0) {
                                            padLength--;
                                        }
                                    }

                                    result.length(result.length() + padLength / 2);
                                }
                            break;
                            case 'H' :
                                {
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
                                        if(padLength > 0) {
                                            padLength--;
                                        }
                                    }

                                    result.length(result.length() + padLength / 2);
                                }
                            break;
                        }
                        break;
                    }

                case 'x' :
                    grow(result, sNil10, occurrences);
                    break;
                case 'X' :
                    try {
                        shrink(result, occurrences);
                    } catch (IllegalArgumentException e) {
                        throw runtime.newArgumentError("in `pack': X outside of string");
                    }
                    break;
                case '@' :
                    occurrences -= result.length();
                    if (occurrences > 0) {
                        grow(result, sNil10, occurrences);
                    }
                    occurrences = -occurrences;
                    if (occurrences > 0) {
                        shrink(result, occurrences);
                    }
                    break;
                case 'u' :
                case 'm' : {
                        if (listSize-- <= 0) throw runtime.newArgumentError(sTooFew);

                        IRubyObject from = list.eltInternal(idx++);
                        if (from == runtime.getNil()) throw runtime.newTypeError(from, "Integer");
                        lCurElemString = from.convertToString().getByteList();
                        if (occurrences == 0 && type == 'm' && !ignoreStar) {
                            encodes(runtime, result, lCurElemString.getUnsafeBytes(),
                                    lCurElemString.getBegin(), lCurElemString.length(),
                                    lCurElemString.length(), (byte)type, false);
                            break;
                        }

                        occurrences = occurrences <= 2 ? 45 : occurrences / 3 * 3;
                        if (lCurElemString.length() == 0) break;

                        byte[] charsToEncode = lCurElemString.getUnsafeBytes();
                        for (int i = 0; i < lCurElemString.length(); i += occurrences) {
                            encodes(runtime, result, charsToEncode,
                                    i + lCurElemString.getBegin(), lCurElemString.length() - i,
                                    occurrences, (byte)type, true);
                        }
                    }
                    break;
                case 'M' : {
                       if (listSize-- <= 0) throw runtime.newArgumentError(sTooFew);

                       IRubyObject from = list.eltInternal(idx++);
                       lCurElemString = from == runtime.getNil() ? ByteList.EMPTY_BYTELIST : from.asString().getByteList();

                       if (occurrences <= 1) {
                           occurrences = 72;
                       }

                       qpencode(result, lCurElemString, occurrences);
                    }
                    break;
                case 'U' :
                    while (occurrences-- > 0) {
                        if (listSize-- <= 0) throw runtime.newArgumentError(sTooFew);

                        IRubyObject from = list.eltInternal(idx++);
                        int code = from == runtime.getNil() ? 0 : RubyNumeric.num2int(from);

                        if (code < 0) throw runtime.newRangeError("pack(U): value out of range");

                        result.ensure(result.getRealSize() + 6);
                        result.setRealSize(result.getRealSize() + utf8Decode(runtime, result.getUnsafeBytes(), result.getBegin() + result.getRealSize(), code));
                    }
                    break;
                case 'w' :
                    while (occurrences-- > 0) {
                        if (listSize-- <= 0) throw runtime.newArgumentError(sTooFew);

                        ByteList buf = new ByteList();
                        IRubyObject from = list.eltInternal(idx++);

                        if (from.isNil()) throw runtime.newTypeError("pack('w') does not take nil");


                        if (from instanceof RubyBignum) {
                            RubyBignum big128 = RubyBignum.newBignum(runtime, 128);
                            while (from instanceof RubyBignum) {
                                RubyBignum bignum = (RubyBignum)from;
                                RubyArray ary = (RubyArray)bignum.divmod(runtime.getCurrentContext(), big128);
                                buf.append((byte)(RubyNumeric.fix2int(ary.at(RubyFixnum.one(runtime))) | 0x80) & 0xff);
                                from = ary.at(RubyFixnum.zero(runtime));
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
                            throw runtime.newArgumentError("can't compress negative numbers");
                        }
                    }

                    break;
            }
        }        

        RubyString output = runtime.newString(result);
        if (taintOutput) output.taint(runtime.getCurrentContext());

        switch (enc_info)
        {
            case 1:
                output.setEncodingAndCodeRange(USASCII, RubyObject.USER8_F);
                break;
            case 2:
                output.force_encoding(runtime.getCurrentContext(),
                        runtime.getEncodingService().convertEncodingToRubyEncoding(UTF8));
                break;
            default:
                /* do nothing, keep ASCII-8BIT */
        }

        return output;
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
        result.append((byte) ((s>>16) & 0xff)).append((byte) ((s>>24) &0xff));
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
