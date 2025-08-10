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

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;

import static org.jruby.api.Error.argumentError;

public class RegexpSupport {

    public enum ErrorMode {RAISE, PREPROCESS, DESC}

    /**
     * Preprocess the given string for use in regexp, raising errors for encoding
     * incompatibilities that arise.
     *
     * This version produces a new unescaped version of the string based on
     * fixes performed while walking.
     *
     * @param runtime current runtime
     * @param str string to preprocess
     * @param enc string's encoding
     * @param fixedEnc new encoding after fixing
     * @param mode mode of errors
     * @return a new unescaped string
     */
    public static ByteList preprocess(Ruby runtime, ByteList str, Encoding enc, Encoding[] fixedEnc, ErrorMode mode) {
        ByteList to = new ByteList(str.getRealSize());

        if (enc.isAsciiCompatible()) {
            fixedEnc[0] = null;
        } else {
            fixedEnc[0] = enc;
            to.setEncoding(enc);
        }

        boolean hasProperty = unescapeNonAscii(runtime, to, str.getUnsafeBytes(), str.getBegin(), str.getBegin() + str.getRealSize(), enc, fixedEnc, str, mode);
        if (hasProperty && fixedEnc[0] == null) fixedEnc[0] = enc;
        if (fixedEnc[0] != null) to.setEncoding(fixedEnc[0]);
        return to;
    }

    /**
     * Unescape non-ascii elements in the given string, appending the results
     * to the given bytelist if provided. (mri: unescape_nonascii).
     *
     * @param runtime current runtime
     * @param to output bytelist; if null, no appending will be done
     * @param bytes the bytes to unescape
     * @param p starting position
     * @param end ending position
     * @param enc bytes' encoding
     * @param encp out param for fixed encoding
     * @param str original wrapper for the bytes
     * @param mode error mode
     * @return whether any propery elements were encountered while walking
     */
    public static boolean unescapeNonAscii(Ruby runtime, ByteList to, byte[] bytes, int p, int end, Encoding enc, Encoding[] encp, ByteList str, ErrorMode mode) {
        boolean hasProperty = false;
        byte[] buf = null;

        while (p < end) {
            int cl = StringSupport.preciseLength(enc, bytes, p, end);
            if (cl <= 0) raisePreprocessError(runtime, str, "invalid multibyte character", mode);
            if (cl > 1 || (bytes[p] & 0x80) != 0) {
                p = appendMBC(runtime, to, bytes, p, enc, encp, str, mode, cl);
                continue;
            }
            int c;
            switch (c = bytes[p++] & 0xff) {
                case '\\':
                    if (p == end) raisePreprocessError(runtime, str, "too short escape sequence", mode);

                    cl = StringSupport.preciseLength(enc, bytes, p, end);
                    if (cl <= 0) raisePreprocessError(runtime, str, "invalid multibyte character", mode);
                    if (cl > 1) {
                        p = appendMBC(runtime, to, bytes, p, enc, encp, str, mode, cl);
                        break;
                    }

                    switch (c = bytes[p++] & 0xff) {
                        case '1': case '2': case '3':
                        case '4': case '5': case '6': case '7': /* \O, \OO, \OOO or backref */
                            if (StringSupport.scanOct(bytes, p - 1, end - (p - 1)) <= 0177) {
                                if (to != null) to.append('\\').append(c);
                                break;
                            }

                        case '0': /* \0, \0O, \0OO */
                        case 'x': /* \xHH */
                        case 'c': /* \cX, \c\M-X */
                        case 'C': /* \C-X, \C-\M-X */
                        case 'M': /* \M-X, \M-\C-X, \M-\cX */
                            p -= 2;
                            if (enc == USASCIIEncoding.INSTANCE) {
                                if (buf == null) buf = new byte[1];
                                int pbeg = p;
                                p = readEscapedByte(runtime, buf, 0, bytes, p, end, str, mode);
                                c = buf[0];
                                if (c == (char)-1) return false;
                                if (to != null) {
                                    to.append(bytes, pbeg, p - pbeg);
                                }
                            }
                            else {
                                p = unescapeEscapedNonAscii(runtime, to, bytes, p, end, enc, encp, str, mode);
                            }
                            break;

                        case 'u':
                            if (p == end) raisePreprocessError(runtime, str, "too short escape sequence", mode);
                            if (bytes[p] == (byte)'{') { /* \\u{H HH HHH HHHH HHHHH HHHHHH ...} */
                                p++;
                                p = unescapeUnicodeList(runtime, to, bytes, p, end, encp, str, mode);
                                if (p == end || bytes[p++] != (byte)'}') raisePreprocessError(runtime, str, "invalid Unicode list", mode);
                            } else { /* \\uHHHH */
                                p = unescapeUnicodeBmp(runtime, to, bytes, p, end, encp, str, mode);
                            }
                            break;
                        case 'p': /* \p{Hiragana} */
                            if (encp[0] == null) hasProperty = true;
                            if (to != null) to.append('\\').append(c);
                            break;

                        default:
                            if (to != null) to.append('\\').append(c);
                            break;
                    } // inner switch
                    break;

                default:
                    if (to != null) to.append(c);
            } // switch
        } // while
        return hasProperty;
    }

