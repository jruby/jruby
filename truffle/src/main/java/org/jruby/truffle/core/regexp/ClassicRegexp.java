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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.jruby.truffle.core.regexp;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Syntax;
import org.joni.WarnCallback;
import org.joni.exception.JOniException;
import org.jruby.Ruby;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.parser.ReOptions;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpSupport;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.collections.WeakValuedMap;
import org.jruby.util.io.EncodingUtils;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.jruby.util.StringSupport.CR_BROKEN;
import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;
import static org.jruby.util.StringSupport.codeRangeScan;

public class ClassicRegexp implements ReOptions, EncodingCapable, MarshalEncoding {
    private final RubyContext context;
    private Regex pattern;
    private ByteList str = ByteList.EMPTY_BYTELIST;
    private RegexpOptions options;

    public void setLiteral() {
        options.setLiteral(true);
    }

    public boolean isLiteral() {
        return options.isLiteral();
    }

    public void setEncodingNone() {
        options.setEncodingNone(true);
    }

    @Override
    public Encoding getEncoding() {
        return pattern.getEncoding();
    }

    @Override
    public void setEncoding(Encoding encoding) {
        // FIXME: Which encoding should be changed here?
        // FIXME: transcode?
    }

    @Override
    public boolean shouldMarshalEncoding() {
        return getEncoding() != ASCIIEncoding.INSTANCE;
    }

    @Override
    public Encoding getMarshalEncoding() {
        return getEncoding();
    }
    // FIXME: Maybe these should not be static?
    static final WeakValuedMap<ByteList, Regex> patternCache = new WeakValuedMap<>();

    private static Regex makeRegexp(RubyContext runtime, ByteList bytes, RegexpOptions options, Encoding enc) {
        try {
            int p = bytes.getBegin();

            return new Regex(bytes.getUnsafeBytes(), p, p + bytes.getRealSize(), options.toJoniOptions(), enc, Syntax.DEFAULT, new WarnCallback() {
                @Override
                public void warn(String s) {
                    //
                }
            });
        } catch (Exception e) {
            throw new org.jruby.truffle.language.control.RaiseException(runtime.getCoreExceptions().regexpError(e.getMessage(), null));
        }
    }

    static Regex getRegexpFromCache(RubyContext runtime, ByteList bytes, Encoding enc, RegexpOptions options) {
        Regex regex = patternCache.get(bytes);
        if (regex != null && regex.getEncoding() == enc && regex.getOptions() == options.toJoniOptions()) return regex;
        regex = makeRegexp(runtime, bytes, options, enc);
        regex.setUserObject(bytes);
        patternCache.put(bytes, regex);
        return regex;
    }

