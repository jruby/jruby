/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;

import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.Syntax;
import org.joni.WarnCallback;
import org.jcodings.Encoding;
import org.joni.exception.JOniException;

import static org.jruby.anno.FrameField.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.parser.ReOptions;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.TypeConverter;

/**
 *
 */
@JRubyClass(name="Regexp")
public class RubyRegexp extends RubyObject implements ReOptions, WarnCallback {
    private KCode kcode;
    private Regex pattern;
    private ByteList str;

    private static final int REGEXP_LITERAL_F =     1 << 11;
    private static final int REGEXP_KCODE_DEFAULT = 1 << 12;

    public void setLiteral() {
        flags |= REGEXP_LITERAL_F;
    }
    
    public void clearLiteral() {
        flags &= ~REGEXP_LITERAL_F;
    }
    
    public boolean isLiteral() {
        return (flags & REGEXP_LITERAL_F) != 0;
    }

    public void setKCodeDefault() {
        flags |= REGEXP_KCODE_DEFAULT;
    }
    
    public void clearKCodeDefault() {
        flags &= ~REGEXP_KCODE_DEFAULT;
    }
    
    public boolean isKCodeDefault() {
        return (flags & REGEXP_KCODE_DEFAULT) != 0;
    }

    public KCode getKCode() {
        return kcode;
    }

    private static Map<ByteList, Regex> getPatternCache() {
        Map<ByteList, Regex> cache = patternCache.get();
        if (cache == null) {
            cache = new ConcurrentHashMap<ByteList, Regex>(5);
            patternCache = new SoftReference<Map<ByteList, Regex>>(cache);
        }
        return cache;
    }

    static volatile SoftReference<Map<ByteList, Regex>> patternCache = new SoftReference<Map<ByteList, Regex>>(null);    

    public static RubyClass createRegexpClass(Ruby runtime) {
        RubyClass regexpClass = runtime.defineClass("Regexp", runtime.getObject(), REGEXP_ALLOCATOR);
        runtime.setRegexp(regexpClass);
        regexpClass.index = ClassIndex.REGEXP;
        regexpClass.kindOf = new RubyModule.KindOf() {
            @Override
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyRegexp;
                }
            };
        
        regexpClass.defineConstant("IGNORECASE", runtime.newFixnum(RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", runtime.newFixnum(RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", runtime.newFixnum(RE_OPTION_MULTILINE));
        
        regexpClass.defineAnnotatedMethods(RubyRegexp.class);

        return regexpClass;
    }
    
