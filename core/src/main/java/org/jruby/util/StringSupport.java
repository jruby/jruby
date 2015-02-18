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

import org.jcodings.Encoding;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.constants.CharacterType;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.IntHash;
import org.joni.Matcher;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

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
        if (n > 0 && end - p >= n) return n;
        return end - p >= enc.minLength() ? enc.minLength() : end - p;
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

    public static int strLength(Encoding enc, byte[]bytes, int p, int end) {
        if (enc.isFixedWidth()) {
            return (end - p + enc.minLength() - 1) / enc.minLength();
        } else if (enc.isAsciiCompatible()) {
            int c = 0;
            while (p < end) {
                if (Encoding.isAscii(bytes[p])) {
                    int q = searchNonAscii(bytes, p, end);
                    if (q == -1) return c + (end - p);
                    c += q - p;
                    p = q;
                }
                p += length(enc, bytes, p, end);
                c++;
            }
            return c;
        }
        
        int c;
        for (c = 0; end > p; c++) p += length(enc, bytes, p, end);
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

    public static int codeLength(Ruby runtime, Encoding enc, int c) {
        int n = enc.codeToMbcLength(c);
        if (n == 0) throw runtime.newRangeError("invalid codepoint " + String.format("0x%x in ", c) + enc.getName());
        return n;
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
        if (enc.isSingleByte()) {
            p += n;
        } else if (enc.isFixedWidth()) {
            p += n * enc.maxLength();             
        } else if (enc.isAsciiCompatible()) {
            p = nthAsciiCompatible(enc, bytes, p, end, n);
        } else {
            p = nthNonAsciiCompatible(enc, bytes, p, end, n);
        }
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
        if (isUnicode) {

            if (c < 0x7F && Encoding.isAscii(c) && ASCIIEncoding.INSTANCE.isPrint(c)) {
                format = "%c"; 
            } else if (c < 0x10000) {
                format = "\\u%04X";
            } else {
                format = "\\u{%X}";
            }
        } else {
            if (c < 0x100) {
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
                int cl = codeLength(runtime, enc, c);
                if (trFind(c, table, tables)) i++;
                p += cl;
            }
        }

        return i;
    }

    /**
     * rb_str_rindex_m
     */
    public static int rindex(ByteList source, int sourceLen, int subLen, int endPosition, CodeRangeable subStringCodeRangeable, Encoding enc) {
        if (subStringCodeRangeable.scanForCodeRange() == CR_BROKEN) return -1;

        if (sourceLen < subLen) return -1;
        if (sourceLen - endPosition < subLen) endPosition = sourceLen - subLen;
        if (sourceLen == 0) return endPosition;

        byte[]bytes = source.getUnsafeBytes();
        int p = source.getBegin();
        int end = p + source.getRealSize();

        final ByteList subString = subStringCodeRangeable.getByteList();
        byte[]sbytes = subString.bytes();
        subLen = subString.getRealSize();

        int s = nth(enc, bytes, p, end, endPosition);
        while (s >= 0) {
            if (ByteList.memcmp(bytes, s, sbytes, 0, subLen) == 0) return endPosition;

            if (endPosition == 0) break;
            endPosition--;

            s = enc.prevCharHead(bytes, p, s, end);
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

    public static TrTables trSetupTable(ByteList value, Ruby runtime, boolean[] table, TrTables tables, boolean init, Encoding enc) {
        final TR tr = new TR(value);
        boolean cflag = false;
        if (value.getRealSize() > 1) {
            if (enc.isAsciiCompatible()) {
                if ((value.getUnsafeBytes()[value.getBegin()] & 0xff) == '^') {
                    cflag = true;
                    tr.p++;
                }
            } else {
                int l = preciseLength(enc, tr.buf, tr.p, tr.pend);
                if (enc.mbcToCode(tr.buf, tr.p, tr.pend) == '^') {
                    cflag = true;
                    tr.p += l;
                }
            }
        }

        if (init) {
            for (int i=0; i< TRANS_SIZE; i++) table[i] = true;
            table[TRANS_SIZE] = cflag;
        } else if (table[TRANS_SIZE] && !cflag) {
            table[TRANS_SIZE] = false;
        }

        final boolean[]buf = new boolean[TRANS_SIZE];
        for (int i=0; i< TRANS_SIZE; i++) buf[i] = cflag;

        int c;
        IntHash<IRubyObject> hash = null, phash = null;
        while ((c = trNext(tr, runtime, enc)) >= 0) {
            if (c < TRANS_SIZE) {
                buf[c & 0xff] = !cflag;
            } else {
                if (hash == null) {
                    hash = new IntHash<IRubyObject>();
                    if (tables == null) tables = new TrTables();
                    if (cflag) {
                        phash = tables.noDel;
                        tables.noDel = hash;
                    } else {
                        phash  = tables.del;
                        tables.del = hash;
                    }
                }
                if (phash == null || phash.get(c) != null) hash.put(c, RubyBasicObject.NEVER);
            }
        }

        for (int i=0; i< TRANS_SIZE; i++) table[i] = table[i] && buf[i];
        return tables;
    }

    public static boolean trFind(int c, boolean[] table, TrTables tables) {
        if (c < TRANS_SIZE) {
            return table[c];
        } else {
            if (tables != null) {
                if (tables.del != null) {
                    if (tables.noDel == null || tables.noDel.get(c) == null) return true;
                } else if (tables.noDel != null && tables.noDel.get(c) != null) return false;
            }
            return table[TRANS_SIZE];
        }
    }

    public static int trNext(TR t, Ruby runtime, Encoding enc) {
        byte[]buf = t.buf;

        for (;;) {
            if (!t.gen) {
                if (t.p == t.pend) return -1;
                if (t.p < t.pend -1 && buf[t.p] == '\\') t.p++;
                t.now = codePoint(runtime, enc, buf, t.p, t.pend);
                t.p += codeLength(runtime, enc, t.now);
                if (t.p < t.pend - 1 && buf[t.p] == '-') {
                    t.p++;
                    if (t.p < t.pend) {
                        int c = codePoint(runtime, enc, buf, t.p, t.pend);
                        t.p += codeLength(runtime, enc, c);
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
            } else if (++t.now < t.max) {
                return t.now;
            } else {
                t.gen = false;
                return t.max;
            }
        }
    }

    /**
     * succ
     */

    public static enum NeighborChar {NOT_CHAR, FOUND, WRAPPED}

    public static ByteList succCommon(ByteList original) {
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
                if (ASCIIEncoding.INSTANCE.isAlpha(bytes[lastAlnum] & 0xff) ?
                        ASCIIEncoding.INSTANCE.isDigit(bytes[s] & 0xff) :
                        ASCIIEncoding.INSTANCE.isDigit(bytes[lastAlnum] & 0xff) ?
                                ASCIIEncoding.INSTANCE.isAlpha(bytes[s] & 0xff) : false) {
                    s = lastAlnum;
                    break;
                }
            }

            int cl = preciseLength(enc, bytes, s, end);
            if (cl <= 0) continue;
            switch (neighbor = succAlnumChar(enc, bytes, s, cl, carry, 0)) {
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
                neighbor = succChar(enc, bytes, s, cl);
                if (neighbor == NeighborChar.FOUND) return valueCopy;
                if (preciseLength(enc, bytes, s, s + 1) != cl) succChar(enc, bytes, s, cl); /* wrapped to \0...\0.  search next valid char. */
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

    public static NeighborChar succChar(Encoding enc, byte[] bytes, int p, int len) {
        while (true) {
            int i = len - 1;
            for (; i >= 0 && bytes[p + i] == (byte)0xff; i--) bytes[p + i] = 0;
            if (i < 0) return NeighborChar.WRAPPED;
            bytes[p + i] = (byte)((bytes[p + i] & 0xff) + 1);
            int cl = preciseLength(enc, bytes, p, p + len);
            if (cl > 0) {
                if (cl == len) {
                    return NeighborChar.FOUND;
                } else {
                    for (int j = p + cl; j < p + len - cl; j++) bytes[j] = (byte)0xff;
                }
            }
            if (cl == -1 && i < len - 1) {
                int len2 = len - 1;
                for (; len2 > 0; len2--) {
                    if (preciseLength(enc, bytes, p, p + len2) != -1) break;
                }
                for (int j = p + len2 + 1; j < p + len - (len2 + 1); j++) bytes[j] = (byte)0xff;
            }
        }
    }

    private static NeighborChar succAlnumChar(Encoding enc, byte[]bytes, int p, int len, byte[]carry, int carryP) {
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
        NeighborChar ret = succChar(enc, bytes, p, len);
        if (ret == NeighborChar.FOUND) {
            c = enc.mbcToCode(bytes, p, p + len);
            if (enc.isCodeCType(c, cType)) return NeighborChar.FOUND;
        }

        System.arraycopy(save, 0, bytes, p, len);
        int range = 1;

        while (true) {
            System.arraycopy(bytes, p, save, 0, len);
            ret = predChar(enc, bytes, p, len);
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
        succChar(enc, carry, carryP, len);
        return NeighborChar.WRAPPED;
    }

    private static NeighborChar predChar(Encoding enc, byte[]bytes, int p, int len) {
        while (true) {
            int i = len - 1;
            for (; i >= 0 && bytes[p + i] == 0; i--) bytes[p + i] = (byte)0xff;
            if (i < 0) return NeighborChar.WRAPPED;
            bytes[p + i] = (byte)((bytes[p + i] & 0xff) - 1);
            int cl = preciseLength(enc, bytes, p, p + len);
            if (cl > 0) {
                if (cl == len) {
                    return NeighborChar.FOUND;
                } else {
                    for (int j = p + cl; j < p + len - cl; j++) bytes[j] = 0;
                }
            }
            if (cl == -1 && i < len - 1) {
                int len2 = len - 1;
                for (; len2 > 0; len2--) {
                    if (preciseLength(enc, bytes, p, p + len2) != -1) break;
                }
                for (int j = p + len2 + 1; j < p + len - (len2 + 1); j++) bytes[j] = 0;
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

    public static void replaceInternal19(int beg, int len, CodeRangeable source, CodeRangeable repl) {
        Encoding enc = source.checkEncoding(repl);
        int p = source.getByteList().getBegin();
        int e;
        if (isSingleByteOptimizable(source, source.getByteList().getEncoding())) {
            p += beg;
            e = p + len;
        } else {
            int end = p + source.getByteList().getRealSize();
            byte[]bytes = source.getByteList().getUnsafeBytes();
            p = StringSupport.nth(enc, bytes, p, end, beg);
            if (p == -1) p = end;
            e = StringSupport.nth(enc, bytes, p, end, len);
            if (e == -1) e = end;
        }

        int cr = source.getCodeRange();
        if (cr == CR_BROKEN) source.clearCodeRange();
        replaceInternal(p - source.getByteList().getBegin(), e - p, source, repl);
        associateEncoding(source, enc);
        cr = CodeRangeSupport.codeRangeAnd(cr, repl.getCodeRange());
        if (cr != CR_BROKEN) source.setCodeRange(cr);
    }

    public static boolean isAsciiOnly(CodeRangeable string) {
        return string.getByteList().getEncoding().isAsciiCompatible() && string.scanForCodeRange() == CR_7BIT;
    }
}
