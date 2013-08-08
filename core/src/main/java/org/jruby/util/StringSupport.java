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
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import sun.misc.Unsafe;

public final class StringSupport {
    public static final int CR_MASK      = RubyObject.USER0_F | RubyObject.USER1_F;  
    public static final int CR_UNKNOWN   = 0;
    public static final int CR_7BIT      = RubyObject.USER0_F; 
    public static final int CR_VALID     = RubyObject.USER1_F;
    public static final int CR_BROKEN    = RubyObject.USER0_F | RubyObject.USER1_F;

    public static final Object UNSAFE = getUnsafe();
    private static final int OFFSET = UNSAFE != null ? ((Unsafe)UNSAFE).arrayBaseOffset(byte[].class) : 0;

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
        if (n > end - p) return -1 - (n - (end - p));
        return n;
    }
    
    // MBCLEN_NEEDMORE_P, ONIGENC_MBCLEN_NEEDMORE_P
    public static boolean MBCLEN_NEEDMORE_P(int r) {
        return r < -1;
    }
    
    // MBCLEN_INVALID_P, ONIGENC_MBCLEN_INVALID_P
    public static boolean MBCLEN_INVALID_P(int r) {
        return r == -1;
    }
    
    // MBCLEN_CHARFOUND_LEN, ONIGENC_CHARFOUND_LEN
    public static int MBCLEN_CHARFOUND_LEN(int r) {
        return r;
    }
    
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
                    len += countUtf8LeadBytes(us.getLong(bytes, OFFSET + p));
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
        if (l > 0) enc.mbcToCode(bytes, p, end);
        return -1;
    }

    @SuppressWarnings("deprecation")
    public static int utf8Nth(byte[]bytes, int p, int end, int n) {
        if (UNSAFE != null) {
            if (n > LONG_SIZE * 2) {
                int ep = ~LOWBITS & (p + LOWBITS);
                while (p < ep) {
                    if ((bytes[p++] & 0xc0 /*utf8 lead byte*/) != 0x80) n--;
                }
                Unsafe us = (Unsafe)UNSAFE;
                int eend = ~LOWBITS & end;
                do {
                    n -= countUtf8LeadBytes(us.getLong(bytes, OFFSET + p));
                    p += LONG_SIZE;
                } while (p < eend && n >= LONG_SIZE);
            }
        }
        while (p < end) {
            if ((bytes[p] & 0xc0 /*utf8 lead byte*/) != 0x80) {
                if (n-- == 0) break;
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

}