    private static ObjectAllocator REGEXP_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyRegexp instance = new RubyRegexp(runtime, klass);
            return instance;
        }
    };

    /** used by allocator
     * 
     */
    private RubyRegexp(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    /** default constructor
     * 
     */
    private RubyRegexp(Ruby runtime) {
        super(runtime, runtime.getRegexp());
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newRegexp(Ruby runtime, String pattern, int options) {
        return newRegexp(runtime, ByteList.create(pattern), options);
    }

    // used only by the compiler/interpreter (will set the literal flag)
    public static RubyRegexp newRegexp(Ruby runtime, ByteList pattern, int options) {
        RubyRegexp regexp = newRegexp(runtime, pattern, options, false);
        regexp.setLiteral();
        return regexp;
    }
    
    public static RubyRegexp newRegexp(Ruby runtime, ByteList pattern, int options, boolean quote) {
        RubyRegexp regexp = new RubyRegexp(runtime);
        regexp.initialize(pattern, options, quote);
        return regexp;
    }

    // internal usage
    static RubyRegexp newRegexp(Ruby runtime, Regex regex) {
        RubyRegexp regexp = new RubyRegexp(runtime);
        regexp.pattern = regex;
        regexp.str = ByteList.EMPTY_BYTELIST;
        regexp.kcode = KCode.NONE;
        return regexp;
    }
    
    public void warn(String message) {
        getRuntime().getWarnings().warn(ID.MISCELLANEOUS, message);
    }

    @JRubyMethod(name = "kcode")
    public IRubyObject kcode(ThreadContext context) {
        return (!isKCodeDefault() && kcode != null) ? 
            context.getRuntime().newString(kcode.name()) : context.getRuntime().getNil();
    }

    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.REGEXP;
    }
    
    public Regex getPattern() {
        return pattern;
    }

    private void check() {
        if (pattern == null || str == null) throw getRuntime().newTypeError("uninitialized Regexp");
    }

    @JRubyMethod(name = "hash")
    @Override
    public RubyFixnum hash() {
        check();
        int hashval = (int)pattern.getOptions();
        int len = this.str.realSize;
        int p = this.str.begin;
        while (len-->0) {
            hashval = hashval * 33 + str.bytes[p++];
        }
        hashval = hashval + (hashval>>5);
        return getRuntime().newFixnum(hashval);
    }

    @JRubyMethod(name = {"==", "eql?"}, required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.getRuntime().getTrue();
        if (!(other instanceof RubyRegexp)) return context.getRuntime().getFalse();
        RubyRegexp otherRegex = (RubyRegexp)other;
        
        check();
        otherRegex.check();
        
        return context.getRuntime().newBoolean(str.equal(otherRegex.str) && 
                kcode == otherRegex.kcode && pattern.getOptions() == otherRegex.pattern.getOptions());
    }

    @JRubyMethod(name = "~", reads = {LASTLINE, BACKREF}, writes = BACKREF)
    public IRubyObject op_match2(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        IRubyObject line = context.getCurrentFrame().getLastLine();
        if (!(line instanceof RubyString)) {
            context.getCurrentFrame().setBackRef(runtime.getNil());
            return runtime.getNil();
        }
        int start = search(context, (RubyString)line, 0, false);
        if (start < 0) {
            return runtime.getNil();
        } else {
            return runtime.newFixnum(start);
        }
    }

    /** rb_reg_eqq
     * 
     */
    @JRubyMethod(name = "===", required = 1, writes = BACKREF)
    public IRubyObject eqq(ThreadContext context, IRubyObject str) {
        Ruby runtime = context.getRuntime();        
        if (!(str instanceof RubyString)) str = str.checkStringType();

        if (str.isNil()) {
            context.getCurrentFrame().setBackRef(runtime.getNil());
            return runtime.getFalse();
        }
        int start = search(context, (RubyString)str, 0, false);
        return (start < 0) ? runtime.getFalse() : runtime.getTrue();
    }

    private static final int REGEX_QUOTED = 1;
    private void initialize(ByteList bytes, int options, boolean quote) {
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) throw getRuntime().newSecurityError("Insecure: can't modify regexp");
        checkFrozen();
        if (isLiteral()) throw getRuntime().newSecurityError("can't modify literal regexp");

        setKCode(options);

        Map<ByteList, Regex> cache = getPatternCache();
        Regex pat = cache.get(bytes);

        if (pat != null &&
            pat.getEncoding() == kcode.getEncoding() &&
            pat.getOptions() == (options & 0xf) &&
            ((pat.getUserOptions() & REGEX_QUOTED) != 0) == quote) { // cache hit
            pattern = pat;
        } else {
            if (quote) bytes = quote(bytes, getRuntime().getKCode());
            makeRegexp(bytes, bytes.begin, bytes.realSize, options & 0xf, kcode.getEncoding());
            if (quote) pattern.setUserOptions(REGEX_QUOTED);
            cache.put(bytes, pattern);
        }

        str = bytes;
    }

    private void makeRegexp(ByteList bytes, int start, int len, int flags, Encoding enc) {
        try {
            pattern = new Regex(bytes.bytes, start, start + len, flags, enc, Syntax.DEFAULT, this);
        } catch(Exception e) {
            rb_reg_raise(bytes.bytes, start, len, e.getMessage(), flags);
        }
    }

    private final void rb_reg_raise(byte[] s, int start, int len, String err,int flags) {
        throw getRuntime().newRegexpError(err + ": " + rb_reg_desc(s,start, len,flags));
    }

    private final StringBuilder rb_reg_desc(byte[] s, int start, int len, int flags) {
        StringBuilder sb = new StringBuilder("/");
        rb_reg_expr_str(sb, s, start, len);
        sb.append("/");

        if ((flags & ReOptions.RE_OPTION_MULTILINE) != 0) sb.append("m");
        if ((flags & ReOptions.RE_OPTION_IGNORECASE) != 0) sb.append("i");
        if ((flags & ReOptions.RE_OPTION_EXTENDED) != 0) sb.append("x");

        if (kcode != null && !isKCodeDefault()) {
            sb.append(kcode.name().charAt(0));
        }
        return sb;
    }

    private final void rb_reg_expr_str(StringBuilder sb, byte[] s, int start, int len) {
        int p,pend;
        boolean need_escape = false;
        p = start;
        pend = start+len;
        Encoding enc = kcode.getEncoding();
        while (p < pend) {
            if (s[p] == '/' || 
                    (!(' ' == s[p] || (!Character.isWhitespace(s[p]) && 
                                       !Character.isISOControl(s[p]))) && 
                    enc.length(s, p, pend) == 1)) {
                need_escape = true;
                break;
            }
            p += enc.length(s, p, pend);
        }
        if (!need_escape) {
            sb.append(new ByteList(s,start,len,false).toString());
        } else {
            p = 0;
            while (p < pend) {
                if (s[p] == '\\') {
                    int n = enc.length(s, p + 1, pend) + 1;
                    sb.append(new ByteList(s,p,n,false).toString());
                    p += n;
                    continue;
                } else if (s[p] == '/') {
                    sb.append("\\/");
                } else if (enc.length(s, p, pend) != 1) {
                    sb.append(new ByteList(s, p, enc.length(s, p, pend), false).toString());
                    p += enc.length(s, p, pend);
                    continue;
                } else if ((' ' == s[p] || (!Character.isWhitespace(s[p]) && 
                                           !Character.isISOControl(s[p])))) {
                    sb.append((char)(s[p]&0xFF));
                } else if (!Character.isWhitespace((char)(s[p]&0xFF))) {
                    sb.append('\\');
                    sb.append(Integer.toString((int)(s[p]&0377),8));
                } else {
                    sb.append((char)(s[p]&0xFF));
                }
                p++;
            }
        }
    }    

    /** rb_reg_init_copy
     */
    @JRubyMethod(name = "initialize_copy", required = 1)
    @Override
    public IRubyObject initialize_copy(IRubyObject re) {
        if (this == re) return this;

        checkFrozen();

        if (getMetaClass().getRealClass() != re.getMetaClass().getRealClass()) {
            throw getRuntime().newTypeError("wrong argument type");
        }

        RubyRegexp regexp = (RubyRegexp)re;
        regexp.check();

        initialize(regexp.str, regexp.getOptions(), false);

        return this;
    }

    /** rb_set_kcode
     */
    private int getKcode() {
        if (kcode == KCode.NONE) {
            return 16;
        } else if (kcode == KCode.EUC) {
            return 32;
        } else if (kcode == KCode.SJIS) {
            return 48;
        } else if (kcode == KCode.UTF8) {
            return 64;
        }
        return 0;
    }

    /**
     */
    private void setKCode(int options) {
        clearKCodeDefault();
        switch(options & ~0xf) {
        case 0:
        default:
            setKCodeDefault();
            kcode = getRuntime().getKCode();
            break;
        case 16:
            kcode = KCode.NONE;
            break;
        case 32:
            kcode = KCode.EUC;
            break;
        case 48:
            kcode = KCode.SJIS;
            break;
        case 64:
            kcode = KCode.UTF8;
            break;
        }        
    }

    /** rb_reg_options
     */
    private int getOptions() {
        check();
        int options = (int)(pattern.getOptions() & (RE_OPTION_IGNORECASE|RE_OPTION_MULTILINE|RE_OPTION_EXTENDED));
        if (!isKCodeDefault()) {
            options |= getKcode();
        }
        return options;
    }

    /** rb_reg_initialize_m
     */
    @JRubyMethod(name = "initialize", optional = 3, visibility = Visibility.PRIVATE)
    public IRubyObject initialize_m(IRubyObject[] args) {
        ByteList bytes;
        int regexFlags = 0;

        if (args[0] instanceof RubyRegexp) {
            if (args.length > 1) {
                getRuntime().getWarnings().warn(ID.REGEXP_IGNORED_FLAGS, "flags" + (args.length == 3 ? " and encoding" : "") + " ignored");
            }
            RubyRegexp regexp = (RubyRegexp)args[0];
            regexp.check();

            regexFlags = (int)regexp.pattern.getOptions() & 0xF;
            if (!regexp.isKCodeDefault() && regexp.kcode != null && regexp.kcode != KCode.NIL) {
                if (regexp.kcode == KCode.NONE) {
                    regexFlags |= 16;
                } else if (regexp.kcode == KCode.EUC) {
                    regexFlags |= 32;
                } else if (regexp.kcode == KCode.SJIS) {
                    regexFlags |= 48;
                } else if (regexp.kcode == KCode.UTF8) {
                    regexFlags |= 64;
                }                
            }
            bytes = regexp.str;
        } else {
            if (args.length >= 2) {
                if (args[1] instanceof RubyFixnum) {
                    regexFlags = RubyNumeric.fix2int(args[1]);
                } else if (args[1].isTrue()) {
                    regexFlags = RE_OPTION_IGNORECASE;
                }
            }
            if (args.length == 3 && !args[2].isNil()) {
                ByteList kcodeBytes = args[2].convertToString().getByteList();
                char first = kcodeBytes.length() > 0 ? kcodeBytes.charAt(0) : 0;
                regexFlags &= ~0x70;
                switch (first) {
                case 'n': case 'N':
                    regexFlags |= 16;
                    break;
                case 'e': case 'E':
                    regexFlags |= 32;
                    break;
                case 's': case 'S':
                    regexFlags |= 48;
                    break;
                case 'u': case 'U':
                    regexFlags |= 64;
                    break;
                default:
                    break;
                }
            }
            bytes = args[0].convertToString().getByteList();
        }
        initialize(bytes, regexFlags, false);

        return this;
    }

    @JRubyMethod(name = {"new", "compile"}, required = 1, optional = 2, meta = true)
    public static RubyRegexp newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyClass klass = (RubyClass)recv;
        
        RubyRegexp re = (RubyRegexp) klass.allocate();
        re.callInit(args, Block.NULL_BLOCK);
        
        return re;
    }

    @JRubyMethod(name = "options")
    public IRubyObject options() {
        return getRuntime().newFixnum(getOptions());
    }
    
    /** rb_reg_search
     */
    public int search(ThreadContext context, RubyString str, int pos, boolean reverse) {
        Ruby runtime = context.getRuntime();
        Frame frame = context.getCurrentRubyFrame();

        ByteList value = str.getByteList();
        if (pos > value.realSize || pos < 0) {
            frame.setBackRef(runtime.getNil());
            return -1;
        }

        return performSearch(reverse, pos, value, frame, runtime, context, str);
    }

    private int performSearch(boolean reverse, int pos, ByteList value, Frame frame, Ruby runtime, ThreadContext context, RubyString str) {
        check();

        int realSize = value.realSize;
        int begin = value.begin;
        int range = reverse ? -pos : realSize - pos;

        Matcher matcher = pattern.matcher(value.bytes, begin, begin + realSize);

        int result = matcher.search(begin + pos, begin + pos + range, Option.NONE);

        if (result < 0) {
            frame.setBackRef(runtime.getNil());
            return result;
        }

        updateBackRef(context, str, frame, matcher);

        return result;
    }
    
    final RubyMatchData updateBackRef(ThreadContext context, RubyString str, Frame frame, Matcher matcher) {
        Ruby runtime = context.getRuntime();
        IRubyObject backref = frame.getBackRef();
        final RubyMatchData match;
        if (backref == null || backref.isNil() || ((RubyMatchData)backref).used()) {
            match = new RubyMatchData(runtime);
        } else {
            match = (RubyMatchData)backref;
            match.setTaint(runtime.getSafeLevel() >= 3);
        }
        
        match.regs = matcher.getRegion(); // lazy, null when no groups defined
        match.begin = matcher.getBegin();
        match.end = matcher.getEnd();
        
        match.str = (RubyString)str.strDup(runtime).freeze(context);
        match.pattern = pattern;

        frame.setBackRef(match);
        
        match.infectBy(this);
        match.infectBy(str);
        return match;
    }

    /** rb_reg_match
     * 
     */
    @JRubyMethod(name = "=~", required = 1, reads = BACKREF, writes = BACKREF)
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject str) {
        int start;
        if (str.isNil()) {
            context.getCurrentFrame().setBackRef(context.getRuntime().getNil());
            return str;
        }
        
        start = search(context, str.convertToString(), 0, false);

        if (start < 0) return context.getRuntime().getNil();

        return RubyFixnum.newFixnum(context.getRuntime(), start);
    }

    /** rb_reg_match_m
     * 
     */
    @JRubyMethod(name = "match", required = 1, reads = BACKREF)
    public IRubyObject match_m(ThreadContext context, IRubyObject str) {
        if (op_match(context, str).isNil()) return context.getRuntime().getNil();

        IRubyObject result =  context.getCurrentFrame().getBackRef();
        if (result instanceof RubyMatchData) {
            ((RubyMatchData)result).use();
        }
        return result;
    }


    public RubyString regsub(RubyString str, RubyString src, Matcher matcher) {
        Region regs = matcher.getRegion();
        int mbeg = matcher.getBegin();
        int mend = matcher.getEnd();
        
        int p, s;
        p = s = 0;
        int no = -1;
        ByteList bs = str.getByteList();
        ByteList srcbs = src.getByteList();
        int e = bs.length();
        RubyString val = null;
        Encoding enc = kcode.getEncoding();

        int beg, end;
        while (s < e) {
            int ss = s;
            int c = bs.charAt(s++);
            int l;
            if ((l = enc.length(bs.bytes, bs.begin + s, bs.begin + bs.realSize)) != 1) {
                s += l - 1;
                continue;
            }
            if (c != '\\' || s == e) continue;
            if (val == null) val = RubyString.newString(getRuntime(), new ByteList(ss - p));

            val.cat(bs.bytes, bs.begin + p, ss - p);
            c = bs.charAt(s++);
            p = s;
            
            switch (c) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                no = c - '0';
                break;
            case '&':
                no = 0;
                break;
            case '`':
                beg = regs == null ? mbeg : regs.beg[0];
                val.cat(srcbs.bytes, srcbs.begin, beg);
                continue;

            case '\'':
                end = regs == null ? mend : regs.end[0];
                val.cat(srcbs.bytes, srcbs.begin + end, src.getByteList().realSize - end);
                continue;

            case '+':
                if (regs == null) {
                    if (mbeg == -1) {
                        no = 0;
                        continue;
                    }
                } else {
                    no = regs.numRegs-1;
                    while (regs.beg[no] == -1 && no > 0) no--;
                    if (no == 0) continue;
                }
                break;
            case '\\':
                val.cat(bs.bytes, s - 1, 1);
                continue;
            default:
                val.cat(bs.bytes, s - 2, 2);
                continue;
            }

            if (regs != null) {
                if (no >= 0) {
                    if (no >= regs.numRegs || regs.beg[no] == -1) continue;
                    val.cat(srcbs.bytes, srcbs.begin + regs.beg[no], regs.end[no] - regs.beg[no]);
                }
            } else {
                if (no != 0 || mbeg == -1) continue;
                val.cat(srcbs.bytes, srcbs.begin + mbeg, mend - mbeg);
            }
        }

        if (p < e) {
            if (val == null) {
                val = RubyString.newString(getRuntime(), bs.makeShared(p, e-p));
            } else {
                val.cat(bs.bytes, bs.begin + p, e - p);
            }
        }
        if (val == null) return str;

        return val;
    }

    final int adjustStartPos(RubyString str, int pos, boolean reverse) {
        check();
        ByteList value = str.getByteList();
        return pattern.adjustStartPosition(value.bytes, value.begin, value.realSize, pos, reverse);
    }

    @JRubyMethod(name = "casefold?")
    public IRubyObject casefold_p(ThreadContext context) {
        check();
        
        return context.getRuntime().newBoolean((pattern.getOptions() & RE_OPTION_IGNORECASE) != 0);
    }

    /** rb_reg_source
     * 
     */
    @JRubyMethod(name = "source")
    public IRubyObject source() {
        Ruby runtime = getRuntime();
        check();
        RubyString str = RubyString.newStringShared(runtime, this.str);
        if (isTaint()) {
            str.taint(runtime.getCurrentContext());
        }
        return str;
    }

    final int length() {
        return str.realSize;
    }

    /** rb_reg_inspect
     *
     */
    @JRubyMethod(name = "inspect")
    @Override
    public IRubyObject inspect() {
        check();
        return getRuntime().newString(ByteList.create(rb_reg_desc(str.bytes, str.begin, str.realSize, pattern.getOptions()).toString()));
    }

    private final static int EMBEDDABLE = RE_OPTION_MULTILINE|RE_OPTION_IGNORECASE|RE_OPTION_EXTENDED;

    @JRubyMethod(name = "to_s")
    @Override
    public IRubyObject to_s() {
        RubyString ss = getRuntime().newString("(?");
        check();

        int options = pattern.getOptions();
        int ptr = str.begin;
        int len = str.realSize;
        byte[] bytes = str.bytes;
        again: do {
            if (len >= 4 && bytes[ptr] == '(' && bytes[ptr + 1] == '?') {
                boolean err = true;
                ptr += 2;
                if ((len -= 2) > 0) {
                    do {
                        if (bytes[ptr] == 'm') {
                            options |= RE_OPTION_MULTILINE;
                        } else if (bytes[ptr] == 'i') {
                            options |= RE_OPTION_IGNORECASE;
                        } else if (bytes[ptr] == 'x') {
                            options |= RE_OPTION_EXTENDED;
                        } else {
                            break;
                        }
                        ptr++;
                    } while (--len > 0);
                }
                if (len > 1 && bytes[ptr] == '-') {
                    ++ptr;
                    --len;
                    do {
                        if (bytes[ptr] == 'm') {
                            options &= ~RE_OPTION_MULTILINE;
                        } else if (bytes[ptr] == 'i') {
                            options &= ~RE_OPTION_IGNORECASE;
                        } else if (bytes[ptr] == 'x') {
                            options &= ~RE_OPTION_EXTENDED;
                        } else {
                            break;
                        }
                        ptr++;
                    } while (--len > 0);
                }

                if (bytes[ptr] == ')') {
                    --len;
                    ++ptr;
                    continue again;
                }

                if (bytes[ptr] == ':' && bytes[ptr + len - 1] == ')') {
                    try {
                        new Regex(bytes, ++ptr, ptr + (len-=2) ,Option.DEFAULT, kcode.getEncoding(), Syntax.DEFAULT);
                        err = false;
                    } catch (JOniException e) {
                        err = true;
                    }
                }

                if (err) {
                    options = (int)pattern.getOptions();
                    ptr = str.begin;
                    len = str.realSize;
                }
            }
            if ((options & RE_OPTION_MULTILINE) !=0 ) ss.cat((byte)'m');
            if ((options & RE_OPTION_IGNORECASE) !=0 ) ss.cat((byte)'i');
            if ((options & RE_OPTION_EXTENDED) !=0 ) ss.cat((byte)'x');

            if ((options & EMBEDDABLE) != EMBEDDABLE) {
                ss.cat((byte)'-');
                if ((options & RE_OPTION_MULTILINE) == 0) ss.cat((byte)'m');
                if ((options & RE_OPTION_IGNORECASE) == 0) ss.cat((byte)'i');
                if ((options & RE_OPTION_EXTENDED) == 0) ss.cat((byte)'x');
            }
            ss.cat((byte)':');
            rb_reg_expr_str(ss, ptr, len);
            ss.cat((byte)')');
            ss.infectBy(this);
            return ss;
        } while (true);
    }

    private final void rb_reg_expr_str(RubyString ss, int s, int len) {
        int p = s;
        int pend = p + len;
        boolean need_escape = false;
        Encoding enc = kcode.getEncoding();
        while (p < pend) {
            if (str.bytes[p] == '/' || (!enc.isPrint(str.bytes[p] & 0xff) && enc.length(str.bytes, p, pend) == 1)) {
                need_escape = true;
                break;
            }
            p += enc.length(str.bytes, p, pend);
        }
        if (!need_escape) {
            ss.cat(str.bytes, s, len);
        } else {
            p = s; 
            while (p<pend) {
                if (str.bytes[p] == '\\') {
                    int n = enc.length(str.bytes, p + 1, pend) + 1;
                    ss.cat(str.bytes, p, n);
                    p += n;
                    continue;
                } else if (str.bytes[p] == '/') {
                    ss.cat((byte)'\\');
                    ss.cat(str.bytes, p, 1);
                } else if (enc.length(str.bytes, p, pend) != 1) {
                    ss.cat(str.bytes, p, enc.length(str.bytes, p, pend));
                    p += enc.length(str.bytes, p, pend);
                    continue;
                } else if (enc.isPrint(str.bytes[p] & 0xff)) {
                    ss.cat(str.bytes,p,1);
                } else if (!enc.isSpace(str.bytes[p] & 0xff)) {
                    ss.cat(ByteList.create(Integer.toString(str.bytes[p] & 0377, 8)));
                } else {
                    ss.cat(str.bytes, p, 1);
                }
                p++;
            }
        }
    }

    /** rb_reg_s_quote
     * 
     */
    @JRubyMethod(name = {"quote", "escape"}, required = 1, optional = 1, meta = true)
    public static RubyString quote(IRubyObject recv, IRubyObject[] args) {
        IRubyObject kcode = args.length == 2 ? args[1] : null;
        IRubyObject str = args[0];
        KCode code = recv.getRuntime().getKCode();

        if (kcode != null && !kcode.isNil()) {
            code = KCode.create(recv.getRuntime(), kcode.toString());
        }
        
        RubyString src = str.convertToString();
        RubyString dst = RubyString.newString(recv.getRuntime(), quote(src.getByteList(), code));
        dst.infectBy(src);
        return dst;
    }

    /** rb_reg_quote
     *
     */
    public static ByteList quote(ByteList str, KCode kcode) {
        ByteList bs = str;
        int tix = 0;
        int s = bs.begin;
        int send = s + bs.length();
        Encoding enc = kcode.getEncoding();
        meta_found: do {
            for(; s < send; s++) {
                int c = bs.bytes[s] & 0xff;
                int l;
                if ((l = enc.length(bs.bytes, s, send)) != 1) {
                    int n = l;
                    while (n-- > 0 && s < send) {
                        s++;
                    }
                    s--;
                    continue;
                }
                switch (c) {
                case '[': case ']': case '{': case '}':
                case '(': case ')': case '|': case '-':
                case '*': case '.': case '\\':
                case '?': case '+': case '^': case '$':
                case ' ': case '#':
                case '\t': case '\f': case '\n': case '\r':
                    break meta_found;
                }
            }
            return bs;
        } while (false);
        ByteList b1 = new ByteList(send*2);
        System.arraycopy(bs.bytes,bs.begin,b1.bytes,b1.begin,s-bs.begin);
        tix += (s-bs.begin);

        for(; s<send; s++) {
            int c = bs.bytes[s] & 0xff;
            int l;
            if ((l = enc.length(bs.bytes, s, send)) != 1) {
                int n = l;
                while (n-- > 0 && s < send) {
                    b1.bytes[tix++] = bs.bytes[s++];
                }
                s--;
                continue;
            }

            switch(c) {
            case '[': case ']': case '{': case '}':
            case '(': case ')': case '|': case '-':
            case '*': case '.': case '\\':
            case '?': case '+': case '^': case '$':
            case '#':
                b1.bytes[tix++] = '\\';
                break;
            case ' ':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = ' ';
                continue;
            case '\t':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 't';
                 continue;
            case '\n':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 'n';
                continue;
            case '\r':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 'r';
                continue;
            case '\f':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 'f';
                continue;
            }
            b1.bytes[tix++] = (byte)c;
        }
        b1.realSize = tix;
        return b1;
    }


    /** rb_reg_nth_match
     *
     */
    public static IRubyObject nth_match(int nth, IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;

        int start, end;
        
        if (m.regs == null) {
            if (nth >= 1) return match.getRuntime().getNil();
            if (nth < 0 && ++nth <= 0) return match.getRuntime().getNil();
            start = m.begin;
            end = m.end;
        } else {
            if (nth >= m.regs.numRegs) return match.getRuntime().getNil();
            if (nth < 0 && (nth+=m.regs.numRegs) <= 0) return match.getRuntime().getNil();
            start = m.regs.beg[nth];
            end = m.regs.end[nth];
        }
        
        if (start == -1) return match.getRuntime().getNil();

        RubyString str = m.str.makeShared(match.getRuntime(), start, end - start);
        str.infectBy(match);
        return str;
    }

    /** rb_reg_last_match
     *
     */
    public static IRubyObject last_match(IRubyObject match) {
        return nth_match(0, match);
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public static IRubyObject last_match_s(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return last_match_s(context, recv);
        case 1:
            return last_match_s(context, recv, args[0]);
        default:
            Arity.raiseArgumentError(recv.getRuntime(), args.length, 0, 1);
            return null; // not reached
        }
    }

    /** rb_reg_s_last_match / match_getter
    *
    */
    @JRubyMethod(name = "last_match", meta = true, reads = BACKREF)
    public static IRubyObject last_match_s(ThreadContext context, IRubyObject recv) {
        IRubyObject match = context.getCurrentFrame().getBackRef();
        if (match instanceof RubyMatchData) ((RubyMatchData)match).use();
        return match;
    }

    /** rb_reg_s_last_match
    *
    */
    @JRubyMethod(name = "last_match", meta = true, reads = BACKREF)
    public static IRubyObject last_match_s(ThreadContext context, IRubyObject recv, IRubyObject nth) {
        IRubyObject match = context.getCurrentFrame().getBackRef();
        if (match.isNil()) return match;
        return nth_match(((RubyMatchData)match).backrefNumber(nth), match);
    }

    /** rb_reg_match_pre
     *
     */
    public static IRubyObject match_pre(IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;
        
        int beg = m.regs == null ? m.begin : m.regs.beg[0];
        
        if (beg == -1) match.getRuntime().getNil(); 

        RubyString str = m.str.makeShared(match.getRuntime(), 0, beg);
        str.infectBy(match);
        return str;
    }

    /** rb_reg_match_post
     *
     */
    public static IRubyObject match_post(IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;

        int end;
        if (m.regs == null) {
            if (m.begin == -1) return match.getRuntime().getNil();
            end = m.end;
        } else {
            if (m.regs.beg[0] == -1) return match.getRuntime().getNil();
            end = m.regs.end[0];
        }
        
        RubyString str = m.str.makeShared(match.getRuntime(), end, m.str.getByteList().realSize - end);
        str.infectBy(match);
        return str;
    }

    /** rb_reg_match_last
     *
     */
    public static IRubyObject match_last(IRubyObject match) {
        if (match.isNil()) return match;
        RubyMatchData m = (RubyMatchData)match;

        if (m.regs == null || m.regs.beg[0] == -1) return match.getRuntime().getNil();

        int i;
        for (i=m.regs.numRegs-1; m.regs.beg[i]==-1 && i>0; i--);
        if (i == 0) return match.getRuntime().getNil();
        
        return nth_match(i,match);
    }

    /** rb_reg_s_union
     *
     */
    @JRubyMethod(name = "union", rest = true, meta = true)
    public static IRubyObject union(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            return newRegexp(recv.getRuntime(), ByteList.create("(?!)"), 0, false);
        } else if (args.length == 1) {
            IRubyObject v = TypeConverter.convertToTypeWithCheck(args[0], recv.getRuntime().getRegexp(), 0, "to_regexp");
            if (!v.isNil()) {
                return v;
            } else {
                // newInstance here
                return newRegexp(recv.getRuntime(), quote(recv,args).getByteList(), 0, false);
            }
        } else {
            KCode kcode = null;
            IRubyObject kcode_re = recv.getRuntime().getNil();
            RubyString source = recv.getRuntime().newString();
            IRubyObject[] _args = new IRubyObject[3];

            for (int i = 0; i < args.length; i++) {
                if (0 < i) {
                    source.cat((byte)'|');
                }
                IRubyObject v = TypeConverter.convertToTypeWithCheck(args[i], recv.getRuntime().getRegexp(), 0, "to_regexp");
                if (!v.isNil()) {
                    if (!((RubyRegexp)v).isKCodeDefault()) {
                        if (kcode == null) {
                            kcode_re = v;
                            kcode = ((RubyRegexp)v).kcode;
                        } else if (((RubyRegexp)v).kcode != kcode) {
                            IRubyObject str1 = kcode_re.inspect();
                            IRubyObject str2 = v.inspect();
                            throw recv.getRuntime().newArgumentError("mixed kcode " + str1 + " and " + str2);
                        }
                    }
                    v = ((RubyRegexp)v).to_s();
                } else {
                    v = quote(recv, new IRubyObject[]{args[i]});
                }
                source.append(v);
            }

            _args[0] = source;
            _args[1] = recv.getRuntime().getNil();
            if (kcode == null) {
                _args[2] = recv.getRuntime().getNil();
            } else if (kcode == KCode.NONE) {
                _args[2] = recv.getRuntime().newString("n");
            } else if (kcode == KCode.EUC) {
                _args[2] = recv.getRuntime().newString("e");
            } else if (kcode == KCode.SJIS) {
                _args[2] = recv.getRuntime().newString("s");
            } else if (kcode == KCode.UTF8) {
                _args[2] = recv.getRuntime().newString("u");
            }
            return recv.callMethod(context, "new", _args);
        }
    }

    /** rb_reg_names
     * 
     */
    @JRubyMethod(name = "names", compat = CompatVersion.RUBY1_9)
    public IRubyObject names() {
        if (pattern.numberOfNames() == 0) return getRuntime().newEmptyArray();

        RubyArray ary = getRuntime().newArray(pattern.numberOfNames());
        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();
            ary.append(RubyString.newStringShared(getRuntime(), e.name, e.nameP, e.nameEnd - e.nameP));
        }
        return ary;
    }

    /** rb_reg_named_captures
     * 
     */
    @JRubyMethod(name = "named_captures", compat = CompatVersion.RUBY1_9)
    public IRubyObject named_captures(ThreadContext context) {
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

    public static RubyRegexp unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyRegexp result = newRegexp(input.getRuntime(), input.unmarshalString(), input.unmarshalInt(), false);
        input.registerLinkTarget(result);
        return result;
    }

    public static void marshalTo(RubyRegexp regexp, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(regexp);
        output.writeString(new String(regexp.str.bytes,regexp.str.begin,regexp.str.realSize));
        output.writeInt(regexp.pattern.getOptions() & EMBEDDABLE);
    }
}