    public static int matcherSearch(Matcher matcher, int start, int range, int option) {
        try {
            SearchMatchTask task = new SearchMatchTask(start, range, option, false);
            return task.run(null, matcher);
        } catch (InterruptedException e) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SearchMatchTask {
        final int start;
        final int range;
        final int option;
        final boolean match;

        SearchMatchTask(int start, int range, int option, boolean match) {
            this.start = start;
            this.range = range;
            this.option = option;
            this.match = match;
        }

        public Integer run(ThreadContext context, Matcher matcher) throws InterruptedException {
            return match ?
                    matcher.matchInterruptible(start, range, option) :
                    matcher.searchInterruptible(start, range, option);
        }
    }

    /** default constructor
     */
    ClassicRegexp(RubyContext context) {
        this.context = context;
        this.options = new RegexpOptions();
    }

    private ClassicRegexp(RubyContext context, ByteList str, RegexpOptions options) {
        this(context);
        str.getClass();

        regexpInitialize(str, str.getEncoding(), options);
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static ClassicRegexp newRegexp(RubyContext runtime, ByteList pattern, int options) {
        return newRegexp(runtime, pattern, RegexpOptions.fromEmbeddedOptions(options));
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static ClassicRegexp newRegexp(RubyContext runtime, ByteList pattern, RegexpOptions options) {
        try {
            return new ClassicRegexp(runtime, pattern, (RegexpOptions)options.clone());
        } catch (RaiseException re) {
            throw new org.jruby.truffle.language.control.RaiseException(runtime.getCoreExceptions().syntaxError(re.getMessage(), null));
        }
    }

    /**
     * throws RaiseException on error so parser can pick this up and give proper line and line number
     * error as opposed to any non-literal regexp creation which may raise a syntax error but will not
     * have this extra source info in the error message
     */
    public static ClassicRegexp newRegexpParser(RubyContext runtime, ByteList pattern, RegexpOptions options) {
        return new ClassicRegexp(runtime, pattern, (RegexpOptions)options.clone());
    }

    /** rb_reg_options
     */
    public RegexpOptions getOptions() {
        check();
        return options;
    }

    public final Regex getPattern() {
        check();
        return pattern;
    }

    private static void preprocessLight(RubyContext context, ByteList str, Encoding enc, Encoding[]fixedEnc, RegexpSupport.ErrorMode mode) {
        if (enc.isAsciiCompatible()) {
            fixedEnc[0] = null;
        } else {
            fixedEnc[0] = enc;
        }

        boolean hasProperty = unescapeNonAscii(context, null, str.getUnsafeBytes(), str.getBegin(), str.getBegin() + str.getRealSize(), enc, fixedEnc, str, mode);
        if (hasProperty && fixedEnc[0] == null) fixedEnc[0] = enc;
    }

    @SuppressWarnings("fallthrough")
    public static boolean unescapeNonAscii(RubyContext context, ByteList to, byte[] bytes, int p, int end, Encoding enc, Encoding[] encp, ByteList str, RegexpSupport.ErrorMode mode) {
        boolean hasProperty = false;
        byte[] buf = null;

        while (p < end) {
            int cl = StringSupport.preciseLength(enc, bytes, p, end);
            if (cl <= 0) raisePreprocessError(context, str, "invalid multibyte character", mode);
            if (cl > 1 || (bytes[p] & 0x80) != 0) {
                if (to != null) to.append(bytes, p, cl);
                p += cl;
                if (encp[0] == null) {
                    encp[0] = enc;
                } else if (encp[0] != enc) {
                    raisePreprocessError(context, str, "non ASCII character in UTF-8 regexp", mode);
                }
                continue;
            }
            int c;
            switch (c = bytes[p++] & 0xff) {
                case '\\':
                    if (p == end) raisePreprocessError(context, str, "too short escape sequence", mode);

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
                                p = readEscapedByte(context, buf, 0, bytes, p, end, str, mode);
                                c = buf[0];
                                if (c == (char)-1) return false;
                                if (to != null) {
                                    to.append(bytes, pbeg, p - pbeg);
                                }
                            }
                            else {
                                p = unescapeEscapedNonAscii(context, to, bytes, p, end, enc, encp, str, mode);
                            }
                            break;

                        case 'u':
                            if (p == end) raisePreprocessError(context, str, "too short escape sequence", mode);
                            if (bytes[p] == (byte)'{') { /* \\u{H HH HHH HHHH HHHHH HHHHHH ...} */
                                p++;
                                p = unescapeUnicodeList(context, to, bytes, p, end, encp, str, mode);
                                if (p == end || bytes[p++] != (byte)'}') raisePreprocessError(context, str, "invalid Unicode list", mode);
                            } else { /* \\uHHHH */
                                p = unescapeUnicodeBmp(context, to, bytes, p, end, encp, str, mode);
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

    private static int unescapeUnicodeBmp(RubyContext context, ByteList to, byte[] bytes, int p, int end, Encoding[] encp, ByteList str, RegexpSupport.ErrorMode mode) {
        if (p + 4 > end) raisePreprocessError(context, str, "invalid Unicode escape", mode);
        int code = StringSupport.scanHex(bytes, p, 4);
        int len = StringSupport.hexLength(bytes, p, 4);
        if (len != 4) raisePreprocessError(context, str, "invalid Unicode escape", mode);
        appendUtf8(context, to, code, encp, str, mode);
        return p + 4;
    }

    private static int unescapeUnicodeList(RubyContext context, ByteList to, byte[]bytes, int p, int end, Encoding[]encp, ByteList str, RegexpSupport.ErrorMode mode) {
        while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) p++;

        boolean hasUnicode = false;
        while (true) {
            int code = StringSupport.scanHex(bytes, p, end - p);
            int len = StringSupport.hexLength(bytes, p, end - p);
            if (len == 0) break;
            if (len > 6) raisePreprocessError(context, str, "invalid Unicode range", mode);
            p += len;
            if (to != null) appendUtf8(context, to, code, encp, str, mode);
            hasUnicode = true;
            while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) p++;
        }

        if (!hasUnicode) raisePreprocessError(context, str, "invalid Unicode list", mode);
        return p;
    }

    private static void appendUtf8(RubyContext context, ByteList to, int code, Encoding[] enc, ByteList str, RegexpSupport.ErrorMode mode) {
        checkUnicodeRange(context, code, str, mode);

        if (code < 0x80) {
            if (to != null) to.append(String.format("\\x%02X", code).getBytes(StandardCharsets.US_ASCII));
        } else {
            if (to != null) {
                to.ensure(to.getRealSize() + 6);
                to.setRealSize(to.getRealSize() + utf8Decode(context, to.getUnsafeBytes(), to.getBegin() + to.getRealSize(), code));
            }
            if (enc[0] == null) {
                enc[0] = UTF8Encoding.INSTANCE;
            } else if (!(enc[0].isUTF8())) {
                raisePreprocessError(context, str, "UTF-8 character in non UTF-8 regexp", mode);
            }
        }
    }

    public static int utf8Decode(RubyContext context, byte[]to, int p, int code) {
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
        throw new org.jruby.truffle.language.control.RaiseException(context.getCoreExceptions().rangeError("pack(U): value out of range", null));
    }

    private static void checkUnicodeRange(RubyContext context, int code, ByteList str, RegexpSupport.ErrorMode mode) {
        // Unicode is can be only 21 bits long, int is enough
        if ((0xd800 <= code && code <= 0xdfff) /* Surrogates */ || 0x10ffff < code) {
            raisePreprocessError(context, str, "invalid Unicode range", mode);
        }
    }

    private static int unescapeEscapedNonAscii(RubyContext context, ByteList to, byte[]bytes, int p, int end, Encoding enc, Encoding[]encp, ByteList str, RegexpSupport.ErrorMode mode) {
        byte[]chBuf = new byte[enc.maxLength()];
        int chLen = 0;

        p = readEscapedByte(context, chBuf, chLen++, bytes, p, end, str, mode);
        while (chLen < enc.maxLength() && StringSupport.MBCLEN_NEEDMORE_P(StringSupport.preciseLength(enc, chBuf, 0, chLen))) {
            p = readEscapedByte(context, chBuf, chLen++, bytes, p, end, str, mode);
        }

        int cl = StringSupport.preciseLength(enc, chBuf, 0, chLen);
        if (cl == -1) {
            raisePreprocessError(context, str, "invalid multibyte escape", mode); // MBCLEN_INVALID_P
        }

        if (chLen > 1 || (chBuf[0] & 0x80) != 0) {
            if (to != null) to.append(chBuf, 0, chLen);

            if (encp[0] == null) {
                encp[0] = enc;
            } else if (encp[0] != enc) {
                raisePreprocessError(context, str, "escaped non ASCII character in UTF-8 regexp", mode);
            }
        } else {
            if (to != null) to.append(String.format("\\x%02X", chBuf[0] & 0xff).getBytes(StandardCharsets.US_ASCII));
        }
        return p;
    }

    public static int raisePreprocessError(RubyContext context, ByteList str, String err, RegexpSupport.ErrorMode mode) {
        switch (mode) {
            case RAISE:
                throw new org.jruby.truffle.language.control.RaiseException(context.getCoreExceptions().regexpError(err, null));
            case PREPROCESS:
                throw new org.jruby.truffle.language.control.RaiseException(context.getCoreExceptions().argumentError("regexp preprocess failed: " + err, null));
            case DESC:
                // silent ?
        }
        return 0;
    }

    @SuppressWarnings("fallthrough")
    public static int readEscapedByte(RubyContext context, byte[] to, int toP, byte[] bytes, int p, int end, ByteList str, RegexpSupport.ErrorMode mode) {
        if (p == end || bytes[p++] != (byte)'\\') raisePreprocessError(context, str, "too short escaped multibyte character", mode);

        boolean metaPrefix = false, ctrlPrefix = false;
        int code = 0;
        while (true) {
            if (p == end) raisePreprocessError(context, str, "too short escape sequence", mode);

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
                    if (len < 1) raisePreprocessError(context, str, "invalid hex escape", mode);
                    p += len;
                    break;

                case 'M': /* \M-X, \M-\C-X, \M-\cX */
                    if (metaPrefix) raisePreprocessError(context, str, "duplicate meta escape", mode);
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
                    raisePreprocessError(context, str, "too short meta escape", mode);

                case 'C': /* \C-X, \C-\M-X */
                    if (p == end || bytes[p++] != (byte)'-') raisePreprocessError(context, str, "too short control escape", mode);

                case 'c': /* \cX, \c\M-X */
                    if (ctrlPrefix) raisePreprocessError(context, str, "duplicate control escape", mode);
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
                    raisePreprocessError(context, str, "too short control escape", mode);
                default:
                    raisePreprocessError(context, str, "unexpected escape sequence", mode);
            } // switch

            if (code < 0 || code > 0xff) raisePreprocessError(context, str, "invalid escape code", mode);

            if (ctrlPrefix) code &= 0x1f;
            if (metaPrefix) code |= 0x80;

            to[toP] = (byte)code;
            return p;
        } // while
    }

    public static void preprocessCheck(RubyContext runtime, ByteList bytes) {
        preprocess(runtime, bytes, bytes.getEncoding(), new Encoding[]{null}, RegexpSupport.ErrorMode.RAISE);
    }

    public static ByteList preprocess(RubyContext runtime, ByteList str, Encoding enc, Encoding[] fixedEnc, RegexpSupport.ErrorMode mode) {
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

    public static ByteList preprocessDRegexp(RubyContext context, ByteList[] strings, RegexpOptions options) {
        ByteList string = null;
        Encoding regexpEnc = null;

        for (int i = 0; i < strings.length; i++) {
            ByteList str = strings[i];
            final Encoding[] encodingHolder = new Encoding[]{null};
            regexpEnc = processDRegexpElement(context, options, regexpEnc, encodingHolder, str);
            if (string == null) {
                string = str.dup();
            } else {
                string.append(str);
            }
        }

        if (regexpEnc != null) string.setEncoding(regexpEnc);

        return string;
    }

    private static Encoding processDRegexpElement(RubyContext context, RegexpOptions options, Encoding regexpEnc, Encoding[] fixedEnc, ByteList str) {
        Encoding strEnc = str.getEncoding();

        if (options.isEncodingNone() && strEnc != ASCIIEncoding.INSTANCE) {
            if (scanForCodeRange(str) != StringSupport.CR_7BIT) {
                throw new org.jruby.truffle.language.control.RaiseException(context.getCoreExceptions().regexpError("/.../n has a non escaped non ASCII character in non ASCII-8BIT script", null));
            }
            strEnc = ASCIIEncoding.INSTANCE;
        }

        // This used to call preprocess, but the resulting bytelist was not
        // used. Since the preprocessing error-checking can be done without
        // creating a new bytelist, I added a "light" path.
        ClassicRegexp.preprocessLight(context, str, strEnc, fixedEnc, RegexpSupport.ErrorMode.PREPROCESS);

        if (fixedEnc[0] != null) {
            if (regexpEnc != null && regexpEnc != fixedEnc[0]) {
                throw new org.jruby.truffle.language.control.RaiseException(context.getCoreExceptions().regexpError("encoding mismatch in dynamic regexp: " + new String(regexpEnc.getName()) + " and " + new String(fixedEnc[0].getName()), null));
            }
            regexpEnc = fixedEnc[0];
        }
        return regexpEnc;
    }

    private static int scanForCodeRange(ByteList str) {
        int cr;
        Encoding enc = str.getEncoding();
        if (enc.minLength() > 1 && enc.isDummy()) {
            cr = CR_BROKEN;
        } else {
            cr = codeRangeScan(EncodingUtils.getActualEncoding(enc, str), str);
        }
        return cr;
    }

    private void check() {
        if (pattern == null) throw getRuntime().newTypeError("uninitialized Regexp");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject recv, IRubyObject args) {
        return TypeConverter.convertToTypeWithCheck(args, context.runtime.getRegexp(), "to_regexp");
    }

    /** rb_reg_quote
     *
     */
    private static final int QUOTED_V = 11;
    public static ByteList quote19(ByteList bs, boolean asciiOnly) {
        int p = bs.getBegin();
        int end = p + bs.getRealSize();
        byte[] bytes = bs.getUnsafeBytes();
        Encoding enc = bs.getEncoding();

        metaFound: do {
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
                    continue;
                }

                switch (c) {
                case '[': case ']': case '{': case '}':
                case '(': case ')': case '|': case '-':
                case '*': case '.': case '\\':
                case '?': case '+': case '^': case '$':
                case ' ': case '#':
                case '\t': case '\f': case QUOTED_V: case '\n': case '\r':
                    break metaFound;
                }
                p += cl;
            }
            if (asciiOnly) {
                ByteList tmp = bs.shallowDup();
                tmp.setEncoding(USASCIIEncoding.INSTANCE);
                return tmp;
            }
            return bs;
        } while (false);

        ByteList result = new ByteList(end * 2);
        result.setEncoding(asciiOnly ? USASCIIEncoding.INSTANCE : bs.getEncoding());
        byte[]obytes = result.getUnsafeBytes();
        int op = p - bs.getBegin();
        System.arraycopy(bytes, bs.getBegin(), obytes, 0, op);

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
                int n = StringSupport.length(enc, bytes, p, end);
                while (n-- > 0) obytes[op++] = bytes[p++];
                continue;
            }
            p += cl;
            switch (c) {
            case '[': case ']': case '{': case '}':
            case '(': case ')': case '|': case '-':
            case '*': case '.': case '\\':
            case '?': case '+': case '^': case '$':
            case '#':
                op += enc.codeToMbc('\\', obytes, op);
                break;
            case ' ':
                op += enc.codeToMbc('\\', obytes, op);
                op += enc.codeToMbc(' ', obytes, op);
                continue;
            case '\t':
                op += enc.codeToMbc('\\', obytes, op);
                op += enc.codeToMbc('t', obytes, op);
                continue;
            case '\n':
                op += enc.codeToMbc('\\', obytes, op);
                op += enc.codeToMbc('n', obytes, op);
                continue;
            case '\r':
                op += enc.codeToMbc('\\', obytes, op);
                op += enc.codeToMbc('r', obytes, op);
                continue;
            case '\f':
                op += enc.codeToMbc('\\', obytes, op);
                op += enc.codeToMbc('f', obytes, op);
                continue;
            case QUOTED_V:
                op += enc.codeToMbc('\\', obytes, op);
                op += enc.codeToMbc('v', obytes, op);
                continue;
            }
            op += enc.codeToMbc(c, obytes, op);
        }

        result.setRealSize(op);
        return result;
    }