    private static int appendMBC(Ruby runtime, ByteList to, byte[] bytes, int p, Encoding enc, Encoding[] encp, ByteList str, ErrorMode mode, int cl) {
        if (to != null) to.append(bytes, p, cl);
        p += cl;
        if (encp[0] == null) {
            encp[0] = enc;
        } else if (encp[0] != enc) {
            raisePreprocessError(runtime, str, "non ASCII character in UTF-8 regexp", mode);
        }
        return p;
    }

    public static int raisePreprocessError(Ruby runtime, ByteList str, String err, ErrorMode mode) {
        switch (mode) {
            case RAISE:
                raiseRegexpError(runtime, str, str.getEncoding(), RegexpOptions.NULL_OPTIONS, err);
            case PREPROCESS:
                throw argumentError(runtime.getCurrentContext(), "regexp preprocess failed: " + err);
            case DESC:
                // silent ?
        }
        return 0;
    }

    @Deprecated
    public static void raiseRegexpError19(Ruby runtime, ByteList bytes, Encoding enc, RegexpOptions options, String err) {
        raiseRegexpError(runtime, bytes, enc, options, err);
    }

    // rb_enc_reg_raise
    public static void raiseRegexpError(Ruby runtime, ByteList bytes, Encoding enc, RegexpOptions options, String err) {
        // TODO: we loose encoding information here, fix it
        throw runtime.newRegexpError(err + ": " + regexpDescription(runtime, bytes, options, enc));
    }

    @Deprecated
    public static ByteList regexpDescription19(Ruby runtime, ByteList bytes, RegexpOptions options, Encoding enc) {
        return regexpDescription(runtime, bytes, options, enc);
    }

    // rb_enc_reg_error_desc
    public static ByteList regexpDescription(Ruby runtime, ByteList bytes, RegexpOptions options, Encoding enc) {
        byte[] s = bytes.getUnsafeBytes();
        int start = bytes.getBegin();
        int len = bytes.getRealSize();
        return regexpDescription(runtime, s, start, len, options, enc);
    }

    private static ByteList regexpDescription(Ruby runtime, byte[] s, int start, int len, RegexpOptions options, Encoding enc) {
        ByteList description = new ByteList();
        description.setEncoding(enc);
        description.append((byte)'/');
        Encoding resultEnc = runtime.getDefaultInternalEncoding();
        if (resultEnc == null) resultEnc = runtime.getDefaultExternalEncoding();

        appendRegexpString(runtime, description, s, start, len, enc, resultEnc);
        description.append((byte)'/');
        appendOptions(description, options);
        if (options.isEncodingNone()) description.append((byte) 'n');
        return description;
    }

