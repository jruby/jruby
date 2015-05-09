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

import static org.jcodings.Encoding.CHAR_INVALID;
import static org.jruby.RubyEnumerator.enumeratorize;

import org.jcodings.Encoding;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.constants.CharacterType;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.IntHash;
import org.joni.Matcher;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyEncoding;
import org.jruby.RubyIO;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.io.EncodingUtils;
import sun.misc.Unsafe;

import java.util.Arrays;

public final class StringSupport {
    public static final int CR_MASK      = RubyObject.USER0_F | RubyObject.USER1_F;
    public static final int CR_UNKNOWN   = 0;
    public static final int CR_7BIT      = RubyObject.USER0_F;
    public static final int CR_VALID     = RubyObject.USER1_F;
    public static final int CR_BROKEN    = RubyObject.USER0_F | RubyObject.USER1_F;

    public static final Object UNSAFE = getUnsafe();
    private static final int OFFSET = UNSAFE != null ? ((Unsafe)UNSAFE).arrayBaseOffset(byte[].class) : 0;
    public static final int TRANS_SIZE = 256;

    private static Object getUnsafe() {
        try {
            Class sunUnsafe = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field f = sunUnsafe.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return sun.misc.Unsafe.class.cast(f.get(sunUnsafe));
        } catch (Exception ex) {
            return null;
        }
    }

    public static String codeRangeAsString(int codeRange) {
        switch (codeRange) {
            case CR_UNKNOWN: return "unknown";
            case CR_7BIT: return "7bit";
            case CR_VALID: return "valid";
            case CR_BROKEN: return "broken";
        }

        return "???";  // Not reached unless something seriously boned
    }

    // rb_enc_fast_mbclen
    public static int encFastMBCLen(byte[] bytes, int p, int e, Encoding enc) {
        return enc.length(bytes, p, e);
    }

    // rb_enc_mbclen
    public static int length(Encoding enc, byte[]bytes, int p, int end) {
        int n = enc.length(bytes, p, end);
        if (MBCLEN_CHARFOUND_P(n) && MBCLEN_CHARFOUND_LEN(n) <= end - p) return MBCLEN_CHARFOUND_LEN(n);
        int min = enc.minLength();
        return min <= end - p ? min : end - p;
    }

    // rb_enc_precise_mbclen
    public static int preciseLength(Encoding enc, byte[]bytes, int p, int end) {
        if (p >= end) return -1 - (1);
        int n = enc.length(bytes, p, end);
        if (n > end - p) return MBCLEN_NEEDMORE(n - (end - p));
        return n;
    }

    // MBCLEN_NEEDMORE_P, ONIGENC_MBCLEN_NEEDMORE_P
    public static boolean MBCLEN_NEEDMORE_P(int r) {
        return r < -1;
    }

    // MBCLEN_NEEDMORE, ONIGENC_MBCLEN_NEEDMORE
    public static int MBCLEN_NEEDMORE(int n) {
        return -1 - n;
    }

    // MBCLEN_INVALID_P, ONIGENC_MBCLEN_INVALID_P
    public static boolean MBCLEN_INVALID_P(int r) {
        return r == -1;
    }

    // MBCLEN_CHARFOUND_LEN, ONIGENC_MBCLEN_CHARFOUND_LEN
    public static int MBCLEN_CHARFOUND_LEN(int r) {
        return r;
    }

    // MBCLEN_CHARFOUND_P, ONIGENC_MBCLEN_CHARFOUND_P
    public static boolean MBCLEN_CHARFOUND_P(int r) {
        return 0 < r;
    }

    // CONSTRUCT_MBCLEN_CHARFOUND, ONIGENC_CONSTRUCT_MBCLEN_CHARFOUND
    public static int CONSTRUCT_MBCLEN_CHARFOUND(int n) {
        return n;
    }

    // MRI: search_nonascii
    public static int searchNonAscii(byte[]bytes, int p, int end) {
        while (p < end) {
            if (!Encoding.isAscii(bytes[p])) return p;
            p++;
        }
        return -1;
    }

    public static int searchNonAscii(ByteList bytes) {
        return searchNonAscii(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    public static int codeRangeScan(Encoding enc, byte[]bytes, int p, int len) {
        if (enc == ASCIIEncoding.INSTANCE) {
            return searchNonAscii(bytes, p, p + len) != -1 ? CR_VALID : CR_7BIT;
        }
        if (enc.isAsciiCompatible()) {
            return codeRangeScanAsciiCompatible(enc, bytes, p, len);
        }
        return codeRangeScanNonAsciiCompatible(enc, bytes, p, len);
    }

    private static int codeRangeScanAsciiCompatible(Encoding enc, byte[]bytes, int p, int len) {
        int end = p + len;
        p = searchNonAscii(bytes, p, end);
        if (p == -1) return CR_7BIT;

        while (p < end) {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl <= 0) return CR_BROKEN;
            p += cl;
            if (p < end) {
                p = searchNonAscii(bytes, p, end);
                if (p == -1) return CR_VALID;
            }
        }
        return p > end ? CR_BROKEN : CR_VALID;
    }

    private static int codeRangeScanNonAsciiCompatible(Encoding enc, byte[]bytes, int p, int len) {
        int end = p + len;
        while (p < end) {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl <= 0) return CR_BROKEN;
            p += cl;
        }
        return p > end ? CR_BROKEN : CR_VALID;
    }