    // rb_reg_initialize
    public ClassicRegexp regexpInitialize(ByteList bytes, Encoding enc, RegexpOptions options) {
        this.options = options;

        //checkFrozen();
        // FIXME: Something unsets this bit, but we aren't...be more permissive until we figure this out
        //if (isLiteral()) throw runtime.newSecurityError("can't modify literal regexp");
        if (pattern != null) throw new org.jruby.truffle.language.control.RaiseException(context.getCoreExceptions().typeError("already initialized regexp", null));
        if (enc.isDummy()) throw new UnsupportedOperationException(); // RegexpSupport.raiseRegexpError19(runtime, bytes, enc, options, "can't make regexp with dummy encoding");

        Encoding[]fixedEnc = new Encoding[]{null};
        ByteList unescaped = preprocess(context, bytes, enc, fixedEnc, RegexpSupport.ErrorMode.RAISE);
        if (fixedEnc[0] != null) {
            if ((fixedEnc[0] != enc && options.isFixed()) ||
               (fixedEnc[0] != ASCIIEncoding.INSTANCE && options.isEncodingNone())) {
                    throw new UnsupportedOperationException();
                    //RegexpSupport.raiseRegexpError19(runtime, bytes, enc, options, "incompatible character encoding");
            }
            if (fixedEnc[0] != ASCIIEncoding.INSTANCE) {
                options.setFixed(true);
                enc = fixedEnc[0];
            }
        } else if (!options.isFixed()) {
            enc = USASCIIEncoding.INSTANCE;
        }

        if (fixedEnc[0] != null) options.setFixed(true);
        if (options.isEncodingNone()) setEncodingNone();

        pattern = getRegexpFromCache(context, unescaped, enc, options);
        bytes.getClass();
        str = bytes;
        return this;
    }

