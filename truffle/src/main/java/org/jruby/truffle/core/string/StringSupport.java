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
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jcodings.Encoding;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.constants.CharacterType;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.util.IntHash;
import org.jruby.truffle.collections.IntHashMap;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jruby.truffle.core.rope.CodeRange.CR_UNKNOWN;
import static org.jruby.truffle.core.rope.CodeRange.CR_7BIT;
import static org.jruby.truffle.core.rope.CodeRange.CR_VALID;
import static org.jruby.truffle.core.rope.CodeRange.CR_BROKEN;

public final class StringSupport {
    public static final int TRANS_SIZE = 256;

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Split string into sub-parts.
     * @param str the string
     * @param sep the separator
     * @see String#split(String)
     *
     * @note We differ from the non-limited {@link String#split(String)} in handling consecutive separator chars at the
     * end of string. While <code>"1;;"split(";")<code/> returns `[ "1" ]` this version returns `[ "1", "" ]` which is
     * consistent when consecutive separators occur elsewhere.
     */
    public static List<String> split(final String str, final char sep) {
        return split(str, sep, 0);
    }

    /**
     * Split string into (limited) sub-parts.
     * @param str the string
     * @param sep the separator
     * @param lim has same effect as with {@link String#split(String, int)}
     */
    public static List<String> split(final String str, final char sep, final int lim) {
        final int len = str.length();
        if ( len == 0 ) return Collections.singletonList(str);

        final ArrayList<String> result = new ArrayList<>(lim <= 0 ? 8 : lim);

        int e; int s = 0; int count = 0;
        while ( (e = str.indexOf(sep, s)) != -1 ) {
            if ( lim == ++count ) { // limited (lim > 0) case
                result.add(str.substring(s));
                return result;
            }
            result.add(str.substring(s, e));
            s = e + 1;
        }
        if ( s < len || ( s == len && lim > 0 ) ) result.add(str.substring(s));

        return result;
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
    @TruffleBoundary
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

    // MBCLEN_NEEDMORE_LEN, ONIGENC_MBCLEN_NEEDMORE_LEN
    public static int MBCLEN_NEEDMORE_LEN(int r) {
        return -1 - r;
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

    // MRI: search_nonascii
    public static int searchNonAscii(byte[]bytes, int p, int end) {
        while (p < end) {
            if (!Encoding.isAscii(bytes[p])) return p;
            p++;
        }
        return -1;
    }

    public static CodeRange codeRangeScan(Encoding enc, byte[]bytes, int p, int len) {
        if (enc == ASCIIEncoding.INSTANCE) {
            return searchNonAscii(bytes, p, p + len) != -1 ? CR_VALID : CR_7BIT;
        }
        if (enc.isAsciiCompatible()) {
            return codeRangeScanAsciiCompatible(enc, bytes, p, len);
        }
        return codeRangeScanNonAsciiCompatible(enc, bytes, p, len);
    }

    private static CodeRange codeRangeScanAsciiCompatible(Encoding enc, byte[]bytes, int p, int len) {
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

    private static CodeRange codeRangeScanNonAsciiCompatible(Encoding enc, byte[]bytes, int p, int len) {
        int end = p + len;
        while (p < end) {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl <= 0) return CR_BROKEN;
            p += cl;
        }
        return p > end ? CR_BROKEN : CR_VALID;
    }

    public static CodeRange codeRangeScan(Encoding enc, ByteList bytes) {
        return codeRangeScan(enc, bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
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
    public static int utf8Length(byte[] bytes, int p, int end) {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, end);
        int len = 0;
        if (end - p > LONG_SIZE * 2) {
            int ep = ~LOWBITS & (p + LOWBITS);
            while (p < ep) {
                if ((bytes[p++] & 0xc0 /*utf8 lead byte*/) != 0x80) len++;
            }
            int eend = ~LOWBITS & end;
            while (p < eend) {
                len += countUtf8LeadBytes(buffer.getLong(p));
                p += LONG_SIZE;
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
    public static int strLength(Encoding enc, byte[]bytes, int p, int e, CodeRange cr) {
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

    public static long strLengthWithCodeRange(Encoding enc, byte[]bytes, int p, int end) {
        if (enc.isFixedWidth()) {
            return (end - p + enc.minLength() - 1) / enc.minLength();
        } else if (enc.isAsciiCompatible()) {
            return strLengthWithCodeRangeAsciiCompatible(enc, bytes, p, end);
        } else {
            return strLengthWithCodeRangeNonAsciiCompatible(enc, bytes, p, end);
        }
    }

    public static long strLengthWithCodeRangeAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
        CodeRange cr = CR_UNKNOWN;
        int c = 0;
        while (p < end) {
            if (Encoding.isAscii(bytes[p])) {
                int q = searchNonAscii(bytes, p, end);
                if (q == -1) return pack(c + (end - p), cr == CR_UNKNOWN ? CR_7BIT.toInt() : cr.toInt());
                c += q - p;
                p = q;
            }
            int cl = preciseLength(enc, bytes, p, end);
            if (cl > 0) {
                if (cr != CR_BROKEN) {
                    cr = CR_VALID;
                }
                p += cl;
            } else {
                cr = CR_BROKEN;
                p++;
            }
            c++;
        }
        return pack(c, cr == CR_UNKNOWN ? CR_7BIT.toInt() : cr.toInt());
    }

    public static long strLengthWithCodeRangeNonAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
        CodeRange cr = CR_UNKNOWN;
        int c;
        for (c = 0; p < end; c++) {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl > 0) {
                if (cr != CR_BROKEN) {
                    cr = CR_VALID;
                }
                p += cl;
            } else {
                cr = CR_BROKEN;
                p++;
            }
        }
        return pack(c, cr == CR_UNKNOWN ? CR_7BIT.toInt() : cr.toInt());
    }

    public static long strLengthWithCodeRange(ByteList bytes, Encoding enc) {
        return strLengthWithCodeRange(enc, bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
    }

    // arg cannot be negative
    public static long pack(int result, int arg) {
        return ((long)arg << 31) | result;
    }

    public static int unpackResult(long len) {
        return (int)len & 0x7fffffff;
    }

    public static int unpackArg(long cr) {
        return (int)(cr >>> 31);
    }

    public static int codePoint(Encoding enc, byte[] bytes, int p, int end) {
        if (p >= end) throw new IllegalArgumentException("empty string");
        int cl = preciseLength(enc, bytes, p, end);
        if (cl <= 0) throw new IllegalArgumentException("invalid byte sequence in " + enc);
        return enc.mbcToCode(bytes, p, end);
    }

    public static int codeLength(Encoding enc, int c) {
        return enc.codeToMbcLength(c);
    }

    public static int preciseCodePoint(Encoding enc, byte[]bytes, int p, int end) {
        int l = preciseLength(enc, bytes, p, end);
        if (l > 0) return enc.mbcToCode(bytes, p, end);
        return -1;
    }

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

    /**
     * rb_str_count
     */
    public static int strCount(ByteList str, boolean[] table, TrTables tables, Encoding enc) {
        final byte[] bytes = str.getUnsafeBytes();
        int p = str.getBegin();
        final int end = p + str.getRealSize();
        final boolean asciiCompat = enc.isAsciiCompatible();

        int count = 0;
        while (p < end) {
            int c;
            if (asciiCompat && (c = bytes[p] & 0xff) < 0x80) {
                if (table[c]) count++;
                p++;
            } else {
                c = codePoint(enc, bytes, p, end);
                int cl = codeLength(enc, c);
                if (trFind(c, table, tables)) count++;
                p += cl;
            }
        }

        return count;
    }

    public static int strLengthFromRubyString(CodeRangeable string) {
        final ByteList bytes = string.getByteList();

        if (isSingleByteOptimizable(string, bytes.getEncoding())) return bytes.getRealSize();
        return strLengthFromRubyStringFull(string, bytes, bytes.getEncoding());
    }

    private static int strLengthFromRubyStringFull(CodeRangeable string, ByteList bytes, Encoding enc) {
        if (string.isCodeRangeValid() && enc.isUTF8()) return utf8Length(bytes);

        long lencr = strLengthWithCodeRange(bytes, enc);
        CodeRange cr = CodeRange.fromInt(unpackArg(lencr));
        if (cr != CR_UNKNOWN) string.setCodeRange(cr);
        return unpackResult(lencr);
    }

    /**
     * rb_str_tr / rb_str_tr_bang
     */
    public static final class TR {
        public TR(ByteList bytes) {
            p = bytes.getBegin();
            pend = bytes.getRealSize() + p;
            buf = bytes.getUnsafeBytes();
            now = max = 0;
            gen = false;
        }

        public TR(Rope bytes) {
            p = 0;
            pend = bytes.byteLength() + p;
            buf = bytes.getBytes();
            now = max = 0;
            gen = false;
        }

        final byte[] buf;
        int p, pend, now, max;
        boolean gen;
    }

    /**
     * tr_setup_table
     */
    public static final class TrTables {
        IntHashMap<Object> del, noDel; // used as ~ Set
    }

    private static final Object DUMMY_VALUE = "";

    public static TrTables trSetupTable(final Rope str,
                                        final boolean[] stable, TrTables tables, final boolean first, final Encoding enc) {
        int i, l[] = {0};
        final boolean cflag;

        final TR tr = new TR(str);

        if (str.byteLength() > 1 && EncodingUtils.encAscget(tr.buf, tr.p, tr.pend, l, enc) == '^') {
            cflag = true;
            tr.p += l[0];
        }
        else {
            cflag = false;
        }

        if (first) {
            for ( i = 0; i < TRANS_SIZE; i++) stable[i] = true;
            stable[TRANS_SIZE] = cflag;
        }
        else if (stable[TRANS_SIZE] && !cflag) {
            stable[TRANS_SIZE] = false;
        }

        if (tables == null) tables = new TrTables();

        byte[] buf = null; // lazy initialized
        IntHashMap<Object> table = null, ptable = null;

        int c;
        while ((c = trNext(tr, enc)) != -1) {
            if (c < TRANS_SIZE) {
                if ( buf == null ) { // initialize buf
                    buf = new byte[TRANS_SIZE];
                    for ( i = 0; i < TRANS_SIZE; i++ ) {
                        buf[i] = (byte) (cflag ? 1 : 0);
                    }
                }
                // update the buff at [c] :
                buf[c & 0xff] = (byte) (cflag ? 0 : 1);
            }
            else {
                if ( table == null && (first || tables.del != null || stable[TRANS_SIZE]) ) {
                    if ( cflag ) {
                        ptable = tables.noDel;
                        table = ptable != null ? ptable : new IntHashMap<>(8);
                        tables.noDel = table;
                    }
                    else {
                        table = new IntHashMap<>(8);
                        ptable = tables.del;
                        tables.del = table;
                    }
                }

                if ( table != null ) {
                    final int key = c;
                    if ( ptable == null ) table.put(key, DUMMY_VALUE);
                    else {
                        if ( cflag ) table.put(key, DUMMY_VALUE);
                        else {
                            final boolean val = ptable.get(key) != null;
                            table.put(key, val ? DUMMY_VALUE : null);
                        }
                    }
                }
            }
        }
        if ( buf != null ) {
            for ( i = 0; i < TRANS_SIZE; i++ ) {
                stable[i] = stable[i] && buf[i] != 0;
            }
        }
        else {
            for ( i = 0; i < TRANS_SIZE; i++ ) {
                stable[i] = stable[i] && cflag;
            }
        }

        if ( table == null && ! cflag ) tables.del = null;

        return tables;
    }

    public static boolean trFind(final int c, final boolean[] table, final TrTables tables) {
        if (c < TRANS_SIZE) return table[c];

        final IntHashMap<Object> del = tables.del, noDel = tables.noDel;

        if (del != null) {
            if (del.get(c) != null &&
                (noDel == null || noDel.get(c) == null)) {
                return true;
            }
        }
        else if (noDel != null && noDel.get(c) != null) {
            return false;
        }

        return table[TRANS_SIZE];
    }

    public static int trNext(final TR tr, Encoding enc) {
        for (;;) {
            if ( ! tr.gen ) {
                return trNext_nextpart(tr, enc);
            }

            while (enc.codeToMbcLength( ++tr.now ) <= 0) {
                if (tr.now == tr.max) {
                    tr.gen = false;
                    return trNext_nextpart(tr, enc);
                }
            }
            if (tr.now < tr.max) {
                return tr.now;
            } else {
                tr.gen = false;
                return tr.max;
            }
        }
    }

    private static int trNext_nextpart(final TR tr, Encoding enc) {
        final int[] n = {0};

        if (tr.p == tr.pend) return -1;
        if (EncodingUtils.encAscget(tr.buf, tr.p, tr.pend, n, enc) == '\\' && tr.p + n[0] < tr.pend) {
            tr.p += n[0];
        }
        tr.now = EncodingUtils.encCodepointLength(tr.buf, tr.p, tr.pend, n, enc);
        tr.p += n[0];
        if (EncodingUtils.encAscget(tr.buf, tr.p, tr.pend, n, enc) == '-' && tr.p + n[0] < tr.pend) {
            tr.p += n[0];
            if (tr.p < tr.pend) {
                int c = EncodingUtils.encCodepointLength(tr.buf, tr.p, tr.pend, n, enc);
                tr.p += n[0];
                if (tr.now > c) {
                    if (tr.now < 0x80 && c < 0x80) {
                        throw new IllegalArgumentException("invalid range \""
                                + (char) tr.now + '-' + (char) c + "\" in string transliteration");
                    }

                    throw new IllegalArgumentException("invalid range in string transliteration");
                }
                tr.gen = true;
                tr.max = c;
            }
        }
        return tr.now;
    }

    public enum NeighborChar {NOT_CHAR, FOUND, WRAPPED}

    // MRI: str_succ
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

    // MRI: enc_succ_char
    public static NeighborChar succChar(Encoding enc, byte[] bytes, int p, int len) {
        int l;
        if (enc.minLength() > 1) {
	        /* wchar, trivial case */
            int r = preciseLength(enc, bytes, p, p + len), c;
            if (!MBCLEN_CHARFOUND_P(r)) {
                return NeighborChar.NOT_CHAR;
            }
            c = codePoint(enc, bytes, p, p + len) + 1;
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
        int l;
        if (enc.minLength() > 1) {
	        /* wchar, trivial case */
            int r = preciseLength(enc, bytes, p, p + len), c;
            if (!MBCLEN_CHARFOUND_P(r)) {
                return NeighborChar.NOT_CHAR;
            }
            c = codePoint(enc, bytes, p, p + len);
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
        byte[] bytes = source.getUnsafeBytes();
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
            if ((string.getCodeRange() != CR_7BIT) || !enc.isAsciiCompatible()) string.clearCodeRange();
            value.setEncoding(enc);
        }
    }

    /**
     * rb_str_delete_bang
     */
    public static CodeRangeable delete_bangCommon19(CodeRangeable rubyString, boolean[] squeeze, TrTables tables, Encoding enc) {
        rubyString.modify();
        rubyString.keepCodeRange();

        final ByteList value = rubyString.getByteList();

        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        boolean modify = false;
        boolean asciiCompatible = enc.isAsciiCompatible();
        CodeRange cr = asciiCompatible ? CR_7BIT : CR_VALID;
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
                c = codePoint(enc, bytes, s, send);
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

    public static ByteList addByteLists(ByteList value1, ByteList value2) {
        ByteList result = new ByteList(value1.getRealSize() + value2.getRealSize());
        result.setRealSize(value1.getRealSize() + value2.getRealSize());
        System.arraycopy(value1.getUnsafeBytes(), value1.getBegin(), result.getUnsafeBytes(), 0, value1.getRealSize());
        System.arraycopy(value2.getUnsafeBytes(), value2.getBegin(), result.getUnsafeBytes(), value1.getRealSize(), value2.getRealSize());
        return result;
    }

    /**
     * rb_str_tr / rb_str_tr_bang
     */

    public static CodeRangeable trTransHelper(CodeRangeable self, CodeRangeable srcStr, CodeRangeable replStr, boolean sflag) {
        // This method does not handle the cases where either srcStr or replStr are empty.  It is the responsibility
        // of the caller to take the appropriate action in those cases.

        final ByteList srcList = srcStr.getByteList();
        final ByteList replList = replStr.getByteList();

        CodeRange cr = self.getCodeRange();
        Encoding e1 = self.checkEncoding(srcStr);
        Encoding e2 = self.checkEncoding(replStr);
        Encoding enc = e1 == e2 ? e1 : srcStr.checkEncoding(replStr);

        final StringSupport.TR trSrc = new StringSupport.TR(srcList);
        boolean cflag = false;
        int[] l = {0};

        if (srcStr.getByteList().getRealSize() > 1 &&
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
            for (int i = 0; i< StringSupport.TRANS_SIZE; i++) {
                trans[i] = 1;
            }

            while ((c = StringSupport.trNext(trSrc, enc)) != -1) {
                if (c < StringSupport.TRANS_SIZE) {
                    trans[c] = -1;
                } else {
                    if (hash == null) hash = new IntHash<>();
                    hash.put(c, 1); // QTRUE
                }
            }
            while ((c = StringSupport.trNext(trRepl, enc)) != -1) {}  /* retrieve last replacer */
            last = trRepl.now;
            for (int i = 0; i< StringSupport.TRANS_SIZE; i++) {
                if (trans[i] != -1) {
                    trans[i] = last;
                }
            }
        } else {
            for (int i = 0; i< StringSupport.TRANS_SIZE; i++) {
                trans[i] = -1;
            }

            while ((c = StringSupport.trNext(trSrc, enc)) != -1) {
                int r = StringSupport.trNext(trRepl, enc);
                if (r == -1) r = trRepl.now;
                if (c < StringSupport.TRANS_SIZE) {
                    trans[c] = r;
                    if (codeLength(enc, r) != 1) singlebyte = false;
                } else {
                    if (hash == null) hash = new IntHash<>();
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
                c0 = c = codePoint(e1, sbytes, s, send);
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
                c0 = c = codePoint(e1, sbytes, s, send);
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

    @TruffleBoundary
    public static int multiByteCasecmp(Encoding enc, Rope value, Rope otherValue) {
        byte[]bytes = value.getBytes();
        int p = 0;
        int end = value.byteLength();

        byte[]obytes = otherValue.getBytes();
        int op = 0;
        int oend = otherValue.byteLength();

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
        final byte[] bytes = value.getUnsafeBytes();
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

    public static boolean multiByteSqueeze(ByteList value, boolean squeeze[], TrTables tables, Encoding enc, boolean isArg) {
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
                c = codePoint(enc, bytes, s, send);
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

    public static boolean multiByteSwapcase(Encoding enc, byte[] bytes, int s, int end) {
        boolean modify = false;
        while (s < end) {
            int c = codePoint(enc, bytes, s, end);
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

    public static boolean singleByteDowncase(byte[] bytes, int s, int end) {
        boolean modify = false;

        while (s < end) {
            int c = bytes[s] & 0xff;
            if (ASCIIEncoding.INSTANCE.isUpper(c)) {
                bytes[s] = AsciiTables.ToLowerCaseTable[c];
                modify = true;
            }
            s++;
        }

        return modify;
    }

    public static boolean multiByteDowncase(Encoding enc, byte[] bytes, int s, int end) {
        boolean modify = false;
        int c;
        while (s < end) {
            if (enc.isAsciiCompatible() && Encoding.isAscii(c = bytes[s] & 0xff)) {
                if (ASCIIEncoding.INSTANCE.isUpper(c)) {
                    bytes[s] = AsciiTables.ToLowerCaseTable[c];
                    modify = true;
                }
                s++;
            } else {
                c = codePoint(enc, bytes, s, end);
                if (enc.isUpper(c)) {
                    enc.codeToMbc(toLower(enc, c), bytes, s);
                    modify = true;
                }
                s += codeLength(enc, c);
            }
        }

        return modify;
    }

    public static boolean singleByteUpcase(byte[] bytes, int s, int end) {
        boolean modify = false;

        while (s < end) {
            int c = bytes[s] & 0xff;
            if (ASCIIEncoding.INSTANCE.isLower(c)) {
                bytes[s] = AsciiTables.ToUpperCaseTable[c];
                modify = true;
            }
            s++;
        }

        return modify;
    }

    public static boolean multiByteUpcase(Encoding enc, byte[] bytes, int s, int end) {
        boolean modify = false;
        int c;

        while (s < end) {
            if (enc.isAsciiCompatible() && Encoding.isAscii(c = bytes[s] & 0xff)) {
                if (ASCIIEncoding.INSTANCE.isLower(c)) {
                    bytes[s] = AsciiTables.ToUpperCaseTable[c];
                    modify = true;
                }
                s++;
            } else {
                c = codePoint(enc, bytes, s, end);
                if (enc.isLower(c)) {
                    enc.codeToMbc(toUpper(enc, c), bytes, s);
                    modify = true;
                }
                s += codeLength(enc, c);
            }
        }

        return modify;
    }
}
