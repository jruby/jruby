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

import org.jcodings.Config;
import org.jcodings.Encoding;
import org.jcodings.IntHolder;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.constants.CharacterType;
import org.jcodings.exception.EncodingError;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.IntHash;
import org.joni.Matcher;
import org.jruby.ObjectFlags;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyEncoding;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.api.Check;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.IntHashMap;
import org.jruby.util.io.EncodingUtils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jruby.RubyString.scanForCodeRange;
import static org.jruby.api.Access.encodingService;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.indexError;
import static org.jruby.api.Warn.warn;

public final class StringSupport {
    public static final int CR_7BIT_F    = ObjectFlags.CR_7BIT_F;
    public static final int CR_VALID_F   = ObjectFlags.CR_VALID_F;
    public static final int CR_UNKNOWN   = 0;

    // We hardcode these so they can be used in a switch.
    // These values also must continue to match the same values in the Prism parser so we perform a hard check.
    public static final int CR_7BIT      = 16;
    public static final int CR_VALID     = 32;
    static {
        if (CR_7BIT != CR_7BIT_F) throw new RuntimeException("BUG: CR_7BIT_F = " + CR_7BIT_F + " but should be " + CR_7BIT);
        if (CR_VALID != CR_VALID_F) throw new RuntimeException("BUG: CR_VALID_F = " + CR_VALID_F + " but should be " + CR_VALID);
    }

    public static final int CR_BROKEN    = CR_7BIT | CR_VALID;
    public static final int CR_MASK      = CR_7BIT | CR_VALID;