    @Deprecated
    public static void appendRegexpString19(Ruby runtime, ByteList to, byte[] bytes, int start, int len, Encoding enc, Encoding resEnc) {
        appendRegexpString(runtime, to, bytes, start, len, enc, resEnc);
    }

    public static void appendRegexpString(Ruby runtime, ByteList to, byte[] bytes, int start, int len, Encoding enc, Encoding resEnc) {
        int p = start;
        int end = p + len;
        boolean needEscape = false;
        while (p < end) {
            final int c;
            final int cl;
            if (enc.isAsciiCompatible()) {
                cl = 1;
                c = bytes[p] & 0xff;
            } else {
                cl = StringSupport.preciseLength(enc, bytes, p, end);
                c = enc.mbcToCode(bytes, p, end);
            }

            if (!Encoding.isAscii(c)) {
                p += StringSupport.length(enc, bytes, p, end);
            } else if (c != '/' && enc.isPrint(c)) {
                p += cl;
            } else {
                needEscape = true;
                break;
            }
        }
        if (!needEscape) {
            to.append(bytes, start, len);
        } else {
            boolean isUnicode = enc.isUnicode();
            p = start;
            while (p < end) {
                final int c;
                final int cl;
                if (enc.isAsciiCompatible()) {
                    cl = 1;
                    c = bytes[p] & 0xff;
                } else {
                    cl = StringSupport.preciseLength(enc, bytes, p, end);
                    c = enc.mbcToCode(bytes, p, end);
                }

                if (c == '\\' && p + cl < end) {
                    int n = cl + StringSupport.length(enc, bytes, p + cl, end);
                    to.append(bytes, p, n);
                    p += n;
                    continue;
                } else if (c == '/') {
                    to.append((byte) '\\');
                    to.append(bytes, p, cl);
                } else if (!Encoding.isAscii(c)) {
                    int l = StringSupport.preciseLength(enc, bytes, p, end);
                    if (l <= 0) {
                        l = 1;
                        Sprintf.sprintf(runtime, to, "\\x%02X", c);
                    } else if (resEnc != null) {
                        int code = enc.mbcToCode(bytes, p, end);
                        Sprintf.sprintf(runtime, to , StringSupport.escapedCharFormat(code, isUnicode), code);
                    } else {
                        to.append(bytes, p, l);
                    }
                    p += l;

                    continue;
                } else if (enc.isPrint(c)) {
                    to.append(bytes, p, cl);
                } else if (!enc.isSpace(c)) {
                    Sprintf.sprintf(runtime, to, "\\x%02X", c);
                } else {
                    to.append(bytes, p, cl);
                }
                p += cl;
            }
        }
    }

    // option_to_str
    public static void appendOptions(ByteList to, RegexpOptions options) {
        if (options.isMultiline()) to.append((byte)'m');
        if (options.isIgnorecase()) to.append((byte)'i');
        if (options.isExtended()) to.append((byte)'x');
    }