    @JRubyMethod
    public IRubyObject options() {
        return getRuntime().newFixnum(getOptions().toOptions());
    }

    public static void appendOptions(ByteList to, RegexpOptions options) {
        if (options.isMultiline()) to.append((byte)'m');
        if (options.isIgnorecase()) to.append((byte)'i');
        if (options.isExtended()) to.append((byte)'x');
    }

    public ByteList toByteList() {
        check();

        RegexpOptions newOptions = (RegexpOptions)options.clone();
        int p = str.getBegin();
        int len = str.getRealSize();
        byte[] bytes = str.getUnsafeBytes();

        ByteList result = new ByteList(len);
        result.append((byte)'(').append((byte)'?');

        again: do {
            if (len >= 4 && bytes[p] == '(' && bytes[p + 1] == '?') {
                boolean err = true;
                p += 2;
                if ((len -= 2) > 0) {
                    do {
                        if (bytes[p] == 'm') {
                            newOptions.setMultiline(true);
                        } else if (bytes[p] == 'i') {
                            newOptions.setIgnorecase(true);
                        } else if (bytes[p] == 'x') {
                            newOptions.setExtended(true);
                        } else {
                            break;
                        }
                        p++;
                    } while (--len > 0);
                }
                if (len > 1 && bytes[p] == '-') {
                    ++p;
                    --len;
                    do {
                        if (bytes[p] == 'm') {
                            newOptions.setMultiline(false);
                        } else if (bytes[p] == 'i') {
                            newOptions.setIgnorecase(false);
                        } else if (bytes[p] == 'x') {
                            newOptions.setExtended(false);
                        } else {
                            break;
                        }
                        p++;
                    } while (--len > 0);
                }

                if (bytes[p] == ')') {
                    --len;
                    ++p;
                    continue again;
                }

                if (bytes[p] == ':' && bytes[p + len - 1] == ')') {
                    try {
                        new Regex(bytes, ++p, p + (len -= 2), Option.DEFAULT, str.getEncoding(), Syntax.DEFAULT);
                        err = false;
                    } catch (JOniException e) {
                        err = true;
                    }
                }

                if (err) {
                    newOptions = options;
                    p = str.getBegin();
                    len = str.getRealSize();
                }
            }

            appendOptions(result, newOptions);

            if (!newOptions.isEmbeddable()) {
                result.append((byte)'-');
                if (!newOptions.isMultiline()) result.append((byte)'m');
                if (!newOptions.isIgnorecase()) result.append((byte)'i');
                if (!newOptions.isExtended()) result.append((byte)'x');
            }
            result.append((byte)':');
            appendRegexpString19(result, bytes, p, len, str.getEncoding(), null);

            result.append((byte)')');
            result.setEncoding(getEncoding());
            return result;
            //return RubyString.newString(getRuntime(), result, getEncoding()).infectBy(this);
        } while (true);
    }