    public static int codeRangeScan(Encoding enc, ByteList bytes) {
        return codeRangeScan(enc, bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
    }

    public static long codeRangeScanRestartable(Encoding enc, byte[]bytes, int s, int end, int cr) {
        if (cr == CR_BROKEN) return pack(end - s, cr);
        int p = s;

        if (enc == ASCIIEncoding.INSTANCE) {
            return pack(end - s, searchNonAscii(bytes, p, end) == -1 && cr != CR_VALID ? CR_7BIT : CR_VALID);
        } else if (enc.isAsciiCompatible()) {
            p = searchNonAscii(bytes, p, end);
            if (p == -1) return pack(end - s, cr != CR_VALID ? CR_7BIT : cr);

            while (p < end) {
                int cl = preciseLength(enc, bytes, p, end);
                if (cl <= 0) return pack(p - s, cl == CHAR_INVALID ? CR_BROKEN : CR_UNKNOWN);
                p += cl;

                if (p < end) {
                    p = searchNonAscii(bytes, p, end);
                    if (p == -1) return pack(end - s, CR_VALID);
                }
            }
        } else {
            while (p < end) {
                int cl = preciseLength(enc, bytes, p, end);
                if (cl <= 0) return pack(p - s, cl == CHAR_INVALID ? CR_BROKEN: CR_UNKNOWN);
                p += cl;
            }
        }
        return pack(p - s, p > end ? CR_BROKEN : CR_VALID);
    }

    private static final long NONASCII_MASK = 0x8080808080808080L;
    private static int countUtf8LeadBytes(long d) {
        d |= ~(d >>> 1);
        d >>>= 6;
        d &= NONASCII_MASK >>> 7;
        d += (d >>> 8);
        d += (d >>> 16);
        d += (d >>> 32);
        return (int)(d & 0xf);
    }

    private static final int LONG_SIZE = 8;
    private static final int LOWBITS = LONG_SIZE - 1;
    @SuppressWarnings("deprecation")
    public static int utf8Length(byte[]bytes, int p, int end) {
        int len = 0;
        if (UNSAFE != null) {
            if (end - p > LONG_SIZE * 2) {
                int ep = ~LOWBITS & (p + LOWBITS);
                while (p < ep) {
                    if ((bytes[p++] & 0xc0 /*utf8 lead byte*/) != 0x80) len++;
                }
                Unsafe us = (Unsafe)UNSAFE;
                int eend = ~LOWBITS & end;
                while (p < eend) {
                    len += countUtf8LeadBytes(us.getLong(bytes, (long)(OFFSET + p)));
                    p += LONG_SIZE;
                }
            }
        }
        while (p < end) {
            if ((bytes[p++] & 0xc0 /*utf8 lead byte*/) != 0x80) len++;
        }
        return len;
    }

    public static int utf8Length(ByteList bytes) {
        return utf8Length(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    // MRI: rb_enc_strlen
    public static int strLength(Encoding enc, byte[]bytes, int p, int end) {
        return strLength(enc, bytes, p, end, CR_UNKNOWN);
    }

    // MRI: enc_strlen
    public static int strLength(Encoding enc, byte[]bytes, int p, int e, int cr) {
        int c;
        if (enc.isFixedWidth()) {
            return (e - p + enc.minLength() - 1) / enc.minLength();
        } else if (enc.isAsciiCompatible()) {
            c = 0;
            if (cr == CR_7BIT || cr == CR_VALID) {
                while (p < e) {
                    if (Encoding.isAscii(bytes[p])) {
                        int q = searchNonAscii(bytes, p, e);
                        if (q == -1) return c + (e - p);
                        c += q - p;
                        p = q;
                    }
                    p += encFastMBCLen(bytes, p, e, enc);
                    c++;
                }
            } else {
                while (p < e) {
                    if (Encoding.isAscii(bytes[p])) {
                        int q = searchNonAscii(bytes, p, e);
                        if (q == -1) return c + (e - p);
                        c += q - p;
                        p = q;
                    }
                    p += length(enc, bytes, p, e);
                    c++;
                }
            }
            return c;
        }

        for (c = 0; p < e; c++) {
            p += length(enc, bytes, p, e);
        }
        return c;
    }

    public static int strLength(ByteList bytes) {
        return strLength(bytes.getEncoding(), bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    public static long strLengthWithCodeRange(Encoding enc, byte[]bytes, int p, int end) {
        if (enc.isFixedWidth()) {
            return (end - p + enc.minLength() - 1) / enc.minLength();
        } else if (enc.isAsciiCompatible()) {
            return strLengthWithCodeRangeAsciiCompatible(enc, bytes, p, end);
        } else {
            return strLengthWithCodeRangeNonAsciiCompatible(enc, bytes, p, end);
        }
    }

    private static long strLengthWithCodeRangeAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
        int cr = 0, c = 0;
        while (p < end) {
            if (Encoding.isAscii(bytes[p])) {
                int q = searchNonAscii(bytes, p, end);
                if (q == -1) return pack(c + (end - p), cr == 0 ? CR_7BIT : cr);
                c += q - p;
                p = q;
            }
            int cl = preciseLength(enc, bytes, p, end);
            if (cl > 0) {
                cr |= CR_VALID;
                p += cl;
            } else {
                cr = CR_BROKEN;
                p++;
            }
            c++;
        }
        return pack(c, cr == 0 ? CR_7BIT : cr);
    }

    private static long strLengthWithCodeRangeNonAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
        int cr = 0, c = 0;
        for (c = 0; p < end; c++) {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl > 0) {
                cr |= CR_VALID;
                p += cl;
            } else {
                cr = CR_BROKEN;
                p++;
            }
        }
        return pack(c, cr == 0 ? CR_7BIT : cr);
    }

    public static long strLengthWithCodeRange(ByteList bytes) {
        return strLengthWithCodeRange(bytes.getEncoding(), bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    public static long strLengthWithCodeRange(ByteList bytes, Encoding enc) {
        return strLengthWithCodeRange(enc, bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    // arg cannot be negative
    static long pack(int result, int arg) {
        return ((long)arg << 31) | result;
    }

    public static int unpackResult(long len) {
        return (int)len & 0x7fffffff;
    }

    public static int unpackArg(long cr) {
        return (int)(cr >>> 31);
    }

    public static int codePoint(Ruby runtime, Encoding enc, byte[]bytes, int p, int end) {
        if (p >= end) throw runtime.newArgumentError("empty string");
        int cl = preciseLength(enc, bytes, p, end);
        if (cl <= 0) throw runtime.newArgumentError("invalid byte sequence in " + enc);
        return enc.mbcToCode(bytes, p, end);
    }

    public static int codeLength(Encoding enc, int c) {
        return enc.codeToMbcLength(c);
    }

    public static long getAscii(Encoding enc, byte[]bytes, int p, int end) {
        return getAscii(enc, bytes, p, end, 0);
    }

    public static long getAscii(Encoding enc, byte[]bytes, int p, int end, int len) {
        if (p >= end) return pack(-1, len);

        if (enc.isAsciiCompatible()) {
            int c = bytes[p] & 0xff;
            if (!Encoding.isAscii(c)) return pack(-1, len);
            return pack(c, len == 0 ? 0 : 1);
        } else {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl <= 0) return pack(-1, len);
            int c = enc.mbcToCode(bytes, p, end);
            if (!Encoding.isAscii(c)) return pack(-1, len);
            return pack(c, len == 0 ? 0 : cl);
        }
    }

    public static int preciseCodePoint(Encoding enc, byte[]bytes, int p, int end) {
        int l = preciseLength(enc, bytes, p, end);
        if (l > 0) return enc.mbcToCode(bytes, p, end);
        return -1;
    }

    @SuppressWarnings("deprecation")
    public static int utf8Nth(byte[]bytes, int p, int e, int nth) {
        // FIXME: Missing our UNSAFE impl because it was doing the wrong thing: See GH #1986
        while (p < e) {
            if ((bytes[p] & 0xc0 /*utf8 lead byte*/) != 0x80) {
                if (nth == 0) break;
                nth--;
            }
            p++;
        }
        return p;
    }

    public static int nth(Encoding enc, byte[]bytes, int p, int end, int n) {
        return nth(enc, bytes, p, end, n, enc.isSingleByte());
    }

    /**
     * Get the position of the nth character in the given byte array, using the given encoding and range.
     *
     * @param enc encoding to use
     * @param bytes bytes to scan
     * @param p starting byte offset
     * @param end ending byte offset
     * @param n index of character for which to find byte offset
     * @param singlebyte whether the byte contents are in a single byte encoding
     * @return the offset of the nth character in the string, or -1 if nth is out of the string
     */
    public static int nth(Encoding enc, byte[]bytes, int p, int end, int n, boolean singlebyte) {
        if (singlebyte) {
            p += n;
        } else if (enc.isFixedWidth()) {
            p += n * enc.maxLength();
        } else if (enc.isAsciiCompatible()) {
            p = nthAsciiCompatible(enc, bytes, p, end, n);
        } else {
            p = nthNonAsciiCompatible(enc, bytes, p, end, n);
        }
        if (p < 0) return -1;
        return p > end ? end : p;
    }

    private static int nthAsciiCompatible(Encoding enc, byte[]bytes, int p, int end, int n) {
        while (p < end && n > 0) {
            int end2 = p + n;
            if (end < end2) return end;
            if (Encoding.isAscii(bytes[p])) {
                int p2 = searchNonAscii(bytes, p, end2);
                if (p2 == -1) return end2;
                n -= p2 - p;
                p = p2;
            }
            int cl = length(enc, bytes, p, end);
            p += cl;
            n--;
        }
        return n != 0 ? end : p;
    }

    private static int nthNonAsciiCompatible(Encoding enc, byte[]bytes, int p, int end, int n) {
        while (p < end && n-- != 0) {
            p += length(enc, bytes, p, end);
        }
        return p;
    }

    public static int utf8Offset(byte[]bytes, int p, int end, int n) {
        int pp = utf8Nth(bytes, p, end, n);
        return pp == -1 ? end - p : pp - p;
    }

    public static int offset(Encoding enc, byte[]bytes, int p, int end, int n) {
        int pp = nth(enc, bytes, p, end, n);
        return pp == -1 ? end - p : pp - p;
    }

    public static int offset(Encoding enc, byte[]bytes, int p, int end, int n, boolean singlebyte) {
        int pp = nth(enc, bytes, p, end, n, singlebyte);
        return pp == -1 ? end - p : pp - p;
    }

    public static int offset(RubyString str, int pos) {
        ByteList value = str.getByteList();
        return offset(str.getEncoding(), value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), pos);
    }

    public static int toLower(Encoding enc, int c) {
        return Encoding.isAscii(c) ? AsciiTables.ToLowerCaseTable[c] : c;
    }

    public static int toUpper(Encoding enc, int c) {
        return Encoding.isAscii(c) ? AsciiTables.ToUpperCaseTable[c] : c;
    }

    public static int caseCmp(byte[]bytes1, int p1, byte[]bytes2, int p2, int len) {
        int i = -1;
        for (; ++i < len && bytes1[p1 + i] == bytes2[p2 + i];) {}
        if (i < len) return (bytes1[p1 + i] & 0xff) > (bytes2[p2 + i] & 0xff) ? 1 : -1;
        return 0;
    }

    public static int scanHex(byte[]bytes, int p, int len) {
        return scanHex(bytes, p, len, ASCIIEncoding.INSTANCE);
    }

    public static int scanHex(byte[]bytes, int p, int len, Encoding enc) {
        int v = 0;
        int c;
        while (len-- > 0 && enc.isXDigit(c = bytes[p++] & 0xff)) {
            v = (v << 4) + enc.xdigitVal(c);
        }
        return v;
    }

    public static int hexLength(byte[]bytes, int p, int len) {
        return hexLength(bytes, p, len, ASCIIEncoding.INSTANCE);
    }

    public static int hexLength(byte[]bytes, int p, int len, Encoding enc) {
        int hlen = 0;
        while (len-- > 0 && enc.isXDigit(bytes[p++] & 0xff)) hlen++;
        return hlen;
    }

    public static int scanOct(byte[]bytes, int p, int len) {
        return scanOct(bytes, p, len, ASCIIEncoding.INSTANCE);
    }

    public static int scanOct(byte[]bytes, int p, int len, Encoding enc) {
        int v = 0;
        int c;
        while (len-- > 0 && enc.isDigit(c = bytes[p++] & 0xff) && c < '8') {
            v = (v << 3) + Encoding.digitVal(c);
        }
        return v;
    }

    public static int octLength(byte[]bytes, int p, int len) {
        return octLength(bytes, p, len, ASCIIEncoding.INSTANCE);
    }

    public static int octLength(byte[]bytes, int p, int len, Encoding enc) {
        int olen = 0;
        int c;
        while (len-- > 0 && enc.isDigit(c = bytes[p++] & 0xff) && c < '8') olen++;
        return olen;
    }

    /**
     * Check whether input object's string value contains a null byte, and if so
     * throw SecurityError.
     * @param runtime
     * @param value
     */
    public static final void checkStringSafety(Ruby runtime, IRubyObject value) {
        RubyString s = value.asString();
        ByteList bl = s.getByteList();
        final byte[] array = bl.getUnsafeBytes();
        final int end = bl.length();
        for (int i = bl.begin(); i < end; ++i) {
            if (array[i] == (byte) 0) {
                throw runtime.newSecurityError("string contains null byte");
            }
        }
    }

    public static boolean isUnicode(Encoding enc) {
        byte[] name = enc.getName();
        return name.length > 4 && name[0] == 'U' && name[1] == 'T' && name[2] == 'F' && name[4] != '7';
    }

    public static String escapedCharFormat(int c, boolean isUnicode) {
        String format;
        // c comparisons must be unsigned 32-bit
        if (isUnicode) {

            if ((c & 0xFFFFFFFFL) < 0x7F && Encoding.isAscii(c) && ASCIIEncoding.INSTANCE.isPrint(c)) {
                format = "%c";
            } else if (c < 0x10000) {
                format = "\\u%04X";
            } else {
                format = "\\u{%X}";
            }
        } else {
            if ((c & 0xFFFFFFFFL) < 0x100) {
                format = "\\x%02X";
            } else {
                format = "\\x{%X}";
            }
        }
        return format;
    }

    // mri: ONIGENC_MBCLEN_NEEDMORE_P - onigurama.h
    public static boolean isIncompleteChar(int b) {
        return b < -1;
    }

    public static int bytesToFixBrokenTrailingCharacter(ByteList val, int usingLength) {
        return bytesToFixBrokenTrailingCharacter(val.getUnsafeBytes(), val.getBegin(), val.getRealSize(), val.getEncoding(), usingLength);
    }

    public static int bytesToFixBrokenTrailingCharacter(byte[] bytes, int begin, int byteSize, Encoding encoding, int usingLength) {
        // read additional bytes to fix broken char
        if (byteSize > 0) {
            // get head offset of broken character
            int charHead = encoding.leftAdjustCharHead(
                    bytes, // string bytes
                    begin, // start of string
                    begin + usingLength - 1, // last byte
                    begin + usingLength); // end of using

            // external offset
            charHead -= begin;

            // byte at char head
            byte byteHead = (byte)(bytes[begin + charHead] & 0xFF);

            // total bytes we would need to complete character
            int extra = encoding.length(byteHead);

            // what we already have
            extra -= usingLength - charHead;

            return extra;
        }

        return 0;
    }

    public static int memchr(byte[] ptr, int start, int find, int len) {
        for (int i = start; i < start + len; i++) {
            if (ptr[i] == find) return i;
        }
        return -1;
    }

    // StringValueCstr, rb_string_value_cstr without trailing null addition
    public static RubyString checkEmbeddedNulls(Ruby runtime, IRubyObject ptr) {
        RubyString str = ptr.convertToString();
        ByteList strByteList = str.getByteList();
        byte[] sBytes = strByteList.unsafeBytes();
        int s = strByteList.begin();
        int len = strByteList.length();
        Encoding enc = str.getEncoding();
        final int minlen = enc.minLength();

        if (minlen > 1) {
            if (strNullChar(sBytes, s, len, minlen, enc) != -1) {
                throw runtime.newArgumentError("string contains null char");
            }
            return strFillTerm(str, sBytes, s, len, minlen, minlen);
        }
        if (memchr(sBytes, s, 0, len) != -1) {
            throw runtime.newArgumentError("string contains null byte");
        }
//        if (s[len]) {
//            rb_str_modify(str);
//            s = RSTRING_PTR(str);
//            s[RSTRING_LEN(str)] = 0;
//        }
        return str;
    }

    // MRI: str_null_char
    public static int strNullChar(byte[] sBytes, int s, int len, final int minlen, Encoding enc) {
        int e = s + len;

        for (; s + minlen <= e; s += enc.length(sBytes, s, e)) {
            if (zeroFilled(sBytes, s, minlen)) return s;
        }
        return -1;
    }

    public static boolean zeroFilled(byte[] sBytes, int s, int n) {
        for (; n > 0; --n) {
            if (sBytes[s++] != 0) return false;
        }
        return true;
    }

    public static RubyString strFillTerm(RubyString str, byte[] sBytes, int s, int len, int oldtermlen, int termlen) {
        int capa = str.getByteList().getUnsafeBytes().length - str.getByteList().begin();

        if (capa < len + termlen) {
            str.modify(len + termlen);
        }
        else if (!str.independent()) {
            if (zeroFilled(sBytes, s + len, termlen)) return str;
            str.makeIndependent();
        }
        sBytes = str.getByteList().getUnsafeBytes();
        s = str.getByteList().begin();
        TERM_FILL(sBytes, s + len, termlen);
        return str;
    }

    public static void TERM_FILL(byte[] ptrBytes, int ptr, int termlen) {
        int term_fill_ptr = ptr;
        int term_fill_len = termlen;
        ptrBytes[term_fill_ptr] = '\0';
        if (term_fill_len > 1)
        Arrays.fill(ptrBytes, term_fill_ptr, term_fill_len, (byte)0);
    }

    /**
     * rb_str_scan
     */

    public static int positionEndForScan(ByteList value, Matcher matcher, Encoding enc, int begin, int range) {
        int end = matcher.getEnd();
        if (matcher.getBegin() == end) {
            if (value.getRealSize() > end) {
                return end + enc.length(value.getUnsafeBytes(), begin + end, range);
            } else {
                return end + 1;
            }
        } else {
            return end;
        }
    }

    /**
     * rb_str_dump
     */

    public static ByteList dumpCommon(Ruby runtime, ByteList byteList) {
        ByteList buf = null;
        Encoding enc = byteList.getEncoding();

        int p = byteList.getBegin();
        int end = p + byteList.getRealSize();
        byte[]bytes = byteList.getUnsafeBytes();

        int len = 2;
        while (p < end) {
            int c = bytes[p++] & 0xff;

            switch (c) {
            case '"':case '\\':case '\n':case '\r':case '\t':case '\f':
            case '\013': case '\010': case '\007': case '\033':
                len += 2;
                break;
            case '#':
                len += isEVStr(bytes, p, end) ? 2 : 1;
                break;
            default:
                if (ASCIIEncoding.INSTANCE.isPrint(c)) {
                    len++;
                } else {
                    if (enc instanceof UTF8Encoding) {
                        int n = preciseLength(enc, bytes, p - 1, end) - 1;
                        if (n > 0) {
                            if (buf == null) buf = new ByteList();
                            int cc = codePoint(runtime, enc, bytes, p - 1, end);
                            Sprintf.sprintf(runtime, buf, "%x", cc);
                            len += buf.getRealSize() + 4;
                            buf.setRealSize(0);
                            p += n;
                            break;
                        }
                    }
                    len += 4;
                }
                break;
            }
        }

        if (!enc.isAsciiCompatible()) {
            len += ".force_encoding(\"".length() + enc.getName().length + "\")".length();
        }

        ByteList outBytes = new ByteList(len);
        byte out[] = outBytes.getUnsafeBytes();
        int q = 0;
        p = byteList.getBegin();
        end = p + byteList.getRealSize();

        out[q++] = '"';
        while (p < end) {
            int c = bytes[p++] & 0xff;
            if (c == '"' || c == '\\') {
                out[q++] = '\\';
                out[q++] = (byte)c;
            } else if (c == '#') {
                if (isEVStr(bytes, p, end)) out[q++] = '\\';
                out[q++] = '#';
            } else if (c == '\n') {
                out[q++] = '\\';
                out[q++] = 'n';
            } else if (c == '\r') {
                out[q++] = '\\';
                out[q++] = 'r';
            } else if (c == '\t') {
                out[q++] = '\\';
                out[q++] = 't';
            } else if (c == '\f') {
                out[q++] = '\\';
                out[q++] = 'f';
            } else if (c == '\013') {
                out[q++] = '\\';
                out[q++] = 'v';
            } else if (c == '\010') {
                out[q++] = '\\';
                out[q++] = 'b';
            } else if (c == '\007') {
                out[q++] = '\\';
                out[q++] = 'a';
            } else if (c == '\033') {
                out[q++] = '\\';
                out[q++] = 'e';
            } else if (ASCIIEncoding.INSTANCE.isPrint(c)) {
                out[q++] = (byte)c;
            } else {
                out[q++] = '\\';
                if (enc instanceof UTF8Encoding) {
                    int n = preciseLength(enc, bytes, p - 1, end) - 1;
                    if (n > 0) {
                        int cc = codePoint(runtime, enc, bytes, p - 1, end);
                        p += n;
                        outBytes.setRealSize(q);
                        Sprintf.sprintf(runtime, outBytes, "u{%x}", cc);
                        q = outBytes.getRealSize();
                        continue;
                    }
                }
                outBytes.setRealSize(q);
                Sprintf.sprintf(runtime, outBytes, "x%02X", c);
                q = outBytes.getRealSize();
            }
        }
        out[q++] = '"';
        outBytes.setRealSize(q);
        assert out == outBytes.getUnsafeBytes(); // must not reallocate

        return outBytes;
    }

    public static boolean isEVStr(byte[]bytes, int p, int end) {
        return p < end ? isEVStr(bytes[p] & 0xff) : false;
    }

    public static boolean isEVStr(int c) {
        return c == '$' || c == '@' || c == '{';
    }

    /**
     * rb_str_count
     */

    public static int countCommon19(ByteList value, Ruby runtime, boolean[] table, TrTables tables, Encoding enc) {
        int i = 0;
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();

        int c;
        while (p < end) {
            if (enc.isAsciiCompatible() && (c = bytes[p] & 0xff) < 0x80) {
                if (table[c]) i++;
                p++;
            } else {
                c = codePoint(runtime, enc, bytes, p, end);
                int cl = codeLength(enc, c);
                if (trFind(c, table, tables)) i++;
                p += cl;
            }
        }

        return i;
    }

    // MRI: rb_str_rindex
    public static int rindex(ByteList source, int sourceChars, int subChars, int pos, CodeRangeable subStringCodeRangeable, Encoding enc) {
        if (subStringCodeRangeable.scanForCodeRange() == CR_BROKEN) return -1;

        final ByteList subString = subStringCodeRangeable.getByteList();

        int sourceSize = source.realSize();
        int subSize = subString.realSize();

        if (sourceChars < subChars || sourceSize < subSize) return -1;
        if (sourceChars - pos < subChars) pos = sourceChars - subChars;
        if (sourceChars == 0) return pos;

        byte[] sourceBytes = source.getUnsafeBytes();
        int sbeg = source.getBegin();
        int end = sbeg + source.getRealSize();

        if (pos == 0) {
            if (ByteList.memcmp(sourceBytes, sbeg, subString.getUnsafeBytes(), subString.begin(), subString.getRealSize()) == 0) {
                return 0;
            } else {
                return -1;
            }
        }

        int s = nth(enc, sourceBytes, sbeg, end, pos);

        return strRindex(source, subString, s, pos, enc);
    }

    private static int strRindex(ByteList str, ByteList sub, int s, int pos, Encoding enc) {
        int slen;
        byte[] strBytes = str.unsafeBytes();
        byte[] subBytes = sub.unsafeBytes();
        int sbeg, e, t;

        sbeg = str.begin();
        e = str.begin() + str.realSize();
        t = sub.begin();
        slen = sub.realSize();

        while (s >= sbeg && s + slen <= sbeg + str.realSize()) {
            if (ByteList.memcmp(strBytes, s, subBytes, t, slen) == 0) {
                return pos;
            }
            if (pos == 0) break;
            pos--;
            s = enc.prevCharHead(strBytes, sbeg, s, e);
        }

        return -1;
    }

    public static int strLengthFromRubyString(CodeRangeable string, Encoding enc) {
        final ByteList bytes = string.getByteList();

        if (isSingleByteOptimizable(string, enc)) return bytes.getRealSize();
        return strLengthFromRubyStringFull(string, bytes, enc);
    }

    public static int strLengthFromRubyString(CodeRangeable string) {
        final ByteList bytes = string.getByteList();
        return strLengthFromRubyStringFull(string, bytes, bytes.getEncoding());
    }

    private static int strLengthFromRubyStringFull(CodeRangeable string, ByteList bytes, Encoding enc) {
        if (string.isCodeRangeValid() && enc instanceof UTF8Encoding) return utf8Length(bytes);

        long lencr = strLengthWithCodeRange(bytes, enc);
        int cr = unpackArg(lencr);
        if (cr != 0) string.setCodeRange(cr);
        return unpackResult(lencr);
    }

    /**
     * rb_str_tr / rb_str_tr_bang
     */

    // TODO (nirvdrum Dec. 19, 2014): Neither the constructor nor the fields should be public. I temporarily escalated visibility during a refactoring that moved the inner class to a new parent class, while the old parent class still needs access.
    public static final class TR {
        public TR(ByteList bytes) {
            p = bytes.getBegin();
            pend = bytes.getRealSize() + p;
            buf = bytes.getUnsafeBytes();
            now = max = 0;
            gen = false;
        }

        public int p, pend, now, max;
        public boolean gen;
        public byte[]buf;
    }

    /**
     * tr_setup_table
     */
    public static final class TrTables {
        private IntHash<IRubyObject> del, noDel;
    }

    public static TrTables trSetupTable(ByteList str, Ruby runtime, boolean[] stable, TrTables tables, boolean first, Encoding enc) {
        int errc = -1;
        byte[] buf = new byte[256];
        final TR tr = new TR(str);
        int c;
        IntHash<IRubyObject> table = null, ptable = null;
        int i, l[] = {0};
        boolean cflag = false;

        tr.buf = str.unsafeBytes(); tr.p = str.begin(); tr.pend = tr.p + str.realSize();
        tr.gen = false;
        tr.now = tr.max = 0;

        if (str.realSize() > 1 && EncodingUtils.encAscget(tr.buf, tr.p, tr.pend, l, enc) == '^') {
            cflag = true;
            tr.p += l[0];
        }
        if (first) {
            for (i=0; i<TRANS_SIZE; i++) {
                stable[i] = true;
            }
            stable[TRANS_SIZE] = cflag;
        }
        else if (stable[TRANS_SIZE] && !cflag) {
            stable[TRANS_SIZE] = false;
        }
        for (i=0; i<TRANS_SIZE; i++) {
            buf[i] = (byte)(cflag ? 1 : 0);
        }

        if (tables == null) tables = new TrTables();

        while ((c = trNext(tr, runtime, enc)) != errc) {
            if (c < TRANS_SIZE) {
                buf[c & 0xff] = (byte)(cflag ? 0 : 1);
            }
            else {
                int key = c;

                if (table == null && (first || tables.del != null || stable[TRANS_SIZE])) {
                    if (cflag) {
                        ptable = tables.noDel;
                        table = ptable != null ? ptable : new IntHash();
                        tables.noDel = table;
                    }
                    else {
                        table = new IntHash();
                        ptable = tables.del;
                        tables.del = table;
                    }
                }
                if (table != null && (ptable == null || (cflag ^ ptable.get(key) == null))) {
                    table.put(key, RubyBasicObject.NEVER);
                }
            }
        }
        for (i=0; i<TRANS_SIZE; i++) {
            stable[i] = stable[i] && buf[i] != 0;
        }
        if (table == null && !cflag) {
            tables.del = null;
        }

        return tables;
    }

    public static boolean trFind(int c, boolean[] table, TrTables tables) {
        if (c < TRANS_SIZE) {
            return table[c];
        } else {
            int v = c;

            if (tables.del != null) {
                if (tables.del.get(v) != null &&
                        (tables.noDel == null || tables.noDel.get(v) == null)) {
                    return true;
                }
            }
            else if (tables.noDel != null && tables.noDel.get(v) != null) {
                return false;
            }
            return table[TRANS_SIZE] ? true : false;
        }
    }

    public static int trNext(TR t, Ruby runtime, Encoding enc) {
        for (;;) {
            if (!t.gen) {
                return trNext_nextpart(t, runtime, enc);
            } else {
                while (enc.codeToMbcLength(++t.now) <= 0) {
                    if (t.now == t.max) {
                        t.gen = false;
                        return trNext_nextpart(t, runtime, enc);
                    }
                }
                if (t.now < t.max) {
                    return t.now;
                } else {
                    t.gen = false;
                    return t.max;
                }
            }
        }
    }

    private static int trNext_nextpart(TR t, Ruby runtime, Encoding enc) {
        int[] n = {0};

        if (t.p == t.pend) return -1;
        if (EncodingUtils.encAscget(t.buf, t.p, t.pend, n, enc) == '\\' && t.p + n[0] < t.pend) {
            t.p += n[0];
        }
        t.now = EncodingUtils.encCodepointLength(runtime, t.buf, t.p, t.pend, n, enc);
        t.p += n[0];
        if (EncodingUtils.encAscget(t.buf, t.p, t.pend, n, enc) == '-' && t.p + n[0] < t.pend) {
            t.p += n[0];
            if (t.p < t.pend) {
                int c = EncodingUtils.encCodepointLength(runtime, t.buf, t.p, t.pend, n, enc);
                t.p += n[0];
                if (t.now > c) {
                    if (t.now < 0x80 && c < 0x80) {
                        throw runtime.newArgumentError("invalid range \""
                                + (char) t.now + "-" + (char) c + "\" in string transliteration");
                    }

                    throw runtime.newArgumentError("invalid range in string transliteration");
                }
                t.gen = true;
                t.max = c;
            }
        }
        return t.now;
    }

    public static enum NeighborChar {NOT_CHAR, FOUND, WRAPPED}

    // MRI: str_succ
    public static ByteList succCommon(Ruby runtime, ByteList original) {
        byte carry[] = new byte[org.jcodings.Config.ENC_CODE_TO_MBC_MAXLEN];
        int carryP = 0;
        carry[0] = 1;
        int carryLen = 1;

        ByteList valueCopy = new ByteList(original);
        valueCopy.setEncoding(original.getEncoding());
        Encoding enc = original.getEncoding();
        int p = valueCopy.getBegin();
        int end = p + valueCopy.getRealSize();
        int s = end;
        byte[]bytes = valueCopy.getUnsafeBytes();

        NeighborChar neighbor = NeighborChar.FOUND;
        int lastAlnum = -1;
        boolean alnumSeen = false;
        while ((s = enc.prevCharHead(bytes, p, s, end)) != -1) {
            if (neighbor == NeighborChar.NOT_CHAR && lastAlnum != -1) {
                ASCIIEncoding ascii = ASCIIEncoding.INSTANCE;
                if (ascii.isAlpha(bytes[lastAlnum] & 0xff) ?
                        ascii.isDigit(bytes[s] & 0xff) :
                        ascii.isDigit(bytes[lastAlnum] & 0xff) ?
                                ascii.isAlpha(bytes[s] & 0xff) : false) {
                    s = lastAlnum;
                    break;
                }
            }

            int cl = preciseLength(enc, bytes, s, end);
            if (cl <= 0) continue;
            switch (neighbor = succAlnumChar(runtime, enc, bytes, s, cl, carry, 0)) {
                case NOT_CHAR: continue;
                case FOUND:    return valueCopy;
                case WRAPPED:  lastAlnum = s;
            }
            alnumSeen = true;
            carryP = s - p;
            carryLen = cl;
        }

        if (!alnumSeen) {
            s = end;
            while ((s = enc.prevCharHead(bytes, p, s, end)) != -1) {
                int cl = preciseLength(enc, bytes, s, end);
                if (cl <= 0) continue;
                neighbor = succChar(runtime, enc, bytes, s, cl);
                if (neighbor == NeighborChar.FOUND) return valueCopy;
                if (preciseLength(enc, bytes, s, s + 1) != cl) succChar(runtime, enc, bytes, s, cl); /* wrapped to \0...\0.  search next valid char. */
                if (!enc.isAsciiCompatible()) {
                    System.arraycopy(bytes, s, carry, 0, cl);
                    carryLen = cl;
                }
                carryP = s - p;
            }
        }
        valueCopy.ensure(valueCopy.getBegin() + valueCopy.getRealSize() + carryLen);
        s = valueCopy.getBegin() + carryP;
        System.arraycopy(valueCopy.getUnsafeBytes(), s, valueCopy.getUnsafeBytes(), s + carryLen, valueCopy.getRealSize() - carryP);
        System.arraycopy(carry, 0, valueCopy.getUnsafeBytes(), s, carryLen);
        valueCopy.setRealSize(valueCopy.getRealSize() + carryLen);
        return valueCopy;
    }

    // MRI: enc_succ_char
    public static NeighborChar succChar(Ruby runtime, Encoding enc, byte[] bytes, int p, int len) {
        int l;
        if (enc.minLength() > 1) {
	        /* wchar, trivial case */
            int r = preciseLength(enc, bytes, p, p + len), c;
            if (!MBCLEN_CHARFOUND_P(r)) {
                return NeighborChar.NOT_CHAR;
            }
            c = codePoint(runtime, enc, bytes, p, p + len) + 1;
            l = codeLength(enc, c);
            if (l == 0) return NeighborChar.NOT_CHAR;
            if (l != len) return NeighborChar.WRAPPED;
            EncodingUtils.encMbcput(c, bytes, p, enc);
            r = preciseLength(enc, bytes, p, p + len);
            if (!MBCLEN_CHARFOUND_P(r)) {
                return NeighborChar.NOT_CHAR;
            }
            return NeighborChar.FOUND;
        }

        while (true) {
            int i = len - 1;
            for (; i >= 0 && bytes[p + i] == (byte)0xff; i--) bytes[p + i] = 0;
            if (i < 0) return NeighborChar.WRAPPED;
            bytes[p + i] = (byte)((bytes[p + i] & 0xff) + 1);
            l = preciseLength(enc, bytes, p, p + len);
            if (MBCLEN_CHARFOUND_P(l)) {
                l = MBCLEN_CHARFOUND_LEN(l);
                if (l == len) {
                    return NeighborChar.FOUND;
                } else {
                    int start = p + l;
                    int end = start + (len - l);
                    Arrays.fill(bytes, start, end, (byte) 0xff);
                }
            }
            if (MBCLEN_INVALID_P(l) && i < len - 1) {
                int len2;
                int l2;
                for (len2 = len-1; 0 < len2; len2--) {
                    l2 = preciseLength(enc, bytes, p, p + len2);
                    if (!MBCLEN_INVALID_P(l2))
                        break;
                }
                int start = p+len2+1;
                int end = start + len-(len2+1);
                Arrays.fill(bytes, start, end, (byte)0xff);
            }
        }
    }

    // MRI: enc_succ_alnum_char
    private static NeighborChar succAlnumChar(Ruby runtime, Encoding enc, byte[]bytes, int p, int len, byte[]carry, int carryP) {
        byte save[] = new byte[org.jcodings.Config.ENC_CODE_TO_MBC_MAXLEN];
        int c = enc.mbcToCode(bytes, p, p + len);

        final int cType;
        if (enc.isDigit(c)) {
            cType = CharacterType.DIGIT;
        } else if (enc.isAlpha(c)) {
            cType = CharacterType.ALPHA;
        } else {
            return NeighborChar.NOT_CHAR;
        }

        System.arraycopy(bytes, p, save, 0, len);
        NeighborChar ret = succChar(runtime, enc, bytes, p, len);
        if (ret == NeighborChar.FOUND) {
            c = enc.mbcToCode(bytes, p, p + len);
            if (enc.isCodeCType(c, cType)) return NeighborChar.FOUND;
        }

        System.arraycopy(save, 0, bytes, p, len);
        int range = 1;

        while (true) {
            System.arraycopy(bytes, p, save, 0, len);
            ret = predChar(runtime, enc, bytes, p, len);
            if (ret == NeighborChar.FOUND) {
                c = enc.mbcToCode(bytes, p, p + len);
                if (!enc.isCodeCType(c, cType)) {
                    System.arraycopy(save, 0, bytes, p, len);
                    break;
                }
            } else {
                System.arraycopy(save, 0, bytes, p, len);
                break;
            }
            range++;
        }

        if (range == 1) return NeighborChar.NOT_CHAR;

        if (cType != CharacterType.DIGIT) {
            System.arraycopy(bytes, p, carry, carryP, len);
            return NeighborChar.WRAPPED;
        }

        System.arraycopy(bytes, p, carry, carryP, len);
        succChar(runtime, enc, carry, carryP, len);
        return NeighborChar.WRAPPED;
    }

    private static NeighborChar predChar(Ruby runtime, Encoding enc, byte[]bytes, int p, int len) {
        int l;
        if (enc.minLength() > 1) {
	        /* wchar, trivial case */
            int r = preciseLength(enc, bytes, p, p + len), c;
            if (!MBCLEN_CHARFOUND_P(r)) {
                return NeighborChar.NOT_CHAR;
            }
            c = codePoint(runtime, enc, bytes, p, p + len);
            if (c == 0) return NeighborChar.NOT_CHAR;
            --c;
            l = codeLength(enc, c);
            if (l == 0) return NeighborChar.NOT_CHAR;
            if (l != len) return NeighborChar.WRAPPED;
            EncodingUtils.encMbcput(c, bytes, p, enc);
            r = preciseLength(enc, bytes, p, p + len);
            if (!MBCLEN_CHARFOUND_P(r)) {
                return NeighborChar.NOT_CHAR;
            }
            return NeighborChar.FOUND;
        }
        while (true) {
            int i = len - 1;
            for (; i >= 0 && bytes[p + i] == 0; i--) bytes[p + i] = (byte)0xff;
            if (i < 0) return NeighborChar.WRAPPED;
            bytes[p + i] = (byte)((bytes[p + i] & 0xff) - 1);
            l = preciseLength(enc, bytes, p, p + len);
            if (MBCLEN_CHARFOUND_P(l)) {
                l = MBCLEN_CHARFOUND_LEN(l);
                if (l == len) {
                    return NeighborChar.FOUND;
                } else {
                    int start = p + l;
                    int end = start + (len - l);
                    Arrays.fill(bytes, start, end, (byte) 0x0);
                }
            }
            if (!MBCLEN_CHARFOUND_P(l) && i < len-1) {
                int len2;
                int l2;
                for (len2 = len-1; 0 < len2; len2--) {
                    l2 = preciseLength(enc, bytes, p, p + len2);
                    if (!MBCLEN_INVALID_P(l2))
                        break;
                }
                int start = p + len2 + 1;
                int end = start + (len - (len2 + 1));
                Arrays.fill(bytes, start, end, (byte) 0);
            }
        }
    }

    public static boolean isSingleByteOptimizable(CodeRangeable string, Encoding encoding) {
        return string.getCodeRange() == CR_7BIT || encoding.maxLength() == 1;
    }

    public static int index(CodeRangeable sourceString, CodeRangeable otherString, int offset, Encoding enc) {
        if (otherString.scanForCodeRange() == CR_BROKEN) return -1;

        int sourceLen = strLengthFromRubyString(sourceString);
        int otherLen = strLengthFromRubyString(otherString);

        if (offset < 0) {
            offset += sourceLen;
            if (offset < 0) return -1;
        }

        final ByteList source = sourceString.getByteList();
        final ByteList other = otherString.getByteList();

        if (sourceLen - offset < otherLen) return -1;
        byte[]bytes = source.getUnsafeBytes();
        int p = source.getBegin();
        int end = p + source.getRealSize();
        if (offset != 0) {
            offset = isSingleByteOptimizable(sourceString, enc) ? offset : offset(enc, bytes, p, end, offset);
            p += offset;
        }
        if (otherLen == 0) return offset;

        while (true) {
            int pos = source.indexOf(other, p - source.getBegin());
            if (pos < 0) return pos;
            pos -= (p - source.getBegin());
            int t = enc.rightAdjustCharHead(bytes, p, p + pos, end);
            if (t == p + pos) return pos + offset;
            if ((sourceLen -= t - p) <= 0) return -1;
            offset += t - p;
            p = t;
        }
    }

    public static void associateEncoding(CodeRangeable string, Encoding enc) {
        final ByteList value = string.getByteList();

        if (value.getEncoding() != enc) {
            if (!CodeRangeSupport.isCodeRangeAsciiOnly(string) || !enc.isAsciiCompatible()) string.clearCodeRange();
            value.setEncoding(enc);
        }
    }

    public static ByteList replaceInternal(int beg, int len, ByteListHolder source, CodeRangeable repl) {
        int oldLength = source.getByteList().getRealSize();
        if (beg + len >= oldLength) len = oldLength - beg;
        ByteList replBytes = repl.getByteList();
        int replLength = replBytes.getRealSize();
        int newLength = oldLength + replLength - len;

        byte[]oldBytes = source.getByteList().getUnsafeBytes();
        int oldBegin = source.getByteList().getBegin();

        source.modify(newLength);
        if (replLength != len) {
            System.arraycopy(oldBytes, oldBegin + beg + len, source.getByteList().getUnsafeBytes(), beg + replLength, oldLength - (beg + len));
        }

        if (replLength > 0) System.arraycopy(replBytes.getUnsafeBytes(), replBytes.getBegin(), source.getByteList().getUnsafeBytes(), beg, replLength);
        source.getByteList().setRealSize(newLength);

        return source.getByteList();
    }

    // MRI: rb_str_update, second half
    public static void replaceInternal19(int beg, int len, CodeRangeable source, CodeRangeable repl) {
        Encoding enc = source.checkEncoding(repl);

        source.modify();
        source.keepCodeRange();
        ByteList sourceBL = source.getByteList();
        byte[] sourceBytes = sourceBL.unsafeBytes();
        int sourceBeg = sourceBL.begin();
        int sourceEnd = sourceBeg + sourceBL.realSize();
        boolean singlebyte = isSingleByteOptimizable(source, source.getByteList().getEncoding());
        int p = nth(enc, sourceBytes, sourceBeg, sourceEnd, beg, singlebyte);
        if (p == -1) p = sourceEnd;
        int e = nth(enc, sourceBytes, p, sourceEnd, len, singlebyte);
        if (e == -1) e = sourceEnd;
        /* error check */
        beg = p - sourceBeg; /* physical position */
        len = e - p; /* physical length */
        replaceInternal(beg, len, source, repl);
        associateEncoding(source, enc);
        int cr = CodeRangeSupport.codeRangeAnd(source.getCodeRange(), repl.getCodeRange());
        if (cr != CR_BROKEN) source.setCodeRange(cr);
    }

    // MRI: rb_str_update, first half
    public static void replaceInternal19(Ruby runtime, int beg, int len, RubyString source, RubyString repl) {
        source.checkEncoding(repl);

        if (len < 0) throw runtime.newIndexError("negative length " + len);

        source.checkEncoding(repl);
        int slen = strLengthFromRubyString(source);

        if (slen < beg) {
            throw runtime.newIndexError("index " + beg + " out of string");
        }
        if (beg < 0) {
            if (-beg > slen) {
                throw runtime.newIndexError("index " + beg + " out of string");
            }
            beg += slen;
        }
        if (slen < len || slen < beg + len) {
            len = slen - beg;
        }

        replaceInternal19(beg, len, source, repl);

        if (repl.isTaint()) source.setTaint(true);
    }

    public static boolean isAsciiOnly(CodeRangeable string) {
        return string.getByteList().getEncoding().isAsciiCompatible() && string.scanForCodeRange() == CR_7BIT;
    }

    /**
     * rb_str_delete_bang
     */
    public static CodeRangeable delete_bangCommon19(CodeRangeable rubyString, Ruby runtime, boolean[] squeeze, TrTables tables, Encoding enc) {
        rubyString.modify();
        rubyString.keepCodeRange();

        final ByteList value = rubyString.getByteList();

        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        boolean modify = false;
        boolean asciiCompatible = enc.isAsciiCompatible();
        int cr = asciiCompatible ? CR_7BIT : CR_VALID;
        while (s < send) {
            int c;
            if (asciiCompatible && Encoding.isAscii(c = bytes[s] & 0xff)) {
                if (squeeze[c]) {
                    modify = true;
                } else {
                    if (t != s) bytes[t] = (byte)c;
                    t++;
                }
                s++;
            } else {
                c = codePoint(runtime, enc, bytes, s, send);
                int cl = codeLength(enc, c);
                if (trFind(c, squeeze, tables)) {
                    modify = true;
                } else {
                    if (t != s) enc.codeToMbc(c, bytes, t);
                    t += cl;
                    if (cr == CR_7BIT) cr = CR_VALID;
                }
                s += cl;
            }
        }
        value.setRealSize(t - value.getBegin());
        rubyString.setCodeRange(cr);

        return modify ? rubyString : null;
    }

    /**
     * MRI: chopped_length
     */
    public static int choppedLength19(CodeRangeable str, Ruby runtime) {
        ByteList bl = str.getByteList();
        Encoding enc = bl.getEncoding();
        int p, p2, beg, end;

        beg = bl.begin();
        end = beg + bl.realSize();
        if (beg > end) return 0;
        p = enc.prevCharHead(bl.unsafeBytes(), beg, end, end);
        if (p == 0) return 0;
        if (p > beg && EncodingUtils.encAscget(bl.unsafeBytes(), p, end, null, enc) == '\n') {
            p2 = enc.prevCharHead(bl.unsafeBytes(), beg, p, end);
            if (p2 != -1 && EncodingUtils.encAscget(bl.unsafeBytes(),  p2, end, null, enc) == '\r') p = p2;
        }
        return p - beg;
    }

    /**
     * rb_enc_compatible
     */

    public static Encoding areCompatible(CodeRangeable string, CodeRangeable other) {
        Encoding enc1 = string.getByteList().getEncoding();
        Encoding enc2 = other.getByteList().getEncoding();

        if (enc1 == enc2) return enc1;

        if (other.getByteList().getRealSize() == 0) return enc1;
        if (string.getByteList().getRealSize() == 0) {
            return (enc1.isAsciiCompatible() && isAsciiOnly(other)) ? enc1 : enc2;
        }

        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;

        return RubyEncoding.areCompatible(enc1, string.scanForCodeRange(), enc2, other.scanForCodeRange());
    }

    public static ByteList addByteLists(ByteList value1, ByteList value2) {
        ByteList result = new ByteList(value1.getRealSize() + value2.getRealSize());
        result.setRealSize(value1.getRealSize() + value2.getRealSize());
        System.arraycopy(value1.getUnsafeBytes(), value1.getBegin(), result.getUnsafeBytes(), 0, value1.getRealSize());
        System.arraycopy(value2.getUnsafeBytes(), value2.getBegin(), result.getUnsafeBytes(), value1.getRealSize(), value2.getRealSize());
        return result;
    }

    public static boolean areComparable(CodeRangeable string, CodeRangeable other) {
        ByteList otherValue = other.getByteList();
        if (string.getByteList().getEncoding() == otherValue.getEncoding() ||
                string.getByteList().getRealSize() == 0 || otherValue.getRealSize() == 0) return true;
        return areComparableViaCodeRange(string, other);
    }

    public static boolean areComparableViaCodeRange(CodeRangeable string, CodeRangeable other) {
        int cr1 = string.scanForCodeRange();
        int cr2 = other.scanForCodeRange();

        if (cr1 == CR_7BIT && (cr2 == CR_7BIT || other.getByteList().getEncoding().isAsciiCompatible())) return true;
        if (cr2 == CR_7BIT && string.getByteList().getEncoding().isAsciiCompatible()) return true;
        return false;
    }

    public static IRubyObject rbStrEnumerateLines(RubyString str, ThreadContext context, String name, IRubyObject arg, Block block, boolean wantarray) {
        Ruby runtime = context.runtime;

        Encoding enc;
        IRubyObject line, rs, orig = str;
        int ptr, pend, subptr, subend, rsptr, hit, adjusted;
        int pos, len, rslen;
        boolean paragraph_mode = false;

        IRubyObject ary = null;

        rs = arg;

        if (block.isGiven()) {
            if (wantarray) {
                // this code should be live in 3.0
                if (false) { // #if STRING_ENUMERATORS_WANTARRAY
                    runtime.getWarnings().warn("given block not used");
                    ary = runtime.newEmptyArray();
                } else {
                    runtime.getWarnings().warning("passing a block to String#lines is deprecated");
                    wantarray = false;
                }
            }
        }
        else {
            if (wantarray) {
                ary = runtime.newEmptyArray();
            } else {
                return enumeratorize(runtime, str, name, arg);
            }
        }

        if (rs.isNil()) {
            if (wantarray) {
                ((RubyArray)ary).push(str);
                return ary;
            }
            else {
                block.yieldSpecific(context, str);
                return orig;
            }
        }

        str = str.newFrozen();
        byte[] strBytes = str.getByteList().unsafeBytes();
        ptr = subptr = str.getByteList().begin();
        pend = ptr + str.size();
        len = str.size();
        rs = rs.convertToString();
        rslen = ((RubyString)rs).size();

        if (rs == context.runtime.getGlobalVariables().get("$/"))
            enc = str.getEncoding();
        else
            enc = str.checkEncoding((RubyString) rs);

        byte[] rsbytes;
        if (rslen == 0) {
            rsbytes = RubyIO.PARAGRAPH_SEPARATOR.unsafeBytes();
            rsptr = RubyIO.PARAGRAPH_SEPARATOR.begin();
            rslen = 2;
            paragraph_mode = true;
        } else {

            rsbytes = ((RubyString)rs).getByteList().unsafeBytes();
            rsptr = ((RubyString)rs).getByteList().begin();
        }

        if ((rs == context.runtime.getGlobalVariables().get("$/") || paragraph_mode) && !enc.isAsciiCompatible()) {
            rs = RubyString.newString(runtime, rsbytes, rsptr, rslen);
            rs = EncodingUtils.rbStrEncode(context, rs, runtime.getEncodingService().convertEncodingToRubyEncoding(enc), 0, context.nil);
            rsbytes = ((RubyString)rs).getByteList().unsafeBytes();
            rsptr = ((RubyString)rs).getByteList().begin();
            rslen = ((RubyString)rs).getByteList().realSize();
        }

        while (subptr < pend) {
            pos = memsearch(rsbytes, rsptr, rslen, strBytes, subptr, pend - subptr, enc);
            if (pos < 0) break;
            hit = subptr + pos;
            adjusted = enc.rightAdjustCharHead(strBytes, subptr, hit, pend);
            if (hit != adjusted) {
                subptr = adjusted;
                continue;
            }
            subend = hit + rslen;
            if (paragraph_mode) {
                while (subend < pend && enc.isNewLine(strBytes, subend, pend)) {
                    subend += enc.length(strBytes, subend, pend);
                }
            }
            line = str.substr(runtime, subptr - ptr, subend - subptr);
            if (wantarray) {
                ((RubyArray)ary).push(line);
            } else {
                block.yieldSpecific(context, line);
                str.modifyCheck(strBytes, len);
            }
            subptr = subend;
        }

        if (subptr != pend) {
            line = str.substr(subptr - ptr, pend - subptr);
            if (wantarray) {
                ((RubyArray) ary).push(line);
            } else {
                block.yieldSpecific(context, line);
            }
        }

        return wantarray ? ary : orig;
    }

    public static int memsearch(byte[] xBytes, int x0, int m, byte[] yBytes, int y0, int n, Encoding enc) {
        int x = x0, y = y0;

        if (m > n) return -1;
        else if (m == n) {
            return ByteList.memcmp(xBytes, x0, yBytes, y0, m) == 0 ? 0 : -1;
        }
        else if (m < 1) {
            return 0;
        }
        else if (m == 1) {
            int ys = memchr(yBytes, y, xBytes[x], n);

            if (ys != -1)
                return ys - y;
            else
                return -1;
        }
        else if (m <= 8) { // SIZEOF_VALUE...meaningless here, but this logic catches short strings
            return rb_memsearch_ss(xBytes, x0, m, yBytes, y0, n);
        }
        else if (enc == UTF8Encoding.INSTANCE){
            return rb_memsearch_qs_utf8(xBytes, x0, m, yBytes, y0, n);
        }
        else {
            return rb_memsearch_qs(xBytes, x0, m, yBytes, y0, n);
        }
    }

    /**
     * rb_str_tr / rb_str_tr_bang
     */

    public static CodeRangeable trTransHelper(Ruby runtime, CodeRangeable self, CodeRangeable srcStr, CodeRangeable replStr, boolean sflag) {
        // This method does not handle the cases where either srcStr or replStr are empty.  It is the responsibility
        // of the caller to take the appropriate action in those cases.

        final ByteList srcList = srcStr.getByteList();
        final ByteList replList = replStr.getByteList();

        int cr = self.getCodeRange();
        Encoding e1 = self.checkEncoding(srcStr);
        Encoding e2 = self.checkEncoding(replStr);
        Encoding enc = e1 == e2 ? e1 : srcStr.checkEncoding(replStr);

        final StringSupport.TR trSrc = new StringSupport.TR(srcList);
        boolean cflag = false;
        int[] l = {0};

        if (self.getByteList().getRealSize() > 1 &&
                EncodingUtils.encAscget(trSrc.buf, trSrc.p, trSrc.pend, l, enc) == '^' &&
                trSrc.p + 1 < trSrc.pend){
            cflag = true;
            trSrc.p++;
        }

        int c, c0, last = 0;
        final int[]trans = new int[StringSupport.TRANS_SIZE];
        final StringSupport.TR trRepl = new StringSupport.TR(replList);
        boolean modify = false;
        IntHash<Integer> hash = null;
        boolean singlebyte = StringSupport.isSingleByteOptimizable(self, EncodingUtils.STR_ENC_GET(self));

        if (cflag) {
            for (int i=0; i< StringSupport.TRANS_SIZE; i++) {
                trans[i] = 1;
            }

            while ((c = StringSupport.trNext(trSrc, runtime, enc)) != -1) {
                if (c < StringSupport.TRANS_SIZE) {
                    trans[c] = -1;
                } else {
                    if (hash == null) hash = new IntHash<Integer>();
                    hash.put(c, 1); // QTRUE
                }
            }
            while ((c = StringSupport.trNext(trRepl, runtime, enc)) != -1) {}  /* retrieve last replacer */
            last = trRepl.now;
            for (int i=0; i< StringSupport.TRANS_SIZE; i++) {
                if (trans[i] != -1) {
                    trans[i] = last;
                }
            }
        } else {
            for (int i=0; i< StringSupport.TRANS_SIZE; i++) {
                trans[i] = -1;
            }

            while ((c = StringSupport.trNext(trSrc, runtime, enc)) != -1) {
                int r = StringSupport.trNext(trRepl, runtime, enc);
                if (r == -1) r = trRepl.now;
                if (c < StringSupport.TRANS_SIZE) {
                    trans[c] = r;
                    if (codeLength(enc, r) != 1) singlebyte = false;
                } else {
                    if (hash == null) hash = new IntHash<Integer>();
                    hash.put(c, r);
                }
            }
        }

        if (cr == CR_VALID) {
            cr = CR_7BIT;
        }
        self.modifyAndKeepCodeRange();
        int s = self.getByteList().getBegin();
        int send = s + self.getByteList().getRealSize();
        byte sbytes[] = self.getByteList().getUnsafeBytes();

        if (sflag) {
            int clen, tlen;
            int max = self.getByteList().getRealSize();
            int save = -1;
            byte[] buf = new byte[max];
            int t = 0;
            while (s < send) {
                boolean mayModify = false;
                c0 = c = codePoint(runtime, e1, sbytes, s, send);
                clen = codeLength(e1, c);
                tlen = enc == e1 ? clen : codeLength(enc, c);
                s += clen;

                if (c < TRANS_SIZE) {
                    c = trCode(c, trans, hash, cflag, last, false);
                } else if (hash != null) {
                    Integer tmp = hash.get(c);
                    if (tmp == null) {
                        if (cflag) {
                            c = last;
                        } else {
                            c = -1;
                        }
                    } else if (cflag) {
                        c = -1;
                    } else {
                        c = tmp;
                    }
                } else {
                    c = -1;
                }

                if (c != -1) {
                    if (save == c) {
                        if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                        continue;
                    }
                    save = c;
                    tlen = codeLength(enc, c);
                    modify = true;
                } else {
                    save = -1;
                    c = c0;
                    if (enc != e1) mayModify = true;
                }

                while (t + tlen >= max) {
                    max *= 2;
                    buf = Arrays.copyOf(buf, max);
                }
                enc.codeToMbc(c, buf, t);
                // MRI does not check s < send again because their null terminator can still be compared
                if (mayModify && (s >= send || ByteList.memcmp(sbytes, s, buf, t, tlen) != 0)) modify = true;
                if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                t += tlen;
            }
            self.getByteList().setUnsafeBytes(buf);
            self.getByteList().setRealSize(t);
        } else if (enc.isSingleByte() || (singlebyte && hash == null)) {
            while (s < send) {
                c = sbytes[s] & 0xff;
                if (trans[c] != -1) {
                    if (!cflag) {
                        c = trans[c];
                        sbytes[s] = (byte)c;
                    } else {
                        sbytes[s] = (byte)last;
                    }
                    modify = true;
                }
                if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                s++;
            }
        } else {
            int clen, tlen, max = (int)(self.getByteList().realSize() * 1.2);
            byte[] buf = new byte[max];
            int t = 0;

            while (s < send) {
                boolean mayModify = false;
                c0 = c = codePoint(runtime, e1, sbytes, s, send);
                clen = codeLength(e1, c);
                tlen = enc == e1 ? clen : codeLength(enc, c);

                if (c < TRANS_SIZE) {
                    c = trans[c];
                } else if (hash != null) {
                    Integer tmp = hash.get(c);
                    if (tmp == null) {
                        if (cflag) {
                            c = last;
                        } else {
                            c = -1;
                        }
                    } else if (cflag) {
                        c = -1;
                    } else {
                        c = tmp;
                    }
                }
                else {
                    c = cflag ? last : -1;
                }
                if (c != -1) {
                    tlen = codeLength(enc, c);
                    modify = true;
                } else {
                    c = c0;
                    if (enc != e1) mayModify = true;
                }
                while (t + tlen >= max) {
                    max <<= 1;
                    buf = Arrays.copyOf(buf, max);
                }
                // headius: I don't see how s and t could ever be the same, since they refer to different buffers
//                if (s != t) {
                enc.codeToMbc(c, buf, t);
                if (mayModify && ByteList.memcmp(sbytes, s, buf, t, tlen) != 0) {
                    modify = true;
                }
//                }

                if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                s += clen;
                t += tlen;
            }
            self.getByteList().setUnsafeBytes(buf);
            self.getByteList().setRealSize(t);
        }

        if (modify) {
            if (cr != CR_BROKEN) self.setCodeRange(cr);
            StringSupport.associateEncoding(self, enc);
            return self;
        }
        return null;
    }

    private static int trCode(int c, int[]trans, IntHash<Integer> hash, boolean cflag, int last, boolean set) {
        if (c < StringSupport.TRANS_SIZE) {
            return trans[c];
        } else if (hash != null) {
            Integer tmp = hash.get(c);
            if (tmp == null) {
                return cflag ? last : -1;
            } else {
                return cflag ? -1 : tmp;
            }
        } else {
            return cflag && set ? last : -1;
        }
    }

    public static int multiByteCasecmp(Encoding enc, ByteList value, ByteList otherValue) {
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();

        byte[]obytes = otherValue.getUnsafeBytes();
        int op = otherValue.getBegin();
        int oend = op + otherValue.getRealSize();

        while (p < end && op < oend) {
            final int c, oc;
            if (enc.isAsciiCompatible()) {
                c = bytes[p] & 0xff;
                oc = obytes[op] & 0xff;
            } else {
                c = preciseCodePoint(enc, bytes, p, end);
                oc = preciseCodePoint(enc, obytes, op, oend);
            }

            int cl, ocl;
            if (enc.isAsciiCompatible() && Encoding.isAscii(c) && Encoding.isAscii(oc)) {
                byte uc = AsciiTables.ToUpperCaseTable[c];
                byte uoc = AsciiTables.ToUpperCaseTable[oc];
                if (uc != uoc) {
                    return uc < uoc ? -1 : 1;
                }
                cl = ocl = 1;
            } else {
                cl = length(enc, bytes, p, end);
                ocl = length(enc, obytes, op, oend);
                // TODO: opt for 2 and 3 ?
                int ret = caseCmp(bytes, p, obytes, op, cl < ocl ? cl : ocl);
                if (ret != 0) return ret < 0 ? -1 : 1;
                if (cl != ocl) return cl < ocl ? -1 : 1;
            }

            p += cl;
            op += ocl;
        }
        if (end - p == oend - op) return 0;
        return end - p > oend - op ? 1 : -1;
    }

    public static boolean singleByteSqueeze(ByteList value, boolean squeeze[]) {
        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        int save = -1;

        while (s < send) {
            int c = bytes[s++] & 0xff;
            if (c != save || !squeeze[c]) bytes[t++] = (byte)(save = c);
        }

        if (t - value.getBegin() != value.getRealSize()) { // modified
            value.setRealSize(t - value.getBegin());
            return true;
        }

        return false;
    }

    public static boolean multiByteSqueeze(Ruby runtime, ByteList value, boolean squeeze[], TrTables tables, Encoding enc, boolean isArg) {
        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        int save = -1;
        int c;

        while (s < send) {
            if (enc.isAsciiCompatible() && (c = bytes[s] & 0xff) < 0x80) {
                if (c != save || (isArg && !squeeze[c])) bytes[t++] = (byte)(save = c);
                s++;
            } else {
                c = codePoint(runtime, enc, bytes, s, send);
                int cl = codeLength(enc, c);
                if (c != save || (isArg && !trFind(c, squeeze, tables))) {
                    if (t != s) enc.codeToMbc(c, bytes, t);
                    save = c;
                    t += cl;
                }
                s += cl;
            }
        }

        if (t - value.getBegin() != value.getRealSize()) { // modified
            value.setRealSize(t - value.getBegin());
            return true;
        }

        return false;
    }

    /**
     * rb_str_swapcase / rb_str_swapcase_bang
     */

    public static boolean singleByteSwapcase(byte[] bytes, int s, int end) {
        boolean modify = false;
        while (s < end) {
            int c = bytes[s] & 0xff;
            if (ASCIIEncoding.INSTANCE.isUpper(c)) {
                bytes[s] = AsciiTables.ToLowerCaseTable[c];
                modify = true;
            } else if (ASCIIEncoding.INSTANCE.isLower(c)) {
                bytes[s] = AsciiTables.ToUpperCaseTable[c];
                modify = true;
            }
            s++;
        }

        return modify;
    }

    public static boolean multiByteSwapcase(Ruby runtime, Encoding enc, byte[] bytes, int s, int end) {
        boolean modify = false;
        while (s < end) {
            int c = codePoint(runtime, enc, bytes, s, end);
            if (enc.isUpper(c)) {
                enc.codeToMbc(toLower(enc, c), bytes, s);
                modify = true;
            } else if (enc.isLower(c)) {
                enc.codeToMbc(toUpper(enc, c), bytes, s);
                modify = true;
            }
            s += codeLength(enc, c);
        }

        return modify;
    }

    private static int rb_memsearch_ss(byte[] xsBytes, int xs, int m, byte[] ysBytes, int ys, int n) {
        int y;

        if ((y = memmem(ysBytes, ys, n, xsBytes, xs, m)) != -1)
            return y - ys;
        else
            return -1;
    }

    // Knuth-Morris-Pratt pattern match
    public static int memmem(byte[] aBytes, int aStart, int aLen, byte[] p, int pStart, int pLen) {
        int[] f = failure(p, pStart, pLen);

        int j = 0;

        for (int i = 0; i < aLen; i++) {
            while (j > 0 && p[pStart + j] != aBytes[aStart + i]) j = f[j - 1];

            if (p[pStart + j] == aBytes[aStart + i]) j++;

            if (j == pLen) return aStart + i - pLen + 1;
        }
        return -1;
    }

    private static int[] failure(byte[] p, int pStart, int pLen) {
        int[] f = new int[pLen];

        int j = 0;
        for (int i = 1; i < pLen; i++) {
            while (j>0 && p[pStart + j] != p[pStart + i]) j = f[j - 1];

            if (p[pStart + j] == p[pStart + i]) j++;

            f[i] = j;
        }

        return f;
    }

    private static int rb_memsearch_qs(byte[] xsBytes, int xs, int m, byte[] ysBytes, int ys, int n) {
        int x = xs, xe = xs + m;
        int y = ys;
        int qstable[] = new int[256];

        /* Preprocessing */
        Arrays.fill(qstable, m + 1);
        for (; x < xe; ++x)
            qstable[xsBytes[x] & 0xFF] = xe - x;
        /* Searching */
        for (; y + m <= ys + n; y += qstable[ysBytes[y + m] & 0xFF]) {
            if (xsBytes[xs] == ysBytes[y] && ByteList.memcmp(xsBytes, xs, ysBytes, y, m) == 0)
                return y - ys;
        }
        return -1;
    }

    private static int rb_memsearch_qs_utf8_hash(byte[] xBytes, int x) {
        int mix = 8353;
        int h = xBytes[x] & 0xFF;
        if (h < 0xC0) {
            return h + 256;
        }
        else if (h < 0xE0) {
            h *= mix;
            h += xBytes[x + 1];
        }
        else if (h < 0xF0) {
            h *= mix;
            h += xBytes[x + 1];
            h *= mix;
            h += xBytes[x + 2];
        }
        else if (h < 0xF5) {
            h *= mix;
            h += xBytes[x + 1];
            h *= mix;
            h += xBytes[x + 2];
            h *= mix;
            h += xBytes[x + 3];
        }
        else {
            return h + 256;
        }
        return h;
    }

    private static int rb_memsearch_qs_utf8(byte[] xsBytes, int xs, int m, byte[] ysBytes, int ys, int n) {
        int x = xs, xe = xs + m;
        int y = ys;
        int qstable[] = new int[512];

        /* Preprocessing */
        Arrays.fill(qstable, m + 1);
        for (; x < xe; ++x) {
            qstable[rb_memsearch_qs_utf8_hash(xsBytes, x)] = xe - x;
        }
        /* Searching */
        for (; y + m <= ys + n; y += qstable[rb_memsearch_qs_utf8_hash(ysBytes, y+m)]) {
            if (xsBytes[xs] == ysBytes[y] && ByteList.memcmp(xsBytes, xs, ysBytes, y, m) == 0)
                return y - ys;
        }
        return -1;
    }
}