    private static final VarHandle BYTES_AS_LONGS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());

    public static final int TRANS_SIZE = 256;

    public static final ByteList[] EMPTY_BYTELIST_ARRAY = new ByteList[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Split string into sub-parts.
     * @param str the string
     * @param sep the separator
     * @see String#split(String)
     *
     * <p>Note: We differ from the non-limited {@link String#split(String)} in handling consecutive separator chars at the</p>
     * end of string. While <code>"1;;"split(";")</code> returns `[ "1" ]` this version returns `[ "1", "" ]` which is
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

    // String.startsWith for a CharSequence
    public static boolean startsWith(final CharSequence str, final String prefix) {
        int p = prefix.length();
        if ( p > str.length() ) return false;
        int i = 0;
        while ( --p >= 0 ) {
            if (str.charAt(i) != prefix.charAt(i)) return false;
            i++;
        }
        return true;
    }

    public static boolean startsWith(final CharSequence str, final char c) {
        return str.length() >= 1 && str.charAt(0) == c;
    }

    public static boolean startsWith(final CharSequence str, final char c1, final char c2) {
        return str.length() >= 2 && str.charAt(0) == c1 && str.charAt(1) == c2;
    }

    // without any char[] array copying, also StringBuilder only has lastIndexOf(String)
    public static int lastIndexOf(final CharSequence str, final char c, int index) {
        while ( index >= 0 ) {
            if ( str.charAt(index) == c ) return index;
            index--;
        }
        return -1;
    }

    public static boolean contentEquals(final CharSequence str, final int chr) {
        return (str.length() == 1) && str.charAt(0) == chr;
    }

    public static boolean contentEquals(final CharSequence str, final int chr1, final int chr2) {
        return (str.length() == 2) && str.charAt(0) == chr1 && str.charAt(1) == chr2;
    }

    public static CharSequence concat(final CharSequence str1, final CharSequence str2) {
        return new StringBuilder(str1.length() + str2.length()).append(str1).append(str2);
    }

    public static String concat(final String str1, final String str2) {
        return new StringBuilder(str1.length() + str2.length()).append(str1).append(str2).toString();
    }

    public static String delete(final String str, final char c) { // str.replaceAll(c.toString(), "")
        char[] ary = null; int end = 0, s = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                if (ary == null) {
                    ary = new char[str.length() - 1];
                }
                end = copy(str, s, i - s, ary, end);
                s = i + 1;
            }
        }
        return ary == null ? str : new String(ary, 0, end);
    }

    public static CharSequence replaceFirst(final String str, final String sub, final String repl) {
        return replaceImpl(str, sub, repl, 1, false);
    }

    public static CharSequence replaceAll(final String str, final String sub, final String repl) {
        return replaceImpl(str, sub, repl, -1, false);
    }

    // borrowed from commons-lang StringUtils
    private static CharSequence replaceImpl(final String str, String sub, final String repl, int max, final boolean ignoreCase) {
        if (str.length() == 0 || sub.length() == 0) return str;

        String search = str;
        if (ignoreCase) {
            search = str.toLowerCase();
            sub = sub.toLowerCase();
        }
        int start = 0;
        int end = search.indexOf(sub, start);
        if (end == -1) return str;

        final int replLength = sub.length();
        int increase = repl.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
        final StringBuilder buf = new StringBuilder(str.length() + increase);
        while (end != -1) {
            buf.append(str, start, end).append(repl);
            start = end + replLength;
            if (--max == 0) break;
            end = search.indexOf(sub, start);
        }
        buf.append(str, start, str.length());
        return buf;
    }

    private static int copy(final String str, final int soff, final int slen, final char[] dest, int doff) {
        switch(slen) {
            case 0:
                break;
            case 1:
                dest[doff++] = str.charAt(soff);
                break;
            case 2:
                dest[doff++] = str.charAt(soff);
                dest[doff++] = str.charAt(soff + 1);
                break;
            case 3:
                dest[doff++] = str.charAt(soff);
                dest[doff++] = str.charAt(soff + 1);
                dest[doff++] = str.charAt(soff + 2);
                break;
            default:
                for (int s = soff; s < slen; s++) dest[doff++] = str.charAt(s);
        }
        return doff;
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
        if (p >= end) return MBCLEN_NEEDMORE(1);
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

    public static int searchNonAscii(String string) {
        for (int p = 0; p < string.length(); p++) {
            if (!Encoding.isAscii(string.charAt(p))) return p;
        }
        return -1;
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
    public static int utf8Length(byte[] bytes, int p, int end) {
        int len = 0;
        if (end - p > LONG_SIZE * 2) {
            int ep = ~LOWBITS & (p + LOWBITS);
            while (p < ep) {
                if ((bytes[p++] & 0xc0 /*utf8 lead byte*/) != 0x80) len++;
            }
            int eend = ~LOWBITS & end;
            while (p < eend) {
                len += countUtf8LeadBytes((long) BYTES_AS_LONGS.get(bytes, p));
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

    public static long strLengthWithCodeRangeAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
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

    public static long strLengthWithCodeRangeNonAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
        int cr = 0, c;
        for (c = 0; p < end; c++) {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl > 0) {
                cr |= CR_VALID;
                p += cl;
            } else {
                cr = CR_BROKEN;
                if (p + enc.minLength() <= end) {
                    p += enc.minLength();
                } else {
                    p = end;
                }
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

    @Deprecated(since = "10.0")
    public static int codePoint(Ruby runtime, Encoding enc, byte[] bytes, int p, int end) {
        return codePoint(runtime.getCurrentContext(), enc, bytes, p, end);
    }

    public static int codePoint(ThreadContext context, Encoding enc, byte[] bytes, int p, int end) {
        try {
            return codePoint(enc, bytes, p, end);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, e.getMessage());
        }
    }

    @Deprecated(since = "10.0")
    public static int codePoint(final Ruby runtime, final ByteList value) {
        return codePoint(runtime.getCurrentContext(), value);
    }

    public static int codePoint(ThreadContext context, final ByteList value) {
        return codePoint(context, EncodingUtils.getEncoding(value),
                value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize());
    }

    public static int codeLength(Encoding enc, int c) {
        int i = enc.codeToMbcLength(c);
        return checkCodepointError(i);
    }

    public static int checkCodepointError(int i) {
        if (i < 0) {
            // for backward compat with code expecting exceptions
            throw new EncodingException(EncodingError.fromCode(i));
        }
        return i;
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

    /**
     * Return the byte offset of the nth character {@code n} in the given byte array between {@code p} and {@code n} using
     * {@code enc} as the encoding.
     *
     * Note that the resulting offset will absolute, and therefore >= {@code p}. Subtract {@code p} to get a relative
     * offset.
     *
     * @param enc the encoding of the characters in the byte array
     * @param bytes the byte array
     * @param p starting offset
     * @param end limit offset
     * @param n character offset to find
     * @return the byte offset of the requested character, or -1 if the requested character offset is outside
     *         the given byte offset range
     */
    public static int nth(Encoding enc, byte[]bytes, int p, int end, int n) {
        return nth(enc, bytes, p, end, n, enc.isSingleByte());
    }

    public static int nth(Encoding enc, ByteList value, int n) {
        return nth(enc, value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), n);
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

    @Deprecated
    public static int toLower(Encoding enc, int c) {
        return Encoding.isAscii(c) ? AsciiTables.ToLowerCaseTable[c] : c;
    }

    @Deprecated
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
     * throw ArgumentError.
     * @param runtime
     * @param value
     */
    public static void checkStringSafety(Ruby runtime, IRubyObject value) {
        RubyString s = value.asString();

        if (s.getCodeRange() != CR_7BIT) {
            checkStringSafetyMBC(runtime, s);
            return;
        }

        ByteList bl = s.getByteList();
        final byte[] array = bl.getUnsafeBytes();
        final int end = bl.length();
        for (int i = bl.begin(); i < end; ++i) {
            if (array[i] == (byte) 0) {
                throw argumentError(runtime.getCurrentContext(), "string contains null byte");
            }
        }
    }

    public static void checkStringSafetyMBC(Ruby runtime, RubyString value) {
        ByteList bl = value.getByteList();
        final byte[] bytes = bl.getUnsafeBytes();
        int len = bl.realSize();
        int end = bl.begin() + len;
        Encoding enc = bl.getEncoding();
        int cl;

        for (int p = bl.begin(); p < end; p += cl) {
            cl = preciseLength(enc, bytes, p, end);
            if (cl <= 0) return;
            if (codePoint(enc, bytes, p, end) == 0) throw argumentError(runtime.getCurrentContext(), "string contains null byte");
        }
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

    @Deprecated(since = "10.0")
    public static int memchr(byte[] ptr, int start, final int find, int len) {
        return Helpers.memchr(ptr, start, find, len);
    }

    // MRI: StringValueCStr, rb_string_value_cstr without trailing null addition
    @Deprecated(since = "10.0")
    public static RubyString checkEmbeddedNulls(Ruby runtime, IRubyObject ptr) {
        return Check.checkEmbeddedNulls(runtime.getCurrentContext(), ptr);
    }

    // MRI: str_null_check without trailing null check (JVM arrays do not null terminate)
    // This function returns Java Object Array, with index0 is RubyString and index1 is Boolean.
    // Boolean corresponds to int *w arg of str_null_check.
    public static Object [] strNullCheck(IRubyObject ptr) {
        final RubyString s = ptr.convertToString();
        ByteList sByteList = s.getByteList();
        byte[] sBytes = sByteList.unsafeBytes();
        int beg = sByteList.begin();
        int len = sByteList.length();
        final Encoding enc = s.getEncoding();
        final int minlen = enc.minLength();

        if (minlen > 1) {
            if (strNullChar(sBytes, beg, len, minlen, enc) != -1) {
                return new Object[] {null, true};
            }
            return new Object[] {strFillTerm(s, sBytes, beg, len, minlen), true};
        }

        if (Helpers.memchr(sBytes, beg, '\0', len) != -1) {
            return new Object[] {null, false};
        }

        return new Object[] {s, false};
    }

    // MRI: str_null_char
    private static int strNullChar(byte[] sBytes, int s, int len, final int minlen, Encoding enc) {
        int e = s + len;

        for (; s + minlen <= e; s += enc.length(sBytes, s, e)) {
            if (zeroFilled(sBytes, s, minlen)) return s;
        }
        return -1;
    }

    // MRI: zero_filled
    private static boolean zeroFilled(byte[] sBytes, int s, int n) {
        for (; n > 0; --n) {
            if (sBytes[s++] != 0) return false;
        }
        return true;
    }

    // MRI: str_fill_term
    private static RubyString strFillTerm(RubyString str, byte[] sBytes, int beg, int len, int termlen) {
        int capa = sBytes.length - beg;

        if (capa < len + termlen) {
            // rb_check_lockedtmp(str);
            str = str.makeIndependent(len + termlen);
            sBytes = str.getByteList().unsafeBytes();
            beg = str.getByteList().begin();
        }
        else if ( ! str.independent() ) {
            if ( ! zeroFilled(sBytes, beg + len, termlen) ) {
                str = str.makeIndependent(len + termlen);
                sBytes = str.getByteList().unsafeBytes();
                beg = str.getByteList().begin();
            }
        }

        TERM_FILL(sBytes, beg, len, termlen);
        return str;
    }

    private static void TERM_FILL(byte[] ptr, final int beg, final int len, final int termlen) {
        final int p = beg + len; Arrays.fill(ptr, p, p + termlen, (byte) '\0');
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
    public static ByteList dumpCommon(Ruby runtime, ByteList bytelist) {
        return dumpCommon(runtime, bytelist, false);
    }

    public static ByteList dumpCommon(Ruby runtime, ByteList byteList, boolean quoteOnlyIfNeeded) {
        Encoding enc = byteList.getEncoding();
        boolean includingsNonprintable = false;

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
                    includingsNonprintable = true;
                    if (enc.isUTF8() && c > 0x7F) {
                        int n = preciseLength(enc, bytes, p - 1, end) - 1;
                        if (MBCLEN_CHARFOUND_LEN(n) > 0) {
                            int cc = codePoint(runtime, enc, bytes, p - 1, end);
                            if (cc <= 0xFFFF) {
                                len += 6;
                            } else if (cc <= 0xFFFFF) {
                                len += 9;
                            } else {
                                len += 10;
                            }
                            p += MBCLEN_CHARFOUND_LEN(n) - 1;
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

        if (!quoteOnlyIfNeeded || includingsNonprintable) out[q++] = '"';
        while (p < end) {
            int c = bytes[p++] & 0xff;
            switch (c) {
                case '"': case '\\':
                    out[q++] = '\\'; out[q++] = (byte)c; break;
                case '#':
                    if (isEVStr(bytes, p, end)) out[q++] = '\\';
                    out[q++] = '#';
                    break;
                case '\n':
                    out[q++] = '\\'; out[q++] = 'n'; break;
                case '\r':
                    out[q++] = '\\'; out[q++] = 'r'; break;
                case '\t':
                    out[q++] = '\\'; out[q++] = 't'; break;
                case '\f':
                    out[q++] = '\\'; out[q++] = 'f'; break;
                case '\013':
                    out[q++] = '\\'; out[q++] = 'v'; break;
                case '\010':
                    out[q++] = '\\'; out[q++] = 'b'; break;
                case '\007':
                    out[q++] = '\\'; out[q++] = 'a'; break;
                case '\033':
                    out[q++] = '\\'; out[q++] = 'e'; break;
                default:
                    if (ASCIIEncoding.INSTANCE.isPrint(c)) {
                        out[q++] = (byte)c;
                    } else {
                        out[q++] = '\\';
                        outBytes.setRealSize(q);
                        if (enc.isUTF8()) {
                            int n = preciseLength(enc, bytes, p - 1, end) - 1;
                            if (MBCLEN_CHARFOUND_LEN(n) > 0) {
                                int cc = codePoint(runtime, enc, bytes, p - 1, end);
                                outBytes.setRealSize(q);
                                p += n;
                                if (cc <= 0xFFFF) {
                                    Sprintf.sprintf(runtime, outBytes, "u%04X", cc);
                                } else {
                                    Sprintf.sprintf(runtime, outBytes, "u{%X}", cc);
                                }
                                q = outBytes.getRealSize();
                                continue;
                            }
                        }
                        Sprintf.sprintf(runtime, outBytes, "x%02X", c);
                        q = outBytes.getRealSize();
                    }
            }
        }
        if (!quoteOnlyIfNeeded || includingsNonprintable) out[q++] = '"';
        outBytes.setRealSize(q);
        assert out == outBytes.getUnsafeBytes(); // must not reallocate

        return outBytes;
    }

    public static boolean isEVStr(byte[] bytes, int p, int end) {
        return p < end ? isEVStr(bytes[p] & 0xff) : false;
    }

    public static boolean isEVStr(int c) {
        return c == '$' || c == '@' || c == '{';
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

    @Deprecated(since = "10.0")
    public static int strCount(ByteList str, Ruby runtime, boolean[] table, TrTables tables, Encoding enc) {
        return strCount(runtime.getCurrentContext(), str, table, tables, enc);
    }

    public static int strCount(ThreadContext context, ByteList str, boolean[] table, TrTables tables, Encoding enc) {
        try {
            return strCount(str, table, tables, enc);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, e.getMessage());
        }
    }

    // MRI: rb_str_rindex
    public static int rindex(ByteList source, int sourceChars, int subChars, int pos, CodeRangeable subStringCodeRangeable, Encoding enc) {
        if (subStringCodeRangeable.scanForCodeRange() == CR_BROKEN) return -1;

        final ByteList subString = subStringCodeRangeable.getByteList();

        final int srcLen = source.getRealSize();
        final int subLen = subString.getRealSize();

        if (sourceChars < subChars || srcLen < subLen) return -1;
        if (sourceChars - pos < subChars) pos = sourceChars - subChars;
        if (sourceChars == 0) return pos;

        byte[] srcBytes = source.getUnsafeBytes();
        final int srcBeg = source.getBegin();

        if (pos == 0) {
            if (ByteList.memcmp(srcBytes, srcBeg, subString.getUnsafeBytes(), subString.getBegin(), subLen) == 0) {
                return 0;
            }
            return -1;
        }

        int s = nth(enc, srcBytes, srcBeg, srcBeg + srcLen, pos);

        return strRindex(srcBytes, srcBeg, srcLen, subString.getUnsafeBytes(), subString.getBegin(), subLen, s, pos, enc);
    }

    public static int byterindex(ByteList source, int pos, CodeRangeable subStringCodeRangeable, Encoding enc) {
        if (subStringCodeRangeable.scanForCodeRange() == CR_BROKEN) return -1;

        final ByteList subString = subStringCodeRangeable.getByteList();
        final int srcLen = source.getRealSize();
        final int subLen = subString.getRealSize();

        if (srcLen < subLen || srcLen < subLen) return -1;
        if (srcLen == 0) return pos;

        byte[] srcBytes = source.getUnsafeBytes();
        final int srcBeg = source.getBegin();

        if (pos == 0) {
            if (ByteList.memcmp(srcBytes, srcBeg, subString.getUnsafeBytes(), subString.getBegin(), subLen) == 0) {
                return 0;
            }
            return -1;
        }

        return byteRindex(srcBytes, srcBeg, srcLen, subString.getUnsafeBytes(), subString.getBegin(), subLen, pos, enc);
    }

    private static int byteRindex(final byte[] strBytes, final int strBeg, final int strLen,
                                 final byte[] subBytes, final int subBeg, final int subLen,
                                 int pos, final Encoding enc) {

        int s = pos;
        final int e = strBeg + strLen;

        while (s >= strBeg) {
            if (s + subLen <= e && ByteList.memcmp(strBytes, s, subBytes, subBeg, subLen) == 0) {
                return pos;
            }
            if (pos == 0) break;
            int t = enc.prevCharHead(strBytes, strBeg, s, e);
            pos -= s - t;
            s = t;
        }

        return -1;
    }

    private static int strRindex(final byte[] strBytes, final int strBeg, final int strLen,
                                 final byte[] subBytes, final int subBeg, final int subLen,
                                 int s, int pos, final Encoding enc) {

        final int e = strBeg + strLen;

        while (s >= strBeg) {
            if (s + subLen <= e && ByteList.memcmp(strBytes, s, subBytes, subBeg, subLen) == 0) {
                return pos;
            }
            if (pos == 0) break;
            pos--;
            s = enc.prevCharHead(strBytes, strBeg, s, e);
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

        if (isSingleByteOptimizable(string, bytes.getEncoding())) return bytes.getRealSize();
        return strLengthFromRubyStringFull(string, bytes, bytes.getEncoding());
    }

    public static int strLengthFromRubyString(CodeRangeable string, final ByteList bytes, final Encoding enc) {
        if (isSingleByteOptimizable(string, enc)) return bytes.getRealSize();
        // NOTE: strLengthFromRubyStringFull but without string.setCodeRange(..)
        if (string.isCodeRangeValid() && enc.isUTF8()) return utf8Length(bytes);

        long lencr = strLengthWithCodeRange(bytes, enc);
        return unpackResult(lencr);
    }

    private static int strLengthFromRubyStringFull(CodeRangeable string, ByteList bytes, Encoding enc) {
        if (string.isCodeRangeValid() && enc.isUTF8()) return utf8Length(bytes);

        long lencr = strLengthWithCodeRange(bytes, enc);
        int cr = unpackArg(lencr);
        if (cr != 0) string.setCodeRange(cr);
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

        final byte[] buf;
        int p;
        final int pend;
        int now;
        int max;
        boolean gen;
    }

    /**
     * tr_setup_table
     */
    public static final class TrTables {
        IntHashMap<Object> del, noDel; // used as ~ Set
    }

    private static final Object DUMMY_VALUE = "";

    public static TrTables trSetupTable(final ByteList str,
                                        final boolean[] stable, TrTables tables, final boolean first, final Encoding enc) {
        int i, l[] = {0};
        final boolean cflag;

        final TR tr = new TR(str);

        if (str.realSize() > 1 && EncodingUtils.encAscget(tr.buf, tr.p, tr.pend, l, enc) == '^') {
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

    @Deprecated(since = "10.0")
    public static TrTables trSetupTable(final ByteList str, final Ruby runtime,
                                        final boolean[] stable, TrTables tables, final boolean first, final Encoding enc) {
        return trSetupTable(runtime.getCurrentContext(), str, stable, tables, first, enc);
    }

    public static TrTables trSetupTable(ThreadContext context, final ByteList str,
        final boolean[] stable, TrTables tables, final boolean first, final Encoding enc) {

        try {
            return trSetupTable(str, stable, tables, first, enc);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, e.getMessage());
        }
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

    public static int trNext(final TR tr, Ruby runtime, Encoding enc) {
        try {
            return trNext(tr, enc);
        } catch (IllegalArgumentException e) {
            throw argumentError(runtime.getCurrentContext(), e.getMessage());
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

    public static enum NeighborChar {NOT_CHAR, FOUND, WRAPPED}

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
                byte tmp[] = new byte[org.jcodings.Config.ENC_CODE_TO_MBC_MAXLEN];
                int cl = preciseLength(enc, bytes, s, end);
                if (cl <= 0) continue;
                System.arraycopy(bytes, s, tmp, 0, cl);
                neighbor = succChar(enc, tmp, 0, cl);
                if (neighbor == NeighborChar.FOUND) {
                    System.arraycopy(tmp, 0, bytes, s, cl);
                    return valueCopy;
                }
                if (neighbor == NeighborChar.WRAPPED) {
                    System.arraycopy(tmp, 0, bytes, s, cl);
                }
                if (preciseLength(enc, bytes, s, s + cl) != cl) succChar(enc, bytes, s, cl); /* wrapped to \0...\0.  search next valid char. */
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

    @Deprecated(since = "10.0")
    public static ByteList succCommon(Ruby runtime, ByteList original) {
        return succCommon(runtime.getCurrentContext(), original);
    }

    public static ByteList succCommon(ThreadContext context, ByteList original) {
        try {
            return succCommon(original);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, e.getMessage());
        }
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

    public static NeighborChar succChar(Ruby runtime, Encoding enc, byte[] bytes, int p, int len) {
        try {
            return succChar(enc, bytes, p, len);
        } catch (IllegalArgumentException e) {
            throw argumentError(runtime.getCurrentContext(), e.getMessage());
        }
    }

    // MRI: enc_succ_alnum_char
    private static NeighborChar succAlnumChar(Encoding enc, byte[]bytes, int p, int len, byte[]carry, int carryP) {
        NeighborChar ret;
        byte save[] = new byte[org.jcodings.Config.ENC_CODE_TO_MBC_MAXLEN];

        /* skip 03A2, invalid char between GREEK CAPITAL LETTERS */
        int tryCounter;
        final int maxGaps = 1;

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
        for (tryCounter = 0; tryCounter <= maxGaps; ++tryCounter) {
            ret = succChar(enc, bytes, p, len);
            if (ret == NeighborChar.FOUND) {
                c = enc.mbcToCode(bytes, p, p + len);
                if (enc.isCodeCType(c, cType)) return NeighborChar.FOUND;
            }
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

    /**
     *
     * @param source string to find index within
     * @param other string to match in source
     * @param offset in bytes to start looking
     * @param enc encoding to use to walk the source string.
     * @return
     */
    public static int index(ByteList source, ByteList other, int offset, Encoding enc) {
        int sourceLen = source.realSize();
        int sourceBegin = source.begin();
        int otherLen = other.realSize();

        if (otherLen == 0) return offset;
        if (sourceLen - offset < otherLen) return -1;

        byte[] sourceBytes = source.getUnsafeBytes();
        int p = sourceBegin + offset;
        int end = p + sourceLen;

        while (true) {
            int pos = source.indexOf(other, p - sourceBegin);
            if (pos < 0) return pos;
            pos -= (p - sourceBegin);
            int t = enc.rightAdjustCharHead(sourceBytes, p, p + pos, end);
            if (t == p + pos) return pos + offset;
            if ((sourceLen -= t - p) <= 0) return -1;
            offset += t - p;
            p = t;
        }
    }

    public static int byteindex(CodeRangeable sourceString, CodeRangeable otherString, int offset, Encoding enc) {
        if (otherString.scanForCodeRange() == CR_BROKEN) return -1;

        final ByteList source = sourceString.getByteList();
        final ByteList other = otherString.getByteList();

        int sourceLen = source.realSize();
        int otherLen = other.realSize();

        if (offset < 0) {
            offset += sourceLen;
            if (offset < 0) return -1;
        }

        if (sourceLen - offset < otherLen) return -1;
        byte[] bytes = source.getUnsafeBytes();
        int p = source.getBegin();
        int end = p + source.getRealSize();
        if (offset != 0) {
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

        byte[] oldBytes = source.getByteList().getUnsafeBytes();
        int oldBegin = source.getByteList().getBegin();

        source.modify(newLength);
        if (replLength != len) {
            System.arraycopy(oldBytes, oldBegin + beg + len, source.getByteList().getUnsafeBytes(), beg + replLength, oldLength - (beg + len));
        }

        if (replLength > 0) System.arraycopy(replBytes.getUnsafeBytes(), replBytes.getBegin(), source.getByteList().getUnsafeBytes(), beg, replLength);
        source.getByteList().setRealSize(newLength);

        return source.getByteList();
    }

    @Deprecated(since = "9.4")
    public static void replaceInternal19(int beg, int len, CodeRangeable source, CodeRangeable repl) {
        strUpdate(beg, len, source, repl);
    }

    @Deprecated(since = "9.4")
    public static void replaceInternal19(Ruby runtime, int beg, int len, RubyString source, RubyString repl) {
        strUpdate(runtime.getCurrentContext(), beg, len, source, repl);
    }

    // MRI: rb_str_update, second half
    public static void strUpdate(int beg, int len, CodeRangeable source, CodeRangeable repl) {
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
        if (cr != source.getCodeRange()) source.setCodeRange(cr);
    }

    @Deprecated(since = "10.0")
    public static void strUpdate(Ruby runtime, int beg, int len, RubyString source, RubyString repl) {
        strUpdate(runtime.getCurrentContext(), beg, len, source, repl);
    }

    // MRI: rb_str_update, first half
    public static void strUpdate(ThreadContext context, int beg, int len, RubyString source, RubyString repl) {
        if (len < 0) throw indexError(context, "negative length " + len);

        int slen = strLengthFromRubyString(source, source.checkEncoding(repl));

        if (slen < beg) throw indexError(context, "index " + beg + " out of string");
        if (beg < 0) {
            if (-beg > slen) throw indexError(context, "index " + beg + " out of string");
            beg += slen;
        }
        if (slen < len || slen < beg + len) len = slen - beg;

        strUpdate(beg, len, source, repl);
    }

    public static boolean isAsciiOnly(CodeRangeable string) {
        return isAsciiOnly(string.getByteList().getEncoding(), string.scanForCodeRange());
    }

    private static boolean isAsciiOnly(Encoding encoding, final int codeRange) {
        return encoding.isAsciiCompatible() && codeRange == CR_7BIT;
    }

    /**
     * rb_str_delete_bang
     */
    static CodeRangeable strDeleteBang(CodeRangeable rubyString, boolean[] squeeze, TrTables tables, Encoding enc) {
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

    @Deprecated(since = "10.0")
    public static CodeRangeable strDeleteBang(CodeRangeable rubyString, Ruby runtime, boolean[] squeeze, TrTables tables, Encoding enc) {
        return strDeleteBang(runtime.getCurrentContext(), rubyString, squeeze, tables, enc);
    }

    public static CodeRangeable strDeleteBang(ThreadContext context, CodeRangeable rubyString, boolean[] squeeze, TrTables tables, Encoding enc) {
        try {
            return strDeleteBang(rubyString, squeeze, tables, enc);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, e.getMessage());
        }
    }

    /**
     * MRI: chopped_length
     */
    public static int choppedLength(CodeRangeable str) {
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
    public static Encoding areCompatible(CodeRangeable str1, CodeRangeable str2) {
        Encoding enc1 = str1.getByteList().getEncoding();
        Encoding enc2 = str2.getByteList().getEncoding();

        if (enc1 == enc2) return enc1;

        str1.scanForCodeRange();
        str2.scanForCodeRange();
        return encCompatibleLatter(str1, str2, enc1, enc2);
    }

    public static Encoding areCompatible(Encoding enc1, CodeRangeable str2) {
        Encoding enc2 = str2.getByteList().getEncoding();

        if (enc1 == enc2) return enc1;

        str2.scanForCodeRange();
        return encCompatibleLatter(null, str2, enc1, enc2);
    }
    
    private static Encoding encCompatibleLatter(CodeRangeable str1, CodeRangeable str2, Encoding enc1, Encoding enc2) {
        boolean isstr1, isstr2;

        isstr2 = str2 instanceof RubyString;
        if (isstr2 && ((RubyString) str2).size() == 0) {
            return enc1;
        }
        isstr1 = str1 instanceof RubyString;
        if (isstr1 && isstr2 && ((RubyString) str1).size() == 0) {
            return (enc1.isAsciiCompatible() && ((RubyString) str2).isAsciiOnly()) ? enc1 : enc2;
        }
        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) {
            return null;
        }

        /* objects whose encoding is the same of contents */
        if (!isstr2 && enc2 == USASCIIEncoding.INSTANCE) {
            return enc1;
        }
        if (!isstr1 && enc1 == USASCIIEncoding.INSTANCE) {
            return enc2;
        }

        if (!isstr1) {
            CodeRangeable tmp = str1;
            str1 = str2;
            str2 = tmp;
            Encoding enc0 = enc1;
            enc1 = enc2;
            enc2 = enc0;
            boolean tmp2 = isstr1;
            isstr1 = isstr2;
            isstr2 = tmp2;
        }
        if (isstr1) {
            int cr1, cr2;

            cr1 = str1.getCodeRange();
            if (isstr2) {
                cr2 = str2.getCodeRange();
                if (cr1 != cr2) {
                    /* may need to handle ENC_CODERANGE_BROKEN */
                    if (cr1 == CR_7BIT) return enc2;
                    if (cr2 == CR_7BIT) return enc1;
                }
                if (cr2 == CR_7BIT) {
                    return enc1;
                }
            }
            if (cr1 == CR_7BIT) {
                return enc2;
            }
        }
        return null;
    }

    public static Encoding areCompatible(ByteList str1, ByteList str2) {
        Encoding enc1 = str1.getEncoding();
        Encoding enc2 = str2.getEncoding();

        if (enc1 == enc2) return enc1;

        if (str2.getRealSize() == 0) return enc1;
        if (str1.getRealSize() == 0) {
            return (enc1.isAsciiCompatible() && isAsciiOnly(enc2, scanForCodeRange(str2))) ? enc1 : enc2;
        }

        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;

        return RubyEncoding.areCompatible(enc1, scanForCodeRange(str1), enc2, scanForCodeRange(str2));
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
        IRubyObject opts = ArgsUtil.getOptionsArg(context, arg);
        return opts == context.nil ?
                rbStrEnumerateLines(str, context, name, arg, context.nil, block, wantarray) :
                rbStrEnumerateLines(str, context, name, globalVariables(context).get("$/"), opts, block, wantarray);
    }

    private static final int NULL_POINTER = -1;

    public static IRubyObject rbStrEnumerateLines(RubyString str, ThreadContext context, String name, IRubyObject arg, IRubyObject opts, Block block, boolean wantarray) {
        Encoding enc;
        IRubyObject line, orig = str;
        int ptr, pend, subptr, subend, hit, adjusted;

        boolean rsnewline = false;
        boolean chomp = false;

        if (opts != context.nil) {
            IRubyObject _chomp = ArgsUtil.extractKeywordArg(context, "chomp", opts);
            chomp = _chomp != null || _chomp.isTrue();
        }

        if (block.isGiven()) {
            if (wantarray) {
                // this code should be live in 3.0
                if (false) { // #if STRING_ENUMERATORS_WANTARRAY
                    warn(context, "given block not used");
                } else {
                    wantarray = false;
                }
            }
        }
        else if (!wantarray) {
            return enumeratorize(context.runtime, str, name, Helpers.arrayOf(arg, opts));
        }

        if (arg == context.nil) { // rs
            if (wantarray) return newArray(context, str);

            block.yieldSpecific(context, str);
            return orig;
        }

        final var ary = wantarray ? newArray(context) : null;

        final IRubyObject defaultSep = globalVariables(context).get("$/");
        RubyString rs = arg.convertToString();

        str = str.newFrozen();
        byte[] strBytes = str.getByteList().unsafeBytes();
        ptr = subptr = str.getByteList().begin();
        final int len = str.size();
        pend = ptr + len;
        int rslen = rs.size();

        enc = (rs == defaultSep) ? str.getEncoding() : str.checkEncoding(rs);

        if (rslen == 0) {
            rbStrEnumerateLinesEmptySep(str, context, chomp, block, ary, strBytes, ptr, len, pend, enc, subptr);
            return wantarray ? ary : orig; // end
        }

        ByteList rsByteList = rs.getByteList();
        byte[] rsbytes = rsByteList.unsafeBytes();
        int rsptr = rsByteList.begin();
        if (rsByteList.length() == enc.minLength() && enc.isNewLine(rsbytes, rsptr, rsByteList.length())) {
            rsnewline = true;
        }

        if (rs == defaultSep && !enc.isAsciiCompatible()) {
            rs = newString(context, rsbytes, rsptr, rslen);
            rs = (RubyString) EncodingUtils.rbStrEncode(context, rs, encodingService(context).convertEncodingToRubyEncoding(enc), 0, context.nil);
            rsByteList = rs.getByteList();
            rsbytes = rsByteList.unsafeBytes();
            rsptr = rsByteList.begin();
            rslen = rsByteList.realSize();
        }

        while (subptr < pend) {
            int pos = memsearch(rsbytes, rsptr, rslen, strBytes, subptr, pend - subptr, enc);
            if (pos < 0) break;
            hit = subptr + pos;
            adjusted = enc.rightAdjustCharHead(strBytes, subptr, hit, pend);
            if (hit != adjusted) {
                subptr = adjusted;
                continue;
            }
            subend = hit += rslen;
            if (chomp) {
                if (rsnewline) {
                    subend = chomp_newline(strBytes, subptr, subend, enc);
                } else {
                    subend -= rslen;
                }
            }
            line = str.substr(context, subptr - ptr, subend - subptr);
            if (wantarray) ary.append(context, line);
            else {
                block.yieldSpecific(context, line);
                str.modifyCheck(strBytes, len);
            }
            subptr = hit;
        }

        if (subptr != pend) {
            if (chomp) {
                if (rsnewline) {
                    pend = chomp_newline(strBytes, subptr, pend, enc);
                } else if (pend - subptr >= rslen &&
                        ByteList.memcmp(strBytes, pend - rslen, rsbytes, rsptr, rslen) == 0) {
                    pend -= rslen;
                }
            }
            line = str.substr(context, subptr - ptr, pend - subptr);
            if (wantarray) ary.append(context, line);
            else block.yieldSpecific(context, line);
        }

        return wantarray ? ary : orig;
    }

    private static void rbStrEnumerateLinesEmptySep(RubyString str,
        ThreadContext context, final boolean chomp, Block block, RubyArray ary,
        final byte[] strBytes, final int ptr, final int len,
        final int pend, final Encoding enc, int subptr) {
        /* paragraph mode */
        int[] n = {0};
        int eol = NULL_POINTER;
        int subend = subptr;
        while (subend < pend) {
            int rslen;
            do {
                if (EncodingUtils.encAscget(strBytes, subend, pend, n, enc) != '\r') n[0] = 0;
                rslen = n[0] + length(enc, strBytes, subend + n[0], pend);
                if (enc.isNewLine(strBytes, subend + n[0], pend)) {
                    if (eol == subend) break;
                    subend += rslen;
                    if (subptr != NULL_POINTER) eol = subend;
                }
                else {
                    if (subptr == NULL_POINTER) subptr = subend;
                    subend += rslen;
                }
                rslen = 0;
            } while (subend < pend);
            if (subptr == NULL_POINTER) break;
            RubyString line = str.makeSharedString(context.runtime, subptr - ptr,
                    subend - subptr + (chomp ? 0 : rslen));
            if (ary != null) ary.append(context, line); // wantarray
            else {
                block.yield(context, line);
                str.modifyCheck(strBytes, len);
            }
            subptr = eol = NULL_POINTER;
        }
    }

    private static int chomp_newline(byte[] bytes, int p, int e, Encoding enc) {
        int prev = enc.prevCharHead(bytes, p, e, e);
        if (enc.isNewLine(bytes, prev, e)) {
            e = prev;
            prev = enc.prevCharHead(bytes, p, e, e);
            if (prev != -1 && EncodingUtils.encAscget(bytes, prev, e, null, enc) == '\r') e = prev;
        }
        return e;
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
            int ys = Helpers.memchr(yBytes, y, xBytes[x], n);

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

    public static CodeRangeable trTransHelper(CodeRangeable self, CodeRangeable srcStr, CodeRangeable replStr, boolean sflag) {
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
            for (int i=0; i< StringSupport.TRANS_SIZE; i++) {
                trans[i] = 1;
            }

            while ((c = StringSupport.trNext(trSrc, enc)) != -1) {
                if (c < StringSupport.TRANS_SIZE) {
                    trans[c] = -1;
                } else {
                    if (hash == null) hash = new IntHash<Integer>();
                    hash.put(c, 1); // QTRUE
                }
            }
            while ((c = StringSupport.trNext(trRepl, enc)) != -1) {}  /* retrieve last replacer */
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

            while ((c = StringSupport.trNext(trSrc, enc)) != -1) {
                int r = StringSupport.trNext(trRepl, enc);
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

        if (cr == CR_VALID && enc.isAsciiCompatible()) {
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
            self.getByteList().setBegin(0);
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
            self.getByteList().setBegin(0);
            self.getByteList().setRealSize(t);
        }

        if (modify) {
            if (cr != CR_BROKEN) self.setCodeRange(cr);
            StringSupport.associateEncoding(self, enc);
            return self;
        }
        return null;
    }

    @Deprecated(since = "10.0")
    public static CodeRangeable trTransHelper(Ruby runtime, CodeRangeable self, CodeRangeable srcStr, CodeRangeable replStr, boolean sflag) {
        try {
            return trTransHelper(self, srcStr, replStr, sflag);
        } catch (IllegalArgumentException e) {
            throw argumentError(runtime.getCurrentContext(), e.getMessage());
        }
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

            final int cl, ocl;
            if (Encoding.isAscii(c) && Encoding.isAscii(oc)) {
                int dc = AsciiTables.ToLowerCaseTable[c];
                int odc = AsciiTables.ToLowerCaseTable[oc];
                if (dc != odc) return dc < odc ? -1 : 1;

                if (enc.isAsciiCompatible()) {
                    cl = ocl = 1;
                } else {
                    cl = preciseLength(enc, bytes, p, end);
                    ocl = preciseLength(enc, obytes, op, oend);
                }
            } else {
                cl = length(enc, bytes, p, end);
                ocl = length(enc, obytes, op, oend);
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

    @Deprecated(since = "10.0")
    public static boolean multiByteSqueeze(Ruby runtime, ByteList value, boolean squeeze[], TrTables tables, Encoding enc, boolean isArg) {
        return multiByteSqueeze(runtime.getCurrentContext(), value, squeeze, tables, enc, isArg);
    }

    public static boolean multiByteSqueeze(ThreadContext context, ByteList value, boolean squeeze[], TrTables tables, Encoding enc, boolean isArg) {
        try {
            return multiByteSqueeze(value, squeeze, tables, enc, isArg);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, e.getMessage());
        }
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

    private static int rb_memsearch_qs_utf8_hash(byte[] xBytes, final int x) {
        final int mix = 8353;
        int h;
        if (x != xBytes.length) {
            h = xBytes[x] & 0xFF;
        }
        else {
            h = '\0'; // (C) ary end - due y+m at rb_memsearch_qs_utf8
        }
        if (h < 0xC0) {
            return h + 256;
        }
        else if (h < 0xE0) {
            h *= mix;
            h += xBytes[x + 1] & 0xff;
        }
        else if (h < 0xF0) {
            h *= mix;
            h += xBytes[x + 1] & 0xff;
            h *= mix;
            h += xBytes[x + 2] & 0xff;

        }
        else if (h < 0xF5) {
            h *= mix;
            h += xBytes[x + 1] & 0xff;
            h *= mix;
            h += xBytes[x + 2] & 0xff;
            h *= mix;
            h += xBytes[x + 3] & 0xff;
        }
        else {
            return h + 256;
        }
        return ((byte)h) & 0xff;
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
        /* Searching */ // due y+m <= ... (y+m) might == ary.length
        for (; y + m <= ys + n; y += qstable[rb_memsearch_qs_utf8_hash(ysBytes, y+m)]) {
            if (xsBytes[xs] == ysBytes[y] && ByteList.memcmp(xsBytes, xs, ysBytes, y, m) == 0)
                return y - ys;
        }
        return -1;
    }

    @Deprecated(since = "10.0")
    public static int checkCaseMapOptions(Ruby runtime, IRubyObject arg0, IRubyObject arg1, int flags) {
        return checkCaseMapOptions(runtime.getCurrentContext(), arg0, arg1, flags);
    }

    public static int checkCaseMapOptions(ThreadContext context, IRubyObject arg0, IRubyObject arg1, int flags) {
        RubySymbol turkic = asSymbol(context, "turkic");
        RubySymbol lithuanian = asSymbol(context, "lithuanian");

        if (arg0.equals(turkic)) {
            flags |= Config.CASE_FOLD_TURKISH_AZERI;
            if (arg1.equals(lithuanian)) {
                flags |= Config.CASE_FOLD_LITHUANIAN;
            } else {
                throw argumentError(context, "invalid second option");
            }
        } else if (arg0.equals(lithuanian)) {
            flags |= Config.CASE_FOLD_LITHUANIAN;
            if (arg1.equals(turkic)) {
                flags |= Config.CASE_FOLD_TURKISH_AZERI;
            } else {
                throw argumentError(context, "invalid second option");
            }
        } else {
            throw argumentError(context, "invalid option");
        }
        return flags;
    }

    @Deprecated(since = "10.0")
    public static int checkCaseMapOptions(Ruby runtime, IRubyObject arg0, int flags) {
        return checkCaseMapOptions(runtime.getCurrentContext(), arg0, arg0, flags);
    }

    public static int checkCaseMapOptions(ThreadContext context, IRubyObject arg0, int flags) {
        if (arg0.equals(asSymbol(context, "ascii"))) {
            flags |= Config.CASE_ASCII_ONLY;
        } else if (arg0.equals(asSymbol(context, "fold"))) {
            if ((flags & (Config.CASE_UPCASE | Config.CASE_DOWNCASE)) == Config.CASE_DOWNCASE) {
                flags ^= Config.CASE_FOLD | Config.CASE_DOWNCASE;
            } else {
                throw argumentError(context, "option :fold only allowed for downcasing");
            }
        } else if (arg0.equals(asSymbol(context, "turkic"))) {
            flags |= Config.CASE_FOLD_TURKISH_AZERI;
        } else if (arg0.equals(asSymbol(context, "lithuanian"))) {
            flags |= Config.CASE_FOLD_LITHUANIAN;
        } else {
            throw argumentError(context, "invalid option");
        }
        return flags;
    }

    private static final class MappingBuffer {
        MappingBuffer next;
        final byte[] bytes;
        int used;

        MappingBuffer() {
            bytes = null;
        }

        MappingBuffer(int size) {
            bytes = new byte[size];
        }
    }

    private static final int CASE_MAPPING_ADDITIONAL_LENGTH = 20;

    @Deprecated(since = "10.0")
    public static ByteList caseMap(Ruby runtime, ByteList src, IntHolder flags, Encoding enc) {
        return caseMap(runtime.getCurrentContext(), src, flags, enc);
    }

    public static ByteList caseMap(ThreadContext context, ByteList src, IntHolder flags, Encoding enc) {
        IntHolder pp = new IntHolder();
        pp.value = src.getBegin();
        int end = src.getRealSize() + pp.value;
        byte[]bytes = src.getUnsafeBytes();
        int tgtLen = 0;

        int buffers = 0;
        MappingBuffer root = new MappingBuffer();
        MappingBuffer buffer = root;
        while (pp.value < end) {
            buffer.next = new MappingBuffer((end - pp.value) * ++buffers + CASE_MAPPING_ADDITIONAL_LENGTH);
            buffer = buffer.next;
            int len = enc.caseMap(flags, bytes, pp, end, buffer.bytes, 0, buffer.bytes.length);
            if (len < 0) throw argumentError(context, "input string invalid");
            buffer.used = len;
            tgtLen += len;
        }

        final ByteList tgt;
        if (buffers == 1) {
            tgt = new ByteList(buffer.bytes, 0, buffer.used, enc, false);
        } else {
            tgt = new ByteList(tgtLen);
            tgt.setEncoding(enc);
            buffer = root.next;
            int tgtPos = 0;
            while (buffer != null) {
                System.arraycopy(buffer.bytes, 0, tgt.getUnsafeBytes(), tgtPos, buffer.used);
                tgtPos += buffer.used;
                buffer = buffer.next;
            }
        }

        return tgt;
    }

    @Deprecated(since = "10.0")
    public static void asciiOnlyCaseMap(Ruby runtime, ByteList value, IntHolder flags, Encoding enc) {
        asciiOnlyCaseMap(runtime.getCurrentContext(), value, flags);
    }

    public static void asciiOnlyCaseMap(ThreadContext context, ByteList value, IntHolder flags) {
        if (value.getRealSize() == 0) return;
        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();

        IntHolder pp = new IntHolder();
        pp.value = s;
        int len = ASCIIEncoding.INSTANCE.caseMap(flags, bytes, pp, end, bytes, s, end);
        if (len < 0) throw argumentError(context, "input string invalid");
    }

    public static int encCoderangeClean(int cr) {
        return (cr ^ (cr >> 1)) & CR_7BIT;
    }

    public static String byteListAsString(ByteList bytes) {
        try {
            Charset charset = bytes.getEncoding().getCharset();
            if (charset != null) return new String(bytes.unsafeBytes(), bytes.begin(), bytes.realSize(), charset);
        } catch (UnsupportedCharsetException e) {}

        return new String(bytes.unsafeBytes(), bytes.begin(), bytes.realSize());
    }

    @Deprecated
    public static boolean isUnicode(Encoding enc) {
        return enc.isUnicode();
    }
}
