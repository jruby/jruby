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
package org.jruby;

import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.anno.FrameField.LASTLINE;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.Syntax;
import org.joni.exception.JOniException;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.parser.ReOptions;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.Pack;
import org.jruby.util.RegexpOptions;
import org.jruby.util.Sprintf;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.collections.WeakValuedMap;

import java.util.Iterator;

@JRubyClass(name="Regexp")
public class RubyRegexp extends RubyObject implements ReOptions, EncodingCapable, MarshalEncoding {
    private Regex pattern;
    private ByteList str = ByteList.EMPTY_BYTELIST;
    private RegexpOptions options;

    public static final int ARG_ENCODING_FIXED     =   ReOptions.RE_FIXED;
    public static final int ARG_ENCODING_NONE      =   ReOptions.RE_NONE;

    public void setLiteral() {
        options.setLiteral(true);
    }

    public void clearLiteral() {
        options.setLiteral(false);
    }

    public boolean isLiteral() {
        return options.isLiteral();
    }

    public boolean isKCodeDefault() {
        return options.isKcodeDefault();
    }

    public void setEncodingNone() {
        options.setEncodingNone(true);
    }
    
    public void clearEncodingNone() {
        options.setEncodingNone(false);
    }

    public boolean isEncodingNone() {
        return options.isEncodingNone();
    }

    public KCode getKCode() {
        return options.getKCode();
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
    static final WeakValuedMap<ByteList, Regex> patternCache = new WeakValuedMap();
    static final WeakValuedMap<ByteList, Regex> quotedPatternCache = new WeakValuedMap();
    static final WeakValuedMap<ByteList, Regex> preprocessedPatternCache = new WeakValuedMap();

    private static Regex makeRegexp(Ruby runtime, ByteList bytes, RegexpOptions options, Encoding enc) {
        try {
            int p = bytes.getBegin();
            return new Regex(bytes.getUnsafeBytes(), p, p + bytes.getRealSize(), options.toJoniOptions(), enc, Syntax.DEFAULT, runtime.getWarnings());
        } catch (Exception e) {
            raiseRegexpError19(runtime, bytes, enc, options, e.getMessage());
            return null; // not reached
        }
    }

    static Regex getRegexpFromCache(Ruby runtime, ByteList bytes, Encoding enc, RegexpOptions options) {
        Regex regex = patternCache.get(bytes);
        if (regex != null && regex.getEncoding() == enc && regex.getOptions() == options.toJoniOptions()) return regex;
        regex = makeRegexp(runtime, bytes, options, enc);
        regex.setUserObject(bytes);
        patternCache.put(bytes, regex);
        return regex;
    }

    static Regex getQuotedRegexpFromCache(Ruby runtime, ByteList bytes, Encoding enc, RegexpOptions options) {
        Regex regex = quotedPatternCache.get(bytes);
        if (regex != null && regex.getEncoding() == enc && regex.getOptions() == options.toJoniOptions()) return regex;
        ByteList quoted = quote(bytes, enc);
        regex = makeRegexp(runtime, quoted, options, enc);
        regex.setUserObject(quoted);
        quotedPatternCache.put(bytes, regex);
        return regex;
    }

    static Regex getQuotedRegexpFromCache19(Ruby runtime, ByteList bytes, RegexpOptions options, boolean asciiOnly) {
        Regex regex = quotedPatternCache.get(bytes);
        Encoding enc = asciiOnly ? USASCIIEncoding.INSTANCE : bytes.getEncoding();
        if (regex != null && regex.getEncoding() == enc && regex.getOptions() == options.toJoniOptions()) return regex;
        ByteList quoted = quote19(bytes, asciiOnly);
        regex = makeRegexp(runtime, quoted, options, quoted.getEncoding());
        regex.setUserObject(quoted);
        quotedPatternCache.put(bytes, regex);
        return regex;
    }

    private static Regex getPreprocessedRegexpFromCache(Ruby runtime, ByteList bytes, Encoding enc, RegexpOptions options, ErrorMode mode) {
        Regex regex = preprocessedPatternCache.get(bytes);
        if (regex != null && regex.getEncoding() == enc && regex.getOptions() == options.toJoniOptions()) return regex;
        ByteList preprocessed = preprocess(runtime, bytes, enc, new Encoding[]{null}, ErrorMode.RAISE);
        regex = makeRegexp(runtime, preprocessed, options, enc);
        regex.setUserObject(preprocessed);
        preprocessedPatternCache.put(bytes, regex);
        return regex;
    }

    public static RubyClass createRegexpClass(Ruby runtime) {
        RubyClass regexpClass = runtime.defineClass("Regexp", runtime.getObject(), REGEXP_ALLOCATOR);
        runtime.setRegexp(regexpClass);

        regexpClass.setClassIndex(ClassIndex.REGEXP);
        regexpClass.setReifiedClass(RubyRegexp.class);
        
        regexpClass.kindOf = new RubyModule.JavaClassKindOf(RubyRegexp.class);

        regexpClass.defineConstant("IGNORECASE", runtime.newFixnum(RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", runtime.newFixnum(RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", runtime.newFixnum(RE_OPTION_MULTILINE));

        regexpClass.defineConstant("FIXEDENCODING", runtime.newFixnum(RE_FIXED));
        regexpClass.defineConstant("NOENCODING", runtime.newFixnum(RE_NONE));

        regexpClass.defineAnnotatedMethods(RubyRegexp.class);
        regexpClass.getSingletonClass().defineAlias("compile", "new");

        return regexpClass;
    }

    private static ObjectAllocator REGEXP_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyRegexp(runtime, klass);
        }
    };
    
    public static int matcherSearch(Ruby runtime, Matcher matcher, int start, int range, int option) {
        try {
            RubyThread thread = runtime.getCurrentContext().getThread();
            SearchMatchTask task = new SearchMatchTask(thread, matcher, start, range, option, false);
            thread.executeBlockingTask(task);
            return task.retval;
        } catch (InterruptedException e) {
            throw runtime.newInterruptedRegexpError("Regexp Interrupted");
        }
    }
    
    public static int matcherMatch(Ruby runtime, Matcher matcher, int start, int range, int option) {
        try {
            RubyThread thread = runtime.getCurrentContext().getThread();
            SearchMatchTask task = new SearchMatchTask(thread, matcher, start, range, option, true);
            thread.executeBlockingTask(task);
            return task.retval;
        } catch (InterruptedException e) {
            throw runtime.newInterruptedRegexpError("Regexp Interrupted");
        }
    }
    
    private static class SearchMatchTask implements RubyThread.BlockingTask {
        int retval;
        final RubyThread thread;
        final Matcher matcher;
        final int start;
        final int range;
        final int option;
        final boolean match;
        
        SearchMatchTask(RubyThread thread, Matcher matcher, int start, int range, int option, boolean match) {
            this.thread = thread;
            this.matcher = matcher;
            this.start = start;
            this.range = range;
            this.option = option;
            this.match = match;
        }
        
        @Override
        public void run() throws InterruptedException {
            retval = match ?
                    matcher.matchInterruptible(start, range, option) :
                    matcher.searchInterruptible(start, range, option);
        }