    public static int readEscapedByte(Ruby runtime, byte[] to, int toP, byte[] bytes, int p, int end, ByteList str, ErrorMode mode) {
        if (p == end || bytes[p++] != (byte)'\\') raisePreprocessError(runtime, str, "too short escaped multibyte character", mode);

        boolean metaPrefix = false, ctrlPrefix = false;
        int code = 0;
        while (true) {
            if (p == end) raisePreprocessError(runtime, str, "too short escape sequence", mode);

            switch (bytes[p++]) {
                case '\\': code = '\\'; break;
                case 'n': code = '\n'; break;
                case 't': code = '\t'; break;
                case 'r': code = '\r'; break;
                case 'f': code = '\f'; break;
                case 'v': code = '\013'; break;
                case 'a': code = '\007'; break;
                case 'e': code = '\033'; break;

            /* \OOO */
                case '0': case '1': case '2': case '3':
                case '4': case '5': case '6': case '7':
                    p--;
                    int olen = end < p + 3 ? end - p : 3;
                    code = StringSupport.scanOct(bytes, p, olen);
                    p += StringSupport.octLength(bytes, p, olen);
                    break;

                case 'x': /* \xHH */
                    int hlen = end < p + 2 ? end - p : 2;
                    code = StringSupport.scanHex(bytes, p, hlen);
                    int len = StringSupport.hexLength(bytes, p, hlen);
                    if (len < 1) raisePreprocessError(runtime, str, "invalid hex escape", mode);
                    p += len;
                    break;

                case 'M': /* \M-X, \M-\C-X, \M-\cX */
                    if (metaPrefix) raisePreprocessError(runtime, str, "duplicate meta escape", mode);
                    metaPrefix = true;
                    if (p + 1 < end && bytes[p++] == (byte)'-' && (bytes[p] & 0x80) == 0) {
                        if (bytes[p] == (byte)'\\') {
                            p++;
                            continue;
                        } else {
                            code = bytes[p++] & 0xff;
                            break;
                        }
                    }
                    raisePreprocessError(runtime, str, "too short meta escape", mode);

                case 'C': /* \C-X, \C-\M-X */
                    if (p == end || bytes[p++] != (byte)'-') raisePreprocessError(runtime, str, "too short control escape", mode);

                case 'c': /* \cX, \c\M-X */
                    if (ctrlPrefix) raisePreprocessError(runtime, str, "duplicate control escape", mode);
                    ctrlPrefix = true;
                    if (p < end && (bytes[p] & 0x80) == 0) {
                        if (bytes[p] == (byte)'\\') {
                            p++;
                            continue;
                        } else {
                            code = bytes[p++] & 0xff;
                            break;
                        }
                    }
                    raisePreprocessError(runtime, str, "too short control escape", mode);
                default:
                    raisePreprocessError(runtime, str, "unexpected escape sequence", mode);
            } // switch

            if (code < 0 || code > 0xff) raisePreprocessError(runtime, str, "invalid escape code", mode);

            if (ctrlPrefix) code &= 0x1f;
            if (metaPrefix) code |= 0x80;

            to[toP] = (byte)code;
            return p;
        } // while
    }

    /**
     * Unescape escaped non-ascii character at start position, appending all
     * to the given bytelist if provided.
     *
     * @param runtime current runtime
     * @param to output bytelist; if null, no appending will be done
     * @param bytes incoming bytes
     * @param p start position
     * @param end end position
     * @param enc bytes' encoding
     * @param encp out param for fixed encoding
     * @param str original bytes wrapper
     * @param mode error mode
     * @return new position after performing unescaping
     */
    // MRI: unescape_escaped_nonascii
    private static int unescapeEscapedNonAscii(Ruby runtime, ByteList to, byte[]bytes, int p, int end, Encoding enc, Encoding[]encp, ByteList str, ErrorMode mode) {
        byte[]chBuf = new byte[enc.maxLength()];
        int chLen = 0;

        do {
            p = readEscapedByte(runtime, chBuf, chLen++, bytes, p, end, str, mode);
        } while (chLen < enc.maxLength() && StringSupport.MBCLEN_NEEDMORE_P(StringSupport.preciseLength(enc, chBuf, 0, chLen)));

        int cl = StringSupport.preciseLength(enc, chBuf, 0, chLen);
        if (cl == -1) {
            raisePreprocessError(runtime, str, "invalid multibyte escape", mode); // MBCLEN_INVALID_P
        }

        if (chLen > 1 || (chBuf[0] & 0x80) != 0) {
            if (to != null) to.append(chBuf, 0, chLen);

            if (encp[0] == null) {
                encp[0] = enc;
            } else if (encp[0] != enc) {
                raisePreprocessError(runtime, str, "escaped non ASCII character in UTF-8 regexp", mode);
            }
        } else {
            if (to != null) Sprintf.sprintf(runtime, to, "\\x%02X", chBuf[0] & 0xff);
        }
        return p;
    }