    public void appendRegexpString19(ByteList to, byte[] bytes, int start, int len, Encoding enc, Encoding resEnc) {
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
            boolean isUnicode = StringSupport.isUnicode(enc);
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
                        to.append(String.format("\\x%02X", c).getBytes(StandardCharsets.US_ASCII));
                    } else if (resEnc != null) {
                        int code = enc.mbcToCode(bytes, p, end);
                        to.append(String.format(StringSupport.escapedCharFormat(code, isUnicode), code).getBytes(StandardCharsets.US_ASCII));
                    } else {
                        to.append(bytes, p, l);
                    }
                    p += l;

                    continue;
                } else if (enc.isPrint(c)) {
                    to.append(bytes, p, cl);
                } else if (!enc.isSpace(c)) {
                    to.append(String.format("\\x%02X", c).getBytes(StandardCharsets.US_ASCII));
                } else {
                    to.append(bytes, p, cl);
                }
                p += cl;
            }
        }
    }

    public String[] getNames() {
        int nameLength = pattern.numberOfNames();
        if (nameLength == 0) return EMPTY_STRING_ARRAY;

        String[] names = new String[nameLength];
        int j = 0;
        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();
            names[j++] = new String(e.name, e.nameP, e.nameEnd - e.nameP).intern();
        }

        return names;
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        Encoding enc = (pattern == null) ? str.getEncoding() : pattern.getEncoding();
        return context.runtime.getEncodingService().getEncoding(enc);
    }

    public Ruby getRuntime() {
        throw new UnsupportedOperationException();
    }

}