        @Override
        public void wakeup() {
            thread.getNativeThread().interrupt();
        }
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.REGEXP;
    }

    /** used by allocator
     */
    private RubyRegexp(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        this.options = new RegexpOptions();
    }

    /** default constructor
     */
    private RubyRegexp(Ruby runtime) {
        super(runtime, runtime.getRegexp());
        this.options = new RegexpOptions();
    }

    private RubyRegexp(Ruby runtime, ByteList str) {
        this(runtime);
        str.getClass();
        this.str = str;
        this.pattern = getRegexpFromCache(runtime, str, getEncoding(runtime, str), RegexpOptions.NULL_OPTIONS);
    }

    private RubyRegexp(Ruby runtime, ByteList str, RegexpOptions options) {
        this(runtime);
        str.getClass();

        initializeCommon19(str, str.getEncoding(), options);
    }

    private Encoding getEncoding(Ruby runtime, ByteList str) {
        return str.getEncoding();
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newRegexp(Ruby runtime, String pattern, RegexpOptions options) {
        return newRegexp(runtime, ByteList.create(pattern), options);
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newRegexp(Ruby runtime, ByteList pattern, int options) {
        return newRegexp(runtime, pattern, RegexpOptions.fromEmbeddedOptions(options));
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newRegexp(Ruby runtime, ByteList pattern, RegexpOptions options) {
        try {
            return new RubyRegexp(runtime, pattern, (RegexpOptions)options.clone());
        } catch (RaiseException re) {
            throw runtime.newSyntaxError(re.getMessage());
        }
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newDRegexp(Ruby runtime, RubyString pattern, RegexpOptions options) {
        try {
            return new RubyRegexp(runtime, pattern.getByteList(), (RegexpOptions)options.clone());
        } catch (RaiseException re) {
            throw runtime.newRegexpError(re.getMessage());
        }
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newDRegexp(Ruby runtime, RubyString pattern, int joniOptions) {
        try {
            RegexpOptions options = RegexpOptions.fromJoniOptions(joniOptions);
            return new RubyRegexp(runtime, pattern.getByteList(), options);
        } catch (RaiseException re) {
            throw runtime.newRegexpError(re.getMessage());
        }
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newDRegexpEmbedded(Ruby runtime, RubyString pattern, int embeddedOptions) {
        try {
            RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
            // FIXME: Massive hack (fix in DRegexpNode too for interpreter)
            if (pattern.getEncoding() == USASCIIEncoding.INSTANCE) {
                pattern.setEncoding(ASCIIEncoding.INSTANCE);
            }
            return new RubyRegexp(runtime, pattern.getByteList(), options);
        } catch (RaiseException re) {
            throw runtime.newRegexpError(re.getMessage());
        }
    }
    
    public static RubyRegexp newDRegexpEmbedded19(Ruby runtime, IRubyObject[] strings, int embeddedOptions) {
        try {
            RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
            RubyString pattern = preprocessDRegexp(runtime, strings, options);
            
            return new RubyRegexp(runtime, pattern.getByteList(), options);
        } catch (RaiseException re) {
            throw runtime.newRegexpError(re.getMessage());
        }
        
    }
    
    public static RubyRegexp newRegexp(Ruby runtime, ByteList pattern) {
        return new RubyRegexp(runtime, pattern);
    }

    static RubyRegexp newRegexp(Ruby runtime, ByteList str, Regex pattern) {
        RubyRegexp regexp = new RubyRegexp(runtime);
        str.getClass();
        regexp.str = str;
        regexp.options = RegexpOptions.fromJoniOptions(pattern.getOptions());
        regexp.pattern = pattern;
        return regexp;
    }
    
    // internal usage (Complex/Rational)
    static RubyRegexp newDummyRegexp(Ruby runtime, Regex regex) {
        RubyRegexp regexp = new RubyRegexp(runtime);
        regexp.pattern = regex;
        regexp.str = ByteList.EMPTY_BYTELIST;
        regexp.options.setFixed(true);
        return regexp;
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

    private static void encodingMatchError(Ruby runtime, Regex pattern, Encoding strEnc) {
        throw runtime.newEncodingCompatibilityError("incompatible encoding regexp match (" +
                pattern.getEncoding() + " regexp with " + strEnc + " string)");
    }

    private Encoding checkEncoding(RubyString str, boolean warn) {
        if (str.scanForCodeRange() == StringSupport.CR_BROKEN) {
            throw getRuntime().newArgumentError("invalid byte sequence in " + str.getEncoding());
        }
        check();
        Encoding enc = str.getEncoding();
        if (!enc.isAsciiCompatible()) {
            if (enc != pattern.getEncoding()) encodingMatchError(getRuntime(), pattern, enc);
        } else if (options.isFixed()) {
            if (enc != pattern.getEncoding() && 
               (!pattern.getEncoding().isAsciiCompatible() ||
               str.scanForCodeRange() != StringSupport.CR_7BIT)) encodingMatchError(getRuntime(), pattern, enc);
            enc = pattern.getEncoding();
        }
        if (warn && isEncodingNone() && enc != ASCIIEncoding.INSTANCE && str.scanForCodeRange() != StringSupport.CR_7BIT) {
            getRuntime().getWarnings().warn(ID.REGEXP_MATCH_AGAINST_STRING, "regexp match /.../n against to " + enc + " string");
        }
        return enc;
    }

    public final Regex preparePattern(RubyString str) {
        check();
        Encoding enc = checkEncoding(str, true);
        if (enc == pattern.getEncoding()) return pattern;
        return getPreprocessedRegexpFromCache(getRuntime(), this.str, enc, options, ErrorMode.PREPROCESS);
    }

    static Regex preparePattern(Ruby runtime, Regex pattern, RubyString str) {
        if (str.scanForCodeRange() == StringSupport.CR_BROKEN) {
            throw runtime.newArgumentError("invalid byte sequence in " + str.getEncoding());
        }
        Encoding enc = str.getEncoding();
        if (!enc.isAsciiCompatible()) {
            if (enc != pattern.getEncoding()) encodingMatchError(runtime, pattern, enc);
        }
        // TODO: check for isKCodeDefault() somehow
//        if (warn && isEncodingNone() && enc != ASCIIEncoding.INSTANCE && str.scanForCodeRange() != StringSupport.CR_7BIT) {
//            getRuntime().getWarnings().warn(ID.REGEXP_MATCH_AGAINST_STRING, "regexp match /.../n against to " + enc + " string");
//        }
        if (enc == pattern.getEncoding()) return pattern;
        return getPreprocessedRegexpFromCache(runtime, (ByteList)pattern.getUserObject(), enc, RegexpOptions.fromJoniOptions(pattern.getOptions()), ErrorMode.PREPROCESS);
    }

    private static enum ErrorMode {RAISE, PREPROCESS, DESC} 

    private static int raisePreprocessError(Ruby runtime, ByteList str, String err, ErrorMode mode) {
        switch (mode) {
        case RAISE:
            raiseRegexpError19(runtime, str, str.getEncoding(), RegexpOptions.NULL_OPTIONS, err);
        case PREPROCESS:
            throw runtime.newArgumentError("regexp preprocess failed: " + err);
        case DESC:
            // silent ?
        }
        return 0;
    }

    private static int readEscapedByte(Ruby runtime, byte[]to, int toP, byte[]bytes, int p, int end, ByteList str, ErrorMode mode) {
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
    // MRI: unescape_escapted_nonascii
    private static int unescapeEscapedNonAscii(Ruby runtime, ByteList to, byte[]bytes, int p, int end, Encoding enc, Encoding[]encp, ByteList str, ErrorMode mode) {
        byte[]chBuf = new byte[enc.maxLength()];
        int chLen = 0;

        p = readEscapedByte(runtime, chBuf, chLen++, bytes, p, end, str, mode);
        while (chLen < enc.maxLength() && StringSupport.preciseLength(enc, chBuf, 0, chLen) < -1) { // MBCLEN_NEEDMORE_P
            p = readEscapedByte(runtime, chBuf, chLen++, bytes, p, end, str, mode);
        }

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

    private static void checkUnicodeRange(Ruby runtime, int code, ByteList str, ErrorMode mode) {
        // Unicode is can be only 21 bits long, int is enough
        if ((0xd800 <= code && code <= 0xdfff) /* Surrogates */ || 0x10ffff < code) {
            raisePreprocessError(runtime, str, "invalid Unicode range", mode);
        }
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
        checkUnicodeRange(runtime, code, str, mode);

        if (code < 0x80) {
            if (to != null) Sprintf.sprintf(runtime, to, "\\x%02X", code);
        } else {
            if (to != null) {
                to.ensure(to.getRealSize() + 6);
                to.setRealSize(to.getRealSize() + Pack.utf8Decode(runtime, to.getUnsafeBytes(), to.getBegin() + to.getRealSize(), code));
            }
            if (enc[0] == null) {
                enc[0] = UTF8Encoding.INSTANCE;
            } else if (!(enc[0] instanceof UTF8Encoding)) { // do not load the class if not used
                raisePreprocessError(runtime, str, "UTF-8 character in non UTF-8 regexp", mode);
            }
        }
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
     * Unescape non-ascii elements in the given string, appending the results
     * to the given bytelist if provided.
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
    private static boolean unescapeNonAscii(Ruby runtime, ByteList to, byte[]bytes, int p, int end, Encoding enc, Encoding[]encp, ByteList str, ErrorMode mode) {
        boolean hasProperty = false;

        while (p < end) {
            int cl = StringSupport.preciseLength(enc, bytes, p, end);
            if (cl <= 0) raisePreprocessError(runtime, str, "invalid multibyte character", mode);
            if (cl > 1 || (bytes[p] & 0x80) != 0) {
                if (to != null) to.append(bytes, p, cl);
                p += cl;
                if (encp[0] == null) {
                    encp[0] = enc;
                } else if (encp[0] != enc) {
                    raisePreprocessError(runtime, str, "non ASCII character in UTF-8 regexp", mode);
                }
                continue;
            }
            int c;
            switch (c = bytes[p++] & 0xff) {
            case '\\':
                if (p == end) raisePreprocessError(runtime, str, "too short escape sequence", mode);

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
                    p = unescapeEscapedNonAscii(runtime, to, bytes, p - 2, end, enc, encp, str, mode);
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
    private static ByteList preprocess(Ruby runtime, ByteList str, Encoding enc, Encoding[]fixedEnc, ErrorMode mode) {
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
     * Preprocess the given string for use in regexp, raising errors for encoding
     * incompatibilities that arise.
     * 
     * This version does not produce a new, unescaped version of the bytelist,
     * and simply does the string-walking portion of the logic.
     * 
     * @param runtime current runtime
     * @param str string to preprocess
     * @param enc string's encoding
     * @param fixedEnc new encoding after fixing
     * @param mode mode of errors
     */
    private static void preprocessLight(Ruby runtime, ByteList str, Encoding enc, Encoding[]fixedEnc, ErrorMode mode) {
        if (enc.isAsciiCompatible()) {
            fixedEnc[0] = null;
        } else {
            fixedEnc[0] = enc;
        }

        boolean hasProperty = unescapeNonAscii(runtime, null, str.getUnsafeBytes(), str.getBegin(), str.getBegin() + str.getRealSize(), enc, fixedEnc, str, mode);
        if (hasProperty && fixedEnc[0] == null) fixedEnc[0] = enc;
    }

    public static void preprocessCheck(Ruby runtime, ByteList bytes) {
        preprocess(runtime, bytes, bytes.getEncoding(), new Encoding[]{null}, ErrorMode.RAISE);
    }

    public static RubyString preprocessDRegexp(Ruby runtime, RubyString[] strings, int embeddedOptions) {
        return preprocessDRegexp(runtime, strings, RegexpOptions.fromEmbeddedOptions(embeddedOptions));
    }
    
    // rb_reg_preprocess_dregexp
    public static RubyString preprocessDRegexp(Ruby runtime, IRubyObject[] strings, RegexpOptions options) {
        RubyString string = null;
        Encoding regexpEnc = null;
        Encoding[] fixedEnc = new Encoding[1];
        
        for (int i = 0; i < strings.length; i++) {
            RubyString str = strings[i].convertToString();
            Encoding strEnc = str.getEncoding();
            
            if (options.isEncodingNone() && strEnc != ASCIIEncoding.INSTANCE) {
                if (str.scanForCodeRange() != StringSupport.CR_7BIT) {
                    throw runtime.newRegexpError("/.../n has a non escaped non ASCII character in non ASCII-8BIT script");
                }
                strEnc = ASCIIEncoding.INSTANCE;
            }
            
            // This used to call preprocess, but the resulting bytelist was not
            // used. Since the preprocessing error-checking can be done without
            // creating a new bytelist, I added a "light" path.
            RubyRegexp.preprocessLight(runtime, str.getByteList(), strEnc, fixedEnc, RubyRegexp.ErrorMode.PREPROCESS);
            
            if (fixedEnc[0] != null) {
                if (regexpEnc != null && regexpEnc != fixedEnc[0]) {
                    throw runtime.newRegexpError("encoding mismatch in dynamic regexp: " + new String(regexpEnc.getName()) + " and " + new String(fixedEnc[0].getName()));
                }
                regexpEnc = fixedEnc[0];
            }
            
            if (string == null) {
                string = (RubyString)str.dup();
            } else {
                string.append19(str);
            }
        }
        
        if (regexpEnc != null) {
            string.setEncoding(regexpEnc);
        }

        return string;
    }

    private void check() {
        if (pattern == null) throw getRuntime().newTypeError("uninitialized Regexp");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject recv, IRubyObject args) {
        return TypeConverter.convertToTypeWithCheck(args, context.runtime.getRegexp(), "to_regexp");
    }

    /** rb_reg_s_quote
     * 
     */
    @JRubyMethod(name = {"quote", "escape"}, meta = true)
    public static IRubyObject quote19(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        RubyString str = operandCheck(runtime, arg);
        return RubyString.newStringShared(runtime, quote19(str.getByteList(), str.isAsciiOnly()));
    }

    /** rb_reg_quote
     *
     */
    private static ByteList quote(ByteList bs, Encoding enc) {
        int p = bs.getBegin();
        int end = p + bs.getRealSize();
        byte[]bytes = bs.getUnsafeBytes();

        metaFound: do {
            for(; p < end; p++) {
                int c = bytes[p] & 0xff;
                int cl = enc.length(bytes, p, end);
                if (cl != 1) {
                    while (cl-- > 0 && p < end) p++;
                    p--;
                    continue;
                }
                switch (c) {
                case '[': case ']': case '{': case '}':
                case '(': case ')': case '|': case '-':
                case '*': case '.': case '\\':
                case '?': case '+': case '^': case '$':
                case ' ': case '#':
                case '\t': case '\f': case '\n': case '\r':
                    break metaFound;
                }
            }
            return bs;
        } while (false);

        ByteList result = new ByteList(end * 2);
        byte[]obytes = result.getUnsafeBytes();
        int op = p - bs.getBegin();
        System.arraycopy(bytes, bs.getBegin(), obytes, 0, op);

        for(; p < end; p++) {
            int c = bytes[p] & 0xff;
            int cl = enc.length(bytes, p, end);
            if (cl != 1) {
                while (cl-- > 0 && p < end) obytes[op++] = bytes[p++];
                p--;
                continue;
            }

            switch (c) {
            case '[': case ']': case '{': case '}':
            case '(': case ')': case '|': case '-':
            case '*': case '.': case '\\':
            case '?': case '+': case '^': case '$':
            case '#': obytes[op++] = '\\'; break;
            case ' ': obytes[op++] = '\\'; obytes[op++] = ' '; continue;
            case '\t':obytes[op++] = '\\'; obytes[op++] = 't'; continue;
            case '\n':obytes[op++] = '\\'; obytes[op++] = 'n'; continue;
            case '\r':obytes[op++] = '\\'; obytes[op++] = 'r'; continue;
            case '\f':obytes[op++] = '\\'; obytes[op++] = 'f'; continue;
            }
            obytes[op++] = (byte)c;
        }

        result.setRealSize(op);
        return result;
    }

    private static final int QUOTED_V = 11;
    public static ByteList quote19(ByteList bs, boolean asciiOnly) {
        int p = bs.getBegin();
        int end = p + bs.getRealSize();
        byte[]bytes = bs.getUnsafeBytes();
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

    /** rb_reg_s_last_match / match_getter
    *
    */
    @JRubyMethod(name = "last_match", meta = true, reads = BACKREF)
    public static IRubyObject last_match_s(ThreadContext context, IRubyObject recv) {
        IRubyObject match = context.getBackRef();
        if (match instanceof RubyMatchData) ((RubyMatchData)match).use();
        return match;
    }

    /** rb_reg_s_last_match
    *
    */
    @JRubyMethod(name = "last_match", meta = true, reads = BACKREF)
    public static IRubyObject last_match_s(ThreadContext context, IRubyObject recv, IRubyObject nth) {
        IRubyObject match = context.getBackRef();
        if (match.isNil()) return match;
        return nth_match(((RubyMatchData)match).backrefNumber(nth), match);
    }

    /** rb_reg_s_union
    *
    */
    public static IRubyObject union(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return union19(context, recv, args);
    }

    @JRubyMethod(name = "union", rest = true, meta = true)
    public static IRubyObject union19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject obj;
        if (args.length == 1 && !(obj = args[0].checkArrayType()).isNil()) {
            RubyArray ary = (RubyArray)obj;
            IRubyObject[]tmp = new IRubyObject[ary.size()];
            ary.copyInto(tmp, 0);
            args = tmp;
        }

        Ruby runtime = context.runtime;
        if (args.length == 0) {
            return runtime.getRegexp().newInstance(context, runtime.newString("(?!)"), Block.NULL_BLOCK);
        } else if (args.length == 1) {
            IRubyObject re = TypeConverter.convertToTypeWithCheck(args[0], runtime.getRegexp(), "to_regexp");
            return !re.isNil() ? re : newRegexp(runtime, ((RubyString)quote19(context, recv, args[0])).getByteList());
        } else {
            boolean hasAsciiOnly = false;
            RubyString source = runtime.newString();
            Encoding hasAsciiCompatFixed = null;
            Encoding hasAsciiIncompat = null;

            for(int i = 0; i < args.length; i++) {
                IRubyObject e = args[i];
                if (i > 0) source.cat((byte)'|');
                IRubyObject v = TypeConverter.convertToTypeWithCheck(args[i], runtime.getRegexp(), "to_regexp");
                Encoding enc;
                if (!v.isNil()) {
                    RubyRegexp regex = (RubyRegexp) v;
                    enc = regex.getEncoding();
                    if (!enc.isAsciiCompatible()) {
                        if (hasAsciiIncompat == null) { // First regexp of union sets kcode.
                            hasAsciiIncompat = enc;
                        } else if (hasAsciiIncompat != enc) { // n kcode doesn't match first one
                            throw runtime.newArgumentError("incompatible encodings: " + hasAsciiIncompat + " and " + enc);
                        }
                    } else if (regex.getOptions().isFixed()) {
                        if (hasAsciiCompatFixed == null) { // First regexp of union sets kcode.
                            hasAsciiCompatFixed = enc;
                        } else if (hasAsciiCompatFixed != enc) { // n kcode doesn't match first one
                            throw runtime.newArgumentError("incompatible encodings: " + hasAsciiCompatFixed + " and " + enc);
                        }
                    } else {
                        hasAsciiOnly = true;
                    }
                    v = regex.to_s();
                } else {
                    RubyString str = args[i].convertToString();
                    enc = str.getEncoding();

                    if (!enc.isAsciiCompatible()) {
                        if (hasAsciiIncompat == null) { // First regexp of union sets kcode.
                            hasAsciiIncompat = enc;
                        } else if (hasAsciiIncompat != enc) { // n kcode doesn't match first one
                            throw runtime.newArgumentError("incompatible encodings: " + hasAsciiIncompat + " and " + enc);
                        }
                    } else if (str.isAsciiOnly()) {
                        hasAsciiOnly = true;
                    } else {
                        if (hasAsciiCompatFixed == null) { // First regexp of union sets kcode.
                            hasAsciiCompatFixed = enc;
                        } else if (hasAsciiCompatFixed != enc) { // n kcode doesn't match first one
                            throw runtime.newArgumentError("incompatible encodings: " + hasAsciiCompatFixed + " and " + enc);
                        }
                    }
                    v = quote19(context, recv, str);
                }

                if (hasAsciiIncompat != null) {
                    if (hasAsciiOnly) {
                        throw runtime.newArgumentError("ASCII incompatible encoding: " + hasAsciiIncompat);
                    }
                    if (hasAsciiCompatFixed != null) {
                        throw runtime.newArgumentError("incompatible encodings: " + hasAsciiIncompat + " and " + hasAsciiCompatFixed);
                    }
                }

                // Enebo: not sure why this is needed.
                if (i == 0) source.setEncoding(enc);
                source.append(v);
            }
            if (hasAsciiIncompat != null) {
                source.setEncoding(hasAsciiIncompat);
            } else if (hasAsciiCompatFixed != null) {
                source.setEncoding(hasAsciiCompatFixed);
            } else {
                source.setEncoding(ASCIIEncoding.INSTANCE);
            }
            return runtime.getRegexp().newInstance(context, source, Block.NULL_BLOCK);
        }
    }

    // rb_enc_reg_raise
    private static void raiseRegexpError19(Ruby runtime, ByteList bytes, Encoding enc, RegexpOptions options, String err) {
        // TODO: we loose encoding information here, fix it
        throw runtime.newRegexpError(err + ": " + regexpDescription19(runtime, bytes, options, enc));
    }

    // rb_enc_reg_error_desc
    static ByteList regexpDescription19(Ruby runtime, ByteList bytes, RegexpOptions options, Encoding enc) {
        return regexpDescription19(runtime, bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize(), options, enc);
    }
    private static ByteList regexpDescription19(Ruby runtime, byte[] s, int start, int len, RegexpOptions options, Encoding enc) {
        ByteList description = new ByteList();
        description.setEncoding(enc);
        description.append((byte)'/');
        Encoding resultEnc = runtime.getDefaultInternalEncoding();
        if (resultEnc == null) resultEnc = runtime.getDefaultExternalEncoding();
        
        appendRegexpString19(runtime, description, s, start, len, enc, resultEnc);
        description.append((byte)'/');
        appendOptions(description, options);
        if (options.isEncodingNone()) description.append((byte) 'n');
        return description; 
    }

    /** rb_reg_init_copy
     */
    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject re) {
        if (this == re) return this;
        checkFrozen();

        if (getMetaClass().getRealClass() != re.getMetaClass().getRealClass()) {
            throw getRuntime().newTypeError("wrong argument type");
        }

        RubyRegexp regexp = (RubyRegexp)re;
        regexp.check();

        return initializeCommon19(regexp.str, regexp.str.getEncoding(), regexp.getOptions());
    }
    
    private int objectAsJoniOptions(IRubyObject arg) {
        if (arg instanceof RubyFixnum) return RubyNumeric.fix2int(arg);
        if (arg.isTrue()) return RE_OPTION_IGNORECASE;
        
        return 0;
    }

    public IRubyObject initialize_m(IRubyObject arg) {
        return initialize_m19(arg);
    }
    
    public IRubyObject initialize_m(IRubyObject arg0, IRubyObject arg1) {
        return initialize_m19(arg0, arg1);
    }
    
    public IRubyObject initialize_m(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return initialize_m19(arg0, arg1, arg2);
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_m19(IRubyObject arg) {
        if (arg instanceof RubyRegexp) return initializeByRegexp19((RubyRegexp)arg);
        return initializeCommon19(arg.convertToString(), new RegexpOptions());
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_m19(IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubyRegexp && Options.PARSER_WARN_FLAGS_IGNORED.load()) {
            getRuntime().getWarnings().warn(ID.REGEXP_IGNORED_FLAGS, "flags ignored");
            return initializeByRegexp19((RubyRegexp)arg0);
        }
        
        return initializeCommon19(arg0.convertToString(), 
                RegexpOptions.fromJoniOptions(objectAsJoniOptions(arg1)));
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_m19(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (arg0 instanceof RubyRegexp && Options.PARSER_WARN_FLAGS_IGNORED.load()) {
            getRuntime().getWarnings().warn(ID.REGEXP_IGNORED_FLAGS, "flags ignored");
            return initializeByRegexp19((RubyRegexp)arg0);
        }

        RegexpOptions newOptions = RegexpOptions.fromJoniOptions(objectAsJoniOptions(arg1));

        if (!arg2.isNil()) {
            ByteList kcodeBytes = arg2.convertToString().getByteList();
            if ((kcodeBytes.getRealSize() > 0 && kcodeBytes.getUnsafeBytes()[kcodeBytes.getBegin()] == 'n') ||
                (kcodeBytes.getRealSize() > 1 && kcodeBytes.getUnsafeBytes()[kcodeBytes.getBegin() + 1] == 'N')) {
                return initializeCommon19(arg0.convertToString().getByteList(), ASCIIEncoding.INSTANCE, newOptions);
            } else {
                getRuntime().getWarnings().warn("encoding option is ignored - " + kcodeBytes);
            }
        }
        return initializeCommon19(arg0.convertToString(), newOptions);
    }

    private IRubyObject initializeByRegexp19(RubyRegexp regexp) {
        regexp.check();
//        System.out.println("str: " + regexp.str + ", ENC: " + regexp.getEncoding() + ", OPT: " + regexp.getOptions());
//        System.out.println("KCODE: " + regexp.kcode);
        // Clone and toggle flags since this is no longer a literal regular expression
        // but it did come from one.
        RegexpOptions newOptions = (RegexpOptions) regexp.getOptions().clone();
        newOptions.setLiteral(false);
        return initializeCommon19(regexp.str, regexp.getEncoding(), newOptions);
    }

    // rb_reg_initialize_str
    private RubyRegexp initializeCommon19(RubyString str, RegexpOptions options) {
        if (isLiteral()) throw getRuntime().newSecurityError("can't modify literal regexp");
        ByteList bytes = str.getByteList();
        Encoding enc = bytes.getEncoding();
        if (options.isEncodingNone()) {
            if (enc != ASCIIEncoding.INSTANCE) {
                if (str.scanForCodeRange() != StringSupport.CR_7BIT) {
                    raiseRegexpError19(getRuntime(), bytes, enc, options, "/.../n has a non escaped non ASCII character in non ASCII-8BIT script");
                }
                enc = ASCIIEncoding.INSTANCE;
            }
        }
        return initializeCommon19(bytes, enc, options);
    }

    // rb_reg_initialize
    private RubyRegexp initializeCommon19(ByteList bytes, Encoding enc, RegexpOptions options) {
        Ruby runtime = getRuntime();
        this.options = options;

        checkFrozen();
        // FIXME: Something unsets this bit, but we aren't...be more permissive until we figure this out
        //if (isLiteral()) throw runtime.newSecurityError("can't modify literal regexp");
        if (pattern != null) throw runtime.newTypeError("already initialized regexp");
        if (enc.isDummy()) raiseRegexpError19(runtime, bytes, enc, options, "can't make regexp with dummy encoding");
        
        Encoding[]fixedEnc = new Encoding[]{null};
        ByteList unescaped = preprocess(runtime, bytes, enc, fixedEnc, ErrorMode.RAISE);
        if (fixedEnc[0] != null) {
            if ((fixedEnc[0] != enc && options.isFixed()) ||
               (fixedEnc[0] != ASCIIEncoding.INSTANCE && options.isEncodingNone())) {
                   raiseRegexpError19(runtime, bytes, enc, options, "incompatible character encoding");
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

        pattern = getRegexpFromCache(runtime, unescaped, enc, options);
        bytes.getClass();
        str = bytes;
        return this;
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        check();
        int hash = pattern.getOptions();
        int len = str.getRealSize();
        int p = str.getBegin();
        byte[]bytes = str.getUnsafeBytes();
        while (len-- > 0) {
            hash = hash * 33 + bytes[p++];
        }
        return getRuntime().newFixnum(hash + (hash >> 5));
    }

    @JRubyMethod(name = {"==", "eql?"}, required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) {
            return context.runtime.getTrue();
        }
        if (!(other instanceof RubyRegexp)) {
            return context.runtime.getFalse();
        }
        RubyRegexp otherRegex = (RubyRegexp)other;
        
        check();
        otherRegex.check();

        return context.runtime.newBoolean(str.equal(otherRegex.str) &&
                getOptions().equals(otherRegex.options));
    }

    public IRubyObject op_match2(ThreadContext context) {
        return op_match2_19(context);
    }

    @JRubyMethod(name = "~", reads = {LASTLINE, BACKREF}, writes = BACKREF)
    public IRubyObject op_match2_19(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject line = context.getLastLine();
        if (line instanceof RubyString) {
            IRubyObject[] holder = {context.nil};
            int start = search19(context, (RubyString)line, 0, false, holder);
            context.setBackRef(holder[0]);
            if (start < 0) return runtime.getNil();
            return runtime.newFixnum(start);
        }
        context.setBackRef(runtime.getNil());
        return runtime.getNil();
    }

    /** rb_reg_eqq
     * 
     */

    public IRubyObject eqq(ThreadContext context, IRubyObject arg) {
        return eqq19(context, arg);
    }

    @JRubyMethod(name = "===", required = 1, writes = BACKREF)
    public IRubyObject eqq19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        arg = operandNoCheck(arg);
        if (arg.isNil()) {
            context.setBackRef(arg);
            return runtime.getFalse();
        }
        IRubyObject[] holder = {context.nil};
        int start = search19(context, (RubyString)arg, 0, false, holder);
        context.setBackRef(holder[0]);
        return (start < 0) ? runtime.getFalse() : runtime.getTrue();
    }
    
    /** rb_reg_match
     * 
     */
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject str) {
        return op_match19(context, str);
    }
    
    @JRubyMethod(name = "=~", required = 1, writes = BACKREF)
    @Override
    public IRubyObject op_match19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        if (arg.isNil()) {
            context.setBackRef(context.nil);
            return arg;
        }
        RubyString str = operandCheck(runtime, arg);
        IRubyObject[] holder = {context.nil};
        int pos = matchPos(context, str, 0, holder);
        context.setBackRef(holder[0]);
        if (pos < 0) return runtime.getNil();
        return RubyFixnum.newFixnum(runtime, str.subLength(pos));
    }

    /** rb_reg_match_m
     * 
     */
    public IRubyObject match_m(ThreadContext context, IRubyObject str) {
        return match_m19(context, str, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "match", reads = BACKREF)
    public IRubyObject match_m19(ThreadContext context, IRubyObject str, Block block) {
        return match19Common(context, str, 0, true, block);
    }
    
    public IRubyObject match_m19(ThreadContext context, IRubyObject str, boolean useBackref, Block block) {
        return match19Common(context, str, 0, useBackref, block);
    }

    @JRubyMethod(name = "match", reads = BACKREF)
    public IRubyObject match_m19(ThreadContext context, IRubyObject str, IRubyObject pos, Block block) {
        return match19Common(context, str, RubyNumeric.num2int(pos), true, block);
    }

    private IRubyObject match19Common(ThreadContext context, IRubyObject arg, int pos, boolean setBackref, Block block) {
        if (arg.isNil()) {
            if (setBackref) context.setBackRef(arg);
            return arg;
        }
        Ruby runtime = context.runtime;
        RubyString _str = operandCheck(runtime, arg);
        
        IRubyObject[] holder = {context.nil};
        if (matchPos(context, _str, pos, holder) < 0) {
            if (setBackref) context.setBackRef(runtime.getNil());
            return runtime.getNil();
        }

        IRubyObject backref = holder[0];
        if (setBackref) context.setBackRef(backref);
        if (block.isGiven()) return block.yield(context, backref);
        return backref;
    }

    private int matchPos(ThreadContext context, RubyString str, int pos, IRubyObject[] holder) {
        if (pos != 0) {
            if (pos < 0) {
                pos += str.strLength();
                if (pos < 0) return pos;
            }
            pos = StringSupport.offset(str, pos);
        }
        return search19(context, str, pos, false, holder);
    }

    /** rb_reg_search
     */
    public final int search(ThreadContext context, RubyString str, int pos, boolean reverse, IRubyObject[] holder) {
        check();
        ByteList value = str.getByteList();

        if (pos <= value.getRealSize() && pos >= 0) {
            int realSize = value.getRealSize();
            int begin = value.getBegin();
            Matcher matcher = pattern.matcher(value.getUnsafeBytes(), begin, begin + realSize);

            int result = matcherSearch(context.runtime, matcher, begin + pos, begin + (reverse ? 0 : realSize), Option.NONE);
            if (result >= 0) {
                RubyMatchData matchData = createMatchData(context, str, matcher, pattern);
                matchData.regexp = this;
                matchData.infectBy(this);
                if (holder != null) holder[0] = matchData;
                return result;
            }
        }

        if (holder != null) holder[0] = context.nil;
        return -1;
    }

    static final RubyMatchData createMatchData(ThreadContext context, RubyString str, Matcher matcher, Regex pattern) {
        Ruby runtime = context.runtime;
        final RubyMatchData match = new RubyMatchData(runtime);

        // FIXME: This is pretty gross; we should have a cleaner initialization
        // that doesn't depend on package-visible fields and ideally is atomic,
        // probably using an immutable structure we replace all at once.

        // The region must be cloned because a subsequent match will update the
        // region, resulting in the MatchData created here pointing at the
        // incorrect region (capture/group).
        Region region = matcher.getRegion(); // lazy, null when no groups defined
        match.regs = region == null ? null : region.clone();
        match.begin = matcher.getBegin();
        match.end = matcher.getEnd();
        match.pattern = pattern;
        match.str = (RubyString)str.strDup(runtime).freeze(context);

        match.infectBy(str);

        return match;
    }

    public final int search19(ThreadContext context, RubyString str, int pos, boolean reverse, IRubyObject[] holder) {
        check();
        ByteList value = str.getByteList();

        if (pos <= value.getRealSize() && pos >= 0) {
            int realSize = value.getRealSize();
            int begin = value.getBegin();
            Matcher matcher = preparePattern(str).matcher(value.getUnsafeBytes(), begin, begin + realSize);

            int result = matcherSearch(context.runtime, matcher, begin + pos, begin + (reverse ? 0 : realSize), Option.NONE);
            if (result >= 0) {
                RubyMatchData matchData = createMatchData19(context, str, matcher, pattern);
                matchData.charOffsetUpdated = false;
                matchData.regexp = this;
                matchData.infectBy(this);
                if (holder != null) holder[0] = matchData;
                return result;
            }
        }
        
        if (holder != null) holder[0] = context.nil;
        return -1;
    }

    static final RubyMatchData createMatchData19(ThreadContext context, RubyString str, Matcher matcher, Regex pattern) {
        RubyMatchData match = createMatchData(context, str, matcher, pattern);
        match.charOffsetUpdated = false;
        return match;
    }

    @JRubyMethod
    public IRubyObject options() {
        return getRuntime().newFixnum(getOptions().toOptions());
    }

    @JRubyMethod(name = "casefold?")
    public IRubyObject casefold_p(ThreadContext context) {
        check();
        return context.runtime.newBoolean(getOptions().isIgnorecase());
    }

    /** rb_reg_source
     * 
     */
    @JRubyMethod
    public IRubyObject source() {
        check();
        RubyString _str = RubyString.newStringShared(getRuntime(), this.str);
        if (isTaint()) _str.setTaint(true);
        return _str;
    }

    final int length() {
        return str.getRealSize();
    }

    /** rb_reg_inspect
     *
     */
    @Override
    public IRubyObject inspect() {
        return inspect19();
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect19() {
        if (pattern == null) return anyToString();
        return RubyString.newString(getRuntime(), regexpDescription19(getRuntime(), str, options, str.getEncoding()));
    }

    private final static int EMBEDDABLE = RE_OPTION_MULTILINE|RE_OPTION_IGNORECASE|RE_OPTION_EXTENDED;

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        check();

        Ruby runtime = getRuntime();
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
                        new Regex(bytes, ++p, p + (len -= 2), Option.DEFAULT, getEncoding(runtime, str), Syntax.DEFAULT);
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
            appendRegexpString19(runtime, result, bytes, p, len, getEncoding(runtime, str), null);
            
            result.append((byte)')');
            return RubyString.newString(getRuntime(), result, getEncoding()).infectBy(this);
        } while (true);
    }

    // rb_reg_expr_str
    private static void appendRegexpString(Ruby runtime, ByteList to, byte[]bytes, int start, int len, Encoding enc) {
        int p = start;
        int end = p + len;
        boolean needEscape = false;
        if (enc.isAsciiCompatible()) {
            while (p < end) {
                int c = bytes[p] & 0xff;
                if (c == '/' || (!enc.isPrint(c) && enc.length(bytes, p, end) == 1)) {
                    needEscape = true;
                    break;
                }
                p += enc.length(bytes, p, end);
            }
        } else {
            needEscape = true;
        }
        
        if (!needEscape) {
            to.append(bytes, start, len);
        } else {
            p = start; 
            while (p < end) {
                int c = bytes[p] & 0xff;
                if (c == '\\') {
                    int n = enc.length(bytes, p + 1, end) + 1;
                    to.append(bytes, p, n);
                    p += n;
                    continue;
                } else if (c == '/') {
                    to.append((byte)'\\');
                    to.append(bytes, p, 1);
                } else if (enc.length(bytes, p, end) != 1) {
                    to.append(bytes, p, enc.length(bytes, p, end));
                    p += enc.length(bytes, p, end);
                    continue;
                } else if (enc.isPrint(c)) {
                    to.append(bytes, p, 1);
                } else if (!enc.isSpace(c)) {
                    Sprintf.sprintf(runtime, to, "\\%03o", bytes[p] & 0377);
                } else {
                    to.append(bytes, p, 1);
                }
                p++;
            }
        }
    }

    private static void appendRegexpString19(Ruby runtime, ByteList to, byte[]bytes, int start, int len, Encoding enc, Encoding resEnc) {
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
    private static void appendOptions(ByteList to, RegexpOptions options) {
        if (options.isMultiline()) to.append((byte)'m');
        if (options.isIgnorecase()) to.append((byte)'i');
        if (options.isExtended()) to.append((byte)'x');
    }

    private static String[] NO_NAMES = new String[] {}; //TODO: Perhaps we have another empty string arr
    public String[] getNames() {
        int nameLength = pattern.numberOfNames();
        if (nameLength == 0) return NO_NAMES;

        String[] names = new String[nameLength];
        int j = 0;
        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();
            names[j++] = new String(e.name, e.nameP, e.nameEnd - e.nameP).intern();
        }

        return names;
    }

    /** rb_reg_names
     * 
     */
    @JRubyMethod
    public IRubyObject names(ThreadContext context) {
        check();
        if (pattern.numberOfNames() == 0) return getRuntime().newEmptyArray();

        RubyArray ary = context.runtime.newArray(pattern.numberOfNames());
        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();
            ary.append(RubyString.newStringShared(getRuntime(), e.name, e.nameP, e.nameEnd - e.nameP));
        }
        return ary;
    }

    /** rb_reg_named_captures
     * 
     */
    @JRubyMethod
    public IRubyObject named_captures(ThreadContext context) {
        check();
        RubyHash hash = RubyHash.newHash(getRuntime());
        if (pattern.numberOfNames() == 0) return hash;

        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();
            int[]backrefs = e.getBackRefs();
            RubyArray ary = getRuntime().newArray(backrefs.length);

            for (int backref : backrefs) ary.append(RubyFixnum.newFixnum(getRuntime(), backref));
            hash.fastASet(RubyString.newStringShared(getRuntime(), e.name, e.nameP, e.nameEnd - e.nameP).freeze(context), ary);
        }
        return hash;
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        Encoding enc = (pattern == null) ? str.getEncoding() : pattern.getEncoding();
        return context.runtime.getEncodingService().getEncoding(enc);
    }

    @JRubyMethod(name = "fixed_encoding?")
    public IRubyObject fixed_encoding_p(ThreadContext context) {
        return context.runtime.newBoolean(options.isFixed());
    }

    /** rb_reg_nth_match
    *
    */
    public static IRubyObject nth_match(int nth, IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;
        m.check();

        Ruby runtime = m.getRuntime();

        final int start, end;
        if (m.regs == null) {
            if (nth >= 1 || (nth < 0 && ++nth <= 0)) return runtime.getNil();
            start = m.begin;
            end = m.end;
        } else {
            if (nth >= m.regs.numRegs || (nth < 0 && (nth+=m.regs.numRegs) <= 0)) return runtime.getNil();
            start = m.regs.beg[nth];
            end = m.regs.end[nth];
        }

        if (start == -1) return runtime.getNil();

        RubyString str = m.str.makeShared19(runtime, m.str.getType(), start, end - start);
        str.infectBy(m);
        return str;
    }

    /** rb_reg_last_match
     *
     */
    public static IRubyObject last_match(IRubyObject match) {
        return nth_match(0, match);
    }

    /** rb_reg_match_pre
     *
     */
    public static IRubyObject match_pre(IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;
        m.check();

        Ruby runtime = m.getRuntime();
        if (m.begin == -1) runtime.getNil(); 
        return m.str.makeShared19(runtime, m.str.getType(), 0,  m.begin).infectBy(m);
    }

    /** rb_reg_match_post
     *
     */
    public static IRubyObject match_post(IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;
        m.check();

        Ruby runtime = m.getRuntime();
        if (m.begin == -1) return runtime.getNil();
        return m.str.makeShared19(runtime, m.str.getType(), m.end, m.str.getByteList().getRealSize() - m.end).infectBy(m);
    }

    /** rb_reg_match_last
     *
     */
    public static IRubyObject match_last(IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;
        m.check();

        if (m.regs == null || m.regs.beg[0] == -1) return match.getRuntime().getNil();

        int i;
        for (i = m.regs.numRegs - 1; m.regs.beg[i] == -1 && i > 0; i--);
        if (i == 0) return match.getRuntime().getNil();
        
        return nth_match(i, match);
    }

    // MRI: ASCGET macro from rb_reg_regsub
    private static final int ASCGET(boolean acompat, byte[] sBytes, int s, int e, int[] cl, Encoding strEnc) {
        if (acompat) {
            cl[0] = 1;
            return Encoding.isAscii(sBytes[s]) ? sBytes[s] & 0xFF : -1;
        } else {
            return EncodingUtils.encAscget(sBytes, s, e, cl, strEnc);
        }
    }

    // rb_reg_regsub
    static RubyString regsub19(ThreadContext context, RubyString str, RubyString src, Matcher matcher, Regex pattern) {
        Ruby runtime = str.getRuntime();

        RubyString val = null;
        int p, s, e;
        int no = 0, clen[] = {0};
        Encoding strEnc = EncodingUtils.encGet(context, str);
        Encoding srcEnc = EncodingUtils.encGet(context, src);
        boolean acompat = EncodingUtils.encAsciicompat(strEnc);

        Region regs = matcher.getRegion();
        ByteList bs = str.getByteList();
        ByteList srcbs = src.getByteList();
        byte[] sBytes = bs.getUnsafeBytes();

        p = s = bs.getBegin();
        e = p + bs.getRealSize();

        while (s < e) {
            int c = ASCGET(acompat, sBytes, s, e, clen, strEnc);

            if (c == -1) {
                s += StringSupport.length(strEnc, sBytes, s, e);
                continue;
            }
            int ss = s;
            s += clen[0];

            if (c != '\\' || s == e) continue;

            if (val == null) {
                val = RubyString.newString(str.getRuntime(), new ByteList(ss - p));
            }
            EncodingUtils.encStrBufCat(runtime, val, sBytes, p, ss - p, strEnc);

            c = ASCGET(acompat, sBytes, s, e, clen, strEnc);

            if (c == -1) {
                s += StringSupport.length(strEnc, sBytes, s, e);
                EncodingUtils.encStrBufCat(runtime, val, sBytes, ss, s - ss, strEnc);
                p = s;
                continue;
            }
            s += clen[0];

            p = s;
            switch (c) {
            case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                if (pattern.noNameGroupIsActive(Syntax.RUBY)) {
                    no = c - '0';
                    break;
                } else {
                    continue;
                }
            case 'k':
                if (s < e && ASCGET(acompat, sBytes, s, e, clen, strEnc) == '<') {
                    int name = s + clen[0];
                    int nameEnd = name;
                    while (nameEnd < e) {
                        c = ASCGET(acompat, sBytes, nameEnd, e, clen, strEnc);
                        if (c == '>') break;
                        nameEnd += c == -1 ? StringSupport.length(strEnc, sBytes, nameEnd, e) : clen[0];
                    }
                    if (nameEnd < e) {
                        try {
                            no = pattern.nameToBackrefNumber(sBytes, name, nameEnd, regs);
                        } catch (JOniException je) {
                            throw str.getRuntime().newIndexError(je.getMessage());
                        }
                        p = s = nameEnd + clen[0];
                        break;
                    } else {
                        throw str.getRuntime().newRuntimeError("invalid group name reference format");
                    }
                }

                EncodingUtils.encStrBufCat(runtime, val, sBytes, ss, s - ss, strEnc);
                continue;
            case '0': case '&':
                no = 0;
                break;
            case '`':
                EncodingUtils.encStrBufCat(runtime, val, srcbs.getUnsafeBytes(), srcbs.getBegin(), matcher.getBegin(), srcEnc);
                continue;
            case '\'':
                EncodingUtils.encStrBufCat(runtime, val, srcbs.getUnsafeBytes(), srcbs.getBegin() + matcher.getEnd(), srcbs.getRealSize() - matcher.getEnd(), srcEnc);
                continue;
            case '+':
                if (regs != null) {
                    no = regs.numRegs - 1;
                    while (regs.beg[no] == -1 && no > 0) no--;
                }
                if (no == 0) continue;
                break;
            case '\\':
                EncodingUtils.encStrBufCat(runtime, val, sBytes, s - clen[0], clen[0], strEnc);
                continue;
            default:
                EncodingUtils.encStrBufCat(runtime, val, sBytes, ss, s - ss, strEnc);
                continue;
            }

            if (regs != null) {
                if (no >= 0) {
                    if (no >= regs.numRegs) continue;
                    if (regs.beg[no] == -1) continue;
                    EncodingUtils.encStrBufCat(runtime, val, srcbs.getUnsafeBytes(), srcbs.getBegin() + regs.beg[no], regs.end[no] - regs.beg[no], srcEnc);
                }
            } else {
                if (no != 0 || matcher.getBegin() == -1) continue;
                EncodingUtils.encStrBufCat(runtime, val, srcbs.getUnsafeBytes(), srcbs.getBegin() + matcher.getBegin(), matcher.getEnd() - matcher.getBegin(), srcEnc);
            }
        }

        if (val == null) return str;
        if (p < e) EncodingUtils.encStrBufCat(runtime, val, sBytes, p, e - p, strEnc);
        return val;
    }

    final int adjustStartPos19(RubyString str, int pos, boolean reverse) {
        return adjustStartPosInternal(str, checkEncoding(str, false), pos, reverse);
    }

    final int adjustStartPos(RubyString str, int pos, boolean reverse) {
        return adjustStartPosInternal(str, pattern.getEncoding(), pos, reverse);
    }

    private final int adjustStartPosInternal(RubyString str, Encoding enc, int pos, boolean reverse) {
        check();

        ByteList value = str.getByteList();
        int len = value.getRealSize();
        if (pos > 0 && enc.maxLength() != 1 && pos < len) {
            int start = value.getBegin();
            if ((reverse ? -pos : len - pos) > 0) {
                return enc.rightAdjustCharHead(value.getUnsafeBytes(), start, start + pos, start + len) - start;
            } else {
                return enc.leftAdjustCharHead(value.getUnsafeBytes(), start, start + pos, start + len) - start;
            }
        }

        return pos;
    }

    private static IRubyObject operandNoCheck(IRubyObject str) {
        if (str instanceof RubySymbol) return ((RubySymbol)str).to_s();
        return str.checkStringType();
    }

    private static RubyString operandCheck(Ruby runtime, IRubyObject str) {
        if (str instanceof RubySymbol) return (RubyString)((RubySymbol)str).to_s();
        IRubyObject tmp = str.checkStringType();
        if (tmp.isNil()) throw runtime.newTypeError("can't convert " + str.getMetaClass() + " into String");
        return (RubyString)tmp;
    }

    public static RubyRegexp unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyRegexp result = newRegexp(input.getRuntime(), input.unmarshalString(), RegexpOptions.fromJoniOptions(input.readSignedByte()));
        input.registerLinkTarget(result);
        return result;
    }

    public static void marshalTo(RubyRegexp regexp, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(regexp);
        output.writeString(regexp.str);
        
        int options = regexp.pattern.getOptions() & EMBEDDABLE;

        if (regexp.getOptions().isFixed()) options |= RE_FIXED;
        
        output.writeByte(options);
    }
}