    /**
     * Unescape unicode characters at given offset, appending to the given
     * out buffer if provided.
     *
     * @param runtime current runtime
     * @param to output buffer; if null, no appending will be done
     * @param bytes input bytes
     * @param p start position
     * @param end end position
     * @param encp out param for fixed encoding
     * @param str original bytes wrapper
     * @param mode error mode
     * @return new position after unescaping
     */
    private static int unescapeUnicodeList(Ruby runtime, ByteList to, byte[]bytes, int p, int end, Encoding[]encp, ByteList str, ErrorMode mode) {
        while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) p++;

        boolean hasUnicode = false;
        while (true) {
            int code = StringSupport.scanHex(bytes, p, end - p);
            int len = StringSupport.hexLength(bytes, p, end - p);
            if (len == 0) break;
            if (len > 6) raisePreprocessError(runtime, str, "invalid Unicode range", mode);
            p += len;
            if (to != null) appendUtf8(runtime, to, code, encp, str, mode);
            hasUnicode = true;
            while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) p++;
        }

        if (!hasUnicode) raisePreprocessError(runtime, str, "invalid Unicode list", mode);
        return p;
    }

    /**
     * Unescape unicode BMP char at given offset, appending to the specified
     * buffer if non-null.
     *
     * @param runtime current runtime
     * @param to output buffer; if null, no appending will be done
     * @param bytes input bytes
     * @param p start position
     * @param end end position
     * @param encp out param for fixed encoding
     * @param str original bytes wrapper
     * @param mode error mode
     * @return new position after unescaping
     */
    private static int unescapeUnicodeBmp(Ruby runtime, ByteList to, byte[] bytes, int p, int end, Encoding[] encp, ByteList str, ErrorMode mode) {
        if (p + 4 > end) raisePreprocessError(runtime, str, "invalid Unicode escape", mode);
        int code = StringSupport.scanHex(bytes, p, 4);
        int len = StringSupport.hexLength(bytes, p, 4);
        if (len != 4) raisePreprocessError(runtime, str, "invalid Unicode escape", mode);
        appendUtf8(runtime, to, code, encp, str, mode);
        return p + 4;
    }

    /**
     * Append the given utf8 characters to the buffer, if given, checking for
     * errors along the way.
     *
     * @param runtime current runtime
     * @param to output buffer; if null, no appending will be done
     * @param code utf8 character code
     * @param enc output param for new encoding
     * @param str original wrapper of source bytes
     * @param mode error mode
     */
    private static void appendUtf8(Ruby runtime, ByteList to, int code, Encoding[] enc, ByteList str, ErrorMode mode) {
        checkUnicodeRange(runtime, code, str, ErrorMode.PREPROCESS);

        if (code < 0x80) {
            if (to != null) Sprintf.sprintf(runtime, to, "\\x%02X", code);
        } else {
            if (to != null) {
                to.ensure(to.getRealSize() + 6);
                to.setRealSize(to.getRealSize() + Pack.utf8Decode(runtime, to.getUnsafeBytes(), to.getBegin() + to.getRealSize(), code));
            }
            if (enc[0] == null) {
                enc[0] = UTF8Encoding.INSTANCE;
            } else if (!(enc[0].isUTF8())) {
                raisePreprocessError(runtime, str, "UTF-8 character in non UTF-8 regexp", mode);
            }
        }
    }

    private static void checkUnicodeRange(Ruby runtime, int code, ByteList str, ErrorMode mode) {
        // Unicode is can be only 21 bits long, int is enough
        if ((0xd800 <= code && code <= 0xdfff) /* Surrogates */ || 0x10ffff < code) {
            raisePreprocessError(runtime, str, "invalid Unicode range", mode);
        }
    }
}
