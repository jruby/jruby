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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import jregex.Matcher;
import jregex.Pattern;
import jregex.REFlags;
import org.jruby.parser.ReOptions;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.Sprintf;

/**
 *
 * @author  amoore
 */
public class RubyRegexp extends RubyObject implements ReOptions {
    private static final RegexpTranslator REGEXP_TRANSLATOR = new RegexpTranslator();

    // \013 is a vertical tab. Java does not support the \v notation used by
    // Ruby.
    private static final Pattern SPECIAL_CHARS = new Pattern("([\\\t\\\n\\\f\\\r\\ \\#\\\013\\+\\-\\[\\]\\.\\?\\*\\(\\)\\{\\}\\|\\\\\\^\\$])");    

	/** Class which represents the multibyte character set code.
	 * (should be an enum in Java 5.0).
	 * 
	 * Warning: THIS IS NOT REALLY SUPPORTED BY JRUBY. 
	 */

    private ByteList source;
    private Pattern pattern;
    private KCode code;
    private int flags;
    private int options;

    KCode getCode() {
        return code;
    }

	// lastTarget and matcher currently only used by searchAgain
	private String lastTarget = null;
	private Matcher matcher = null;

    public RubyRegexp(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    private RubyRegexp(Ruby runtime) {
        super(runtime, runtime.getClass("Regexp"));
    }
    
    private static ObjectAllocator REGEXP_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyRegexp instance = new RubyRegexp(runtime, klass);
            
            return instance;
        }
    };

    public static RubyClass createRegexpClass(Ruby runtime) {
        RubyClass regexpClass = runtime.defineClass("Regexp", runtime.getObject(), REGEXP_ALLOCATOR);
        regexpClass.index = ClassIndex.REGEXP;
        
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyRegexp.class);
        
        regexpClass.defineConstant("IGNORECASE", runtime.newFixnum(RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", runtime.newFixnum(RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", runtime.newFixnum(RE_OPTION_MULTILINE));

        regexpClass.defineFastMethod("initialize", callbackFactory.getFastOptMethod("initialize"));
        regexpClass.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("initialize_copy",RubyKernel.IRUBY_OBJECT));        
        regexpClass.defineFastMethod("==", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("eql?", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("===", callbackFactory.getFastMethod("eqq", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("=~", callbackFactory.getFastMethod("match", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("~", callbackFactory.getFastMethod("match2"));
        regexpClass.defineFastMethod("match", callbackFactory.getFastMethod("match_m", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        regexpClass.defineFastMethod("source", callbackFactory.getFastMethod("source"));
        regexpClass.defineFastMethod("casefold?", callbackFactory.getFastMethod("casefold"));
        regexpClass.defineFastMethod("kcode", callbackFactory.getFastMethod("kcode"));
        regexpClass.defineFastMethod("options", callbackFactory.getFastMethod("options"));
        regexpClass.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        regexpClass.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));

        regexpClass.getMetaClass().defineFastMethod("new", callbackFactory.getFastOptSingletonMethod("newInstance"));
        regexpClass.getMetaClass().defineFastMethod("compile", callbackFactory.getFastOptSingletonMethod("newInstance"));
        regexpClass.getMetaClass().defineFastMethod("quote", callbackFactory.getFastOptSingletonMethod("quote"));
        regexpClass.getMetaClass().defineFastMethod("escape", callbackFactory.getFastSingletonMethod("quote", RubyString.class));
        regexpClass.getMetaClass().defineFastMethod("last_match", callbackFactory.getFastOptSingletonMethod("last_match_s"));
        regexpClass.getMetaClass().defineFastMethod("union", callbackFactory.getFastOptSingletonMethod("union"));
        
        regexpClass.dispatcher = callbackFactory.createDispatcher(regexpClass);

        return regexpClass;
    }
    
    public int getNativeTypeIndex() {
        return ClassIndex.REGEXP;
    }
    
    public void initialize(ByteList regex, int options) {
        try {
            source = regex;
            pattern = REGEXP_TRANSLATOR.translate(regex, options, code.flags());
            flags = REGEXP_TRANSLATOR.flagsFor(options, code.flags());
            this.options = options;
        } catch(jregex.PatternSyntaxException e) {
            //            System.err.println(regex);
            //            e.printStackTrace();
            throw getRuntime().newRegexpError(e.getMessage());
        }
    }

    public void initialize(String regex, int options) {
        try {
            source = ByteList.create(regex);
            pattern = REGEXP_TRANSLATOR.translate(regex, options, code.flags());
            flags = REGEXP_TRANSLATOR.flagsFor(options, code.flags());
            this.options = options;
        } catch(jregex.PatternSyntaxException e) {
            //            System.err.println(regex);
            //            e.printStackTrace();
            throw getRuntime().newRegexpError(e.getMessage());
        }
    }

    public static String escapeSpecialChars(String original) {
    	return SPECIAL_CHARS.replacer("\\\\$1").replace(original);
    }

    private void recompileIfNeeded() {
        checkInitialized();
    }

    private void checkInitialized() {
        if (pattern == null) {
            throw getRuntime().newTypeError("uninitialized Regexp");
        }
    }
    
    public static RubyRegexp regexpValue(IRubyObject obj) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp) obj;
        } else if (obj instanceof RubyString) {
            return newRegexp(obj.getRuntime().newString(escapeSpecialChars(((RubyString) obj).toString())), 0, null);
        } else {
            throw obj.getRuntime().newArgumentError("can't convert arg to Regexp");
        }
    }

    // Methods of the Regexp class (rb_reg_*):

    public static RubyRegexp newRegexp(RubyString str, int options, String lang) {
        return newRegexp(str.getRuntime(), str.getByteList(), options, lang);
    }
    
    public static RubyRegexp newRegexp(Ruby runtime, String source, Pattern pattern, int flags, String lang) {
        RubyRegexp re = new RubyRegexp(runtime);
        re.code = KCode.create(runtime, lang);
        re.source = ByteList.create(source);
        re.pattern = pattern;
        re.flags = flags;
        return re;
    }
    
    public static RubyRegexp newRegexp(Ruby runtime, ByteList source, Pattern pattern, int flags, String lang) {
        RubyRegexp re = new RubyRegexp(runtime);
        re.code = KCode.create(runtime, lang);
        re.source = source;
        re.pattern = pattern;
        re.flags = flags;
        return re;
    }
    
    public static RubyRegexp newRegexp(Ruby runtime, String str, int options, String kcode) {
        RubyRegexp re = new RubyRegexp(runtime);
        re.code = KCode.create(runtime, kcode);
        re.initialize(str, options);
        return re;
    }
    
    public static RubyRegexp newRegexp(Ruby runtime, ByteList str, int options, String kcode) {
        RubyRegexp re = new RubyRegexp(runtime);
        re.code = KCode.create(runtime, kcode);
        re.initialize(str, options);
        return re;
    }
    
    public static RubyRegexp newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyClass klass = (RubyClass)recv;
        
        RubyRegexp re = (RubyRegexp) klass.allocate();
        
        re.callInit(args, Block.NULL_BLOCK);
        
        return re;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        ByteList pat =
            (args[0] instanceof RubyRegexp)
                ? ((RubyRegexp) args[0]).source().getByteList()
                : RubyString.stringValue(args[0]).getByteList();
        int opts = 0;
        if (args.length > 1) {
            if (args[1] instanceof RubyFixnum) {
                opts = (int) ((RubyFixnum) args[1]).getLongValue();
            } else if (args[1].isTrue()) {
                opts |= RE_OPTION_IGNORECASE;
            }
        }
        if (args.length > 2) {
        	code = KCode.create(getRuntime(), RubyString.stringValue (args[2]).toString());
        } else {
        	code = KCode.create(getRuntime(), null);
        }
        initialize(pat, opts);
        return getRuntime().getNil();
    }

    /** rb_reg_s_quote
     * 
     */
    public static RubyString quote(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0 || args.length > 2) {
            throw recv.getRuntime().newArgumentError(0, args.length);
        }

        KCode kcode = recv.getRuntime().getKCode();
        
        if (args.length > 1) {
            kcode = KCode.create(recv.getRuntime(), args[1].toString());
        }
        
        RubyString str = args[0].convertToString();

        if (kcode == KCode.NONE) {
            return quote(recv, str);
        }
        
        try {
            // decode with the specified encoding, escape as appropriate, and reencode
            // FIXME: This could probably be more efficent.
            ByteList bytes = str.getByteList();
            CharBuffer decoded = kcode.decoder().decode(ByteBuffer.wrap(bytes.unsafeBytes(), bytes.begin(), bytes.length()));
            String escaped = escapeSpecialChars(decoded.toString());
            ByteBuffer encoded = kcode.encoder().encode(CharBuffer.wrap(escaped));
            
            return (RubyString)RubyString.newString(recv.getRuntime(), encoded.array()).infectBy(str);
        } catch (CharacterCodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Utility version of quote that doesn't use encoding
     */
    public static RubyString quote(IRubyObject recv, RubyString str) {        
        return (RubyString) recv.getRuntime().newString(escapeSpecialChars(str.toString())).infectBy(str);
    }

    /** 
     * 
     */
    public static IRubyObject last_match_s(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            IRubyObject ret = recv.getRuntime().getCurrentContext().getBackref();
            if(ret instanceof RubyMatchData) {
                ((RubyMatchData)ret).use();
            }
            return ret;
        }
        
        // FIXME: 
        return ((RubyMatchData)recv.getRuntime().getCurrentContext().getBackref()).aref(args);
    }

    /** rb_reg_equal
     * 
     */
    public IRubyObject equal(IRubyObject other) {
        if (other == this) {
            return getRuntime().getTrue();
        }
        if (!(other instanceof RubyRegexp)) {
            return getRuntime().getFalse();
        }
        RubyRegexp re = (RubyRegexp) other;
        checkInitialized();
        if (!(re.pattern.toString().equals(pattern.toString()) && re.flags == this.flags)) {
            return getRuntime().getFalse();
        }
        
        if (code != re.code) {
        	return getRuntime().getFalse();
        }
        
        return getRuntime().getTrue();
    }

    /** rb_reg_match2
     * 
     */
    public IRubyObject match2() {
        IRubyObject target = getRuntime().getCurrentContext().getLastline();
        
        return target instanceof RubyString ? match(target) : getRuntime().getNil();
    }
    
    /** rb_reg_eqq
     * 
     */
    public IRubyObject eqq(IRubyObject target) {
        if(!(target instanceof RubyString)) {
            target = target.checkStringType();
            if(target.isNil()) {
                getRuntime().getCurrentContext().setBackref(getRuntime().getNil());
                return getRuntime().getFalse();
            }
        }
        RubyString ss = RubyString.stringValue(target);
    	String string = ss.toString();
        if (string.length() == 0 && "^$".equals(pattern.toString())) {
    		string = "\n";
        }
    	
        int result = search(string, ss, 0);
        
        return result < 0 ? getRuntime().getFalse() : getRuntime().getTrue();
    }

    /** rb_reg_match
     * 
     */
    public IRubyObject match(IRubyObject target) {
        if (target.isNil()) {
            return getRuntime().getFalse();
        }
        // FIXME:  I think all String expecting functions has this magic via RSTRING
    	if (target instanceof RubySymbol || target instanceof RubyHash || target instanceof RubyArray) {
    		return getRuntime().getFalse();
    	}
        // FIXME: make Unicode-aware
    	RubyString ss = RubyString.stringValue(target);
    	String string = ss.toString();
        if (string.length() == 0 && "^$".equals(pattern.toString())) {
    		string = "\n";
        }
    	
        int result = search(string, ss, 0);
        
        return result < 0 ? getRuntime().getNil() :
        	getRuntime().newFixnum(result);
    }

    /** rb_reg_match_m
     * 
     */
    public IRubyObject match_m(IRubyObject target) {
        if (target.isNil()) return target;

        IRubyObject result = match(target);
        
        if(result.isNil()) {
            return result;
        }

        result = getRuntime().getCurrentContext().getBackref();
        ((RubyMatchData)result).use();

        return result;
    }

    /** rb_reg_source
     * 
     */
    public RubyString source() {
        checkInitialized();
        return getRuntime().newString(source);
    }

    public IRubyObject kcode() {
        if(code == KCode.NIL) {
            return code.kcode(getRuntime());
        } else {
            return getRuntime().newString(code.kcode(getRuntime()).toString().toLowerCase());
        }
    }

    public IRubyObject options() {
        if((flags & REFlags.IGNORE_CASE) != 0) {
            this.options |= 1;
        }
        if((flags & REFlags.IGNORE_SPACES) != 0) {
            this.options |= 2;
        }
        if((flags & REFlags.DOTALL) != 0) {
            this.options |= 4;
        }
         return getRuntime().newFixnum(options + code.bits());
    }

    /** rb_reg_casefold_p
     * 
     */
    public RubyBoolean casefold() {
        checkInitialized();
        return getRuntime().newBoolean((flags & REFlags.IGNORE_CASE) != 0);
    }

    /** rb_reg_nth_match
     *
     */
    public static IRubyObject nth_match(int n, IRubyObject match) {
        IRubyObject nil = match.getRuntime().getNil();
        if (match.isNil()) {
            return nil;
        }
        
        RubyMatchData rmd = (RubyMatchData) match;
        
        if (n > rmd.getSize()) {
            return nil;
        }
        
        if (n < 0) {
            n += rmd.getSize();
            if (n <= 0) {
                return nil;
            }
        }
        return rmd.group(n);
    }

    /** rb_reg_last_match
     *
     */
    public static IRubyObject last_match(IRubyObject match) {
        if(match.isNil()) {
            return match;
        }
        ((RubyMatchData)match).use();
        return ((RubyMatchData) match).group(0);
    }

    /** rb_reg_match_pre
     *
     */
    public static IRubyObject match_pre(IRubyObject match) {
        if(match.isNil()) {
            return match;
        }
        ((RubyMatchData)match).use();
        return ((RubyMatchData) match).pre_match();
    }

    /** rb_reg_match_post
     *
     */
    public static IRubyObject match_post(IRubyObject match) {
        if(match.isNil()) {
            return match;
        }
        ((RubyMatchData)match).use();
        return ((RubyMatchData) match).post_match();
    }

    /** rb_reg_match_last
     *
     */
    public static IRubyObject match_last(IRubyObject match) {
        if (match.isNil()) {
            return match;
        }
        RubyMatchData md = (RubyMatchData) match;
        md.use();
        for (long i = md.getSize() - 1; i > 0; i--) {
            if (!md.group(i).isNil()) {
                return md.group(i);
            }
        }
        return md.getRuntime().getNil();
    }

    /** rb_reg_search
     *
     */
    public int search(String target, RubyString rtarget, int pos) {
        if (pos > target.length()) {
            return -1;
        }
        recompileIfNeeded();

        // If nothing match then nil will be returned
        IRubyObject result = match(target, rtarget, pos);
        getRuntime().getCurrentContext().setBackref(result);

        // If nothing match then -1 will be returned
        return result instanceof RubyMatchData ? ((RubyMatchData) result).matchStartPosition() : -1;
    }
    
    public IRubyObject search2(String str, RubyString rtarget) {
        IRubyObject result = match(str, rtarget, 0);
        
        getRuntime().getCurrentContext().setBackref(result);
        
    	return result;
    }
	
    public int searchAgain(String target, RubyString rtarget, boolean utf) {
        if (matcher == null || !target.equals(lastTarget)) {
            matcher = pattern.matcher(target);
            lastTarget = target;
        }
        
        if (!matcher.find()) {
            return -1;
        }
        
        IRubyObject _match = getRuntime().getCurrentContext().getBackref();
        RubyMatchData match;
        if(!_match.isNil() && !((match = (RubyMatchData)_match).used()) && (utf ^ (match instanceof RubyMatchData.RString))) {
            match.matcher = matcher;
            if(utf) {
                ((RubyMatchData.JavaString)match).original = target;
            } else {
                ((RubyMatchData.RString)match).original = rtarget;
                ((RubyMatchData.RString)match).set();
            }
        } else {
            match = (utf) ? 
                ((RubyMatchData)new RubyMatchData.JavaString(getRuntime(), target, matcher))
                :
                ((RubyMatchData)new RubyMatchData.RString(getRuntime(), rtarget, matcher));
            getRuntime().getCurrentContext().setBackref(match);
        }
        
        return match.matchStartPosition();
    }
    
    public IRubyObject match(String target, RubyString rtarget, int startPos) {
        boolean utf8 = getCode() == KCode.UTF8;
        String t = target;
        if(utf8) {
            try {
                byte[] bs = ByteList.plain(target);
                String string = new String(bs, 0, startPos, "UTF8");
                startPos = string.length();
                t = new String(ByteList.plain(target),"UTF8");
            } catch(Exception e) {
            }
        }

    	Matcher aMatcher = pattern.matcher(t);
    	
        aMatcher.setPosition(startPos);

        if (aMatcher.find()) {
            IRubyObject _match = getRuntime().getCurrentContext().getBackref();
            RubyMatchData match;
            if(!_match.isNil() && !((match = (RubyMatchData)_match).used()) && (utf8 ^ (match instanceof RubyMatchData.RString))) {
                match.matcher = aMatcher;
                if(utf8) {
                    ((RubyMatchData.JavaString)match).original = target;
                } else {
                    ((RubyMatchData.RString)match).original = rtarget;
                    ((RubyMatchData.RString)match).set();
                }
                return match;
            } else {
                return (utf8) ? 
                    ((RubyMatchData)new RubyMatchData.JavaString(getRuntime(), target, aMatcher))
                    :
                    ((RubyMatchData)new RubyMatchData.RString(getRuntime(), rtarget, aMatcher));
            }
        }
        return getRuntime().getNil();
    }

    /** rb_reg_regsub
    *
    */
    public RubyString regsub(IRubyObject str, RubyString src, RubyMatchData match) {
        ByteList val = null;

        ByteList strList = str.convertToString().getByteList();
        byte[]strBytes = strList.unsafeBytes();
        int p, s;
        p = s = strList.begin;
        int e = s + strList.realSize;

        while (s < e) {
            int ss = s;
            char c = (char)(strBytes[s++] & 0xFF);
            if (c != '\\' || s == e) continue;

            if (val == null) {
                val = new ByteList(ss - p);
                val.append(strBytes, p, ss - p);
            } else {
                val.append(strBytes, p, ss - p);
            }

            c = (char)(strBytes[s++] & 0xFF);
            p = s;

            int no;
            Matcher mat = match.matcher;

            ByteList srcList;
            byte[]srcBytes;

            switch (c) {
                case '0' :
                case '1' :
                case '2' :
                case '3' :
                case '4' :
                case '5' :
                case '6' :
                case '7' :
                case '8' :
                case '9' :
                    no = c - '0';
                    break;
                case '&' :
                    no = 0;
                    break;
                case '`' :
                    srcList = src.getByteList();
                    srcBytes = srcList.unsafeBytes();
                    val.append(srcBytes, srcList.begin, mat.start(0));
                    continue;
                case '\'':
                    srcList = src.getByteList();
                    srcBytes = srcList.unsafeBytes();
                    val.append(srcBytes, srcList.begin + mat.end(0), srcList.length() - mat.end(0));
                    continue;

                case '+' :
                    no = mat.groupCount() - 1;
                    while (!mat.isCaptured(no) && no > 0) no--;
                    if (no == 0) continue;
                    break;

                case '\\':
                    val.append(strBytes, s - 1, 1);
                    continue;

                default:
                    val.append(strBytes, s - 2, 2);
                    continue;
            }

            if (no >= 0) {
                if (no >= mat.groupCount()) continue;
                if (!mat.isCaptured(no)) continue;
                srcList = src.getByteList();
                srcBytes = srcList.unsafeBytes();
                val.append(srcBytes, srcList.begin + mat.start(no), mat.end(no) - mat.start(no));
            }
            
        }

        if (p < e) {
            if (val == null) {
                val = new ByteList(e - p);
                val.append(strBytes, p, e - p);
            } else {
                val.append(strBytes, p, e - p);
            }
        }

        if (val == null) return (RubyString)str;
        return RubyString.newString(getRuntime(), val);
    }

    /** rb_reg_init_copy
     * 
     */
    public IRubyObject initialize_copy(IRubyObject original) {
        if (this == original) return this;
        
        if (!(getMetaClass() == original.getMetaClass())){ // MRI also does a pointer comparison here
            throw getRuntime().newTypeError("wrong argument class");
        }

        RubyRegexp origRegexp = (RubyRegexp)original;
        source = origRegexp.source;
        pattern = origRegexp.pattern;
        code = origRegexp.code;

        return this;
    }

    /** rb_reg_inspect
     *
     */
    public IRubyObject inspect() {
        final ByteList regex = source;
        final int length = regex.length();
        StringBuffer sb = new StringBuffer(length + 2);

        sb.append('/');
        for (int i = 0; i < length; i++) {
            char c = regex.charAt(i);

            if (RubyString.isAlnum(c)) {
                sb.append(c);
            } else if (c == '/') {
                if (i == 0 || regex.charAt(i - 1) != '\\') {
                    sb.append("\\");
                }
            	sb.append(c);
            } else if (RubyString.isPrint(c)) {
                sb.append(c);
            } else if (c == '\n') {
                sb.append('\\').append('n');
            } else if (c == '\r') {
                sb.append('\\').append('r');
            } else if (c == '\t') {
                sb.append('\\').append('t');
            } else if (c == '\f') {
                sb.append('\\').append('f');
            } else if (c == '\u000B') {
                sb.append('\\').append('v');
            } else if (c == '\u0007') {
                sb.append('\\').append('a');
            } else if (c == '\u001B') {
                sb.append('\\').append('e');
            } else {
                sb.append(Sprintf.sprintf(getRuntime(),"\\%.3o",c));
            }
        }
        sb.append('/');

        if(code == KCode.NONE) {
            sb.append('n');
        } else if(code == KCode.UTF8) {
            sb.append('u');
        } else if(code == KCode.SJIS) {
            sb.append('s');
        }
        if ((flags & REFlags.IGNORE_CASE) > 0) {
            sb.append('i');
        }
  
        if ((flags & REFlags.DOTALL) > 0) {
            sb.append('m');
        }
        
        if ((flags & REFlags.IGNORE_SPACES) > 0) {
            sb.append('x');
        }

        return getRuntime().newString(sb.toString());
    }
    
    /**
     * rb_reg_s_union
     */
    public static IRubyObject union(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        if (args.length == 0) {
            return newInstance(recv, new IRubyObject[] {runtime.newString("(?!)")});
        }
        
        if (args.length == 1) {
            IRubyObject arg = args[0].convertToType(runtime.getClass("Regexp"), 0, "to_regexp", false);
            if (!arg.isNil()) {
                return arg;
            }
            return newInstance(recv, new IRubyObject[] {quote(recv, args[0].convertToString())});
        }
        
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
        	if (i > 0) {
        		buffer.append("|");
            }
        	IRubyObject arg = args[i].convertToType(runtime.getClass("Regexp"), 0, "to_regexp", false);
            if (arg.isNil()) {
                arg = quote(recv, args[i].convertToString());
            }
            buffer.append(arg.toString());
        }
        
        return newInstance(recv, new IRubyObject[] {runtime.newString(buffer.toString())});
    }

    
    public IRubyObject to_s() {
        return getRuntime().newString(toString());
    }
    
    public String toString() {
    	StringBuffer buffer = new StringBuffer(100);
    	StringBuffer off = new StringBuffer(3);
    	
    	buffer.append("(?");
    	
    	flagToString(buffer, off, REFlags.DOTALL, 'm');
    	flagToString(buffer, off, REFlags.IGNORE_CASE, 'i');
    	flagToString(buffer, off, REFlags.IGNORE_SPACES, 'x');

		if (off.length() > 0) {
			buffer.append('-').append(off);
		}

    	buffer.append(':');
        buffer.append(pattern.toString().replaceAll("^/|([^\\\\])/", "$1\\\\/"));
		buffer.append(')');

    	return buffer.toString();
    }

    /** Helper method for the {@link #toString() toString} method which creates
     * an <i>on-off</i> pattern of {@link Pattern Pattern} flags. 
     * 
	 * @param buffer the default buffer for the output
	 * @param off temporary buffer for the off flags
	 * @param flag a Pattern flag
	 * @param c the char which represents the flag
	 */
	private void flagToString(StringBuffer buffer, StringBuffer off, int flag, char c) {
		if ((flags & flag) != 0) {
    		buffer.append(c);
    	} else {
    		off.append(c);
    	}
	}

    public static RubyRegexp unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyRegexp result = newRegexp(input.getRuntime(), 
                                      input.unmarshalString(), input.unmarshalInt(), null);
        input.registerLinkTarget(result);
        return result;
    }

    public static void marshalTo(RubyRegexp regexp, MarshalStream output) throws java.io.IOException {
        output.writeString(regexp.pattern.toString());

        int _flags = 0;
        if ((regexp.flags & REFlags.DOTALL) > 0) {
            _flags |= RE_OPTION_MULTILINE;
        }
        if ((regexp.flags & REFlags.IGNORE_CASE) > 0) {
            _flags |= RE_OPTION_IGNORECASE;
        }
        if ((regexp.flags & REFlags.IGNORE_SPACES) > 0) {
            _flags |= RE_OPTION_EXTENDED;
        }
        output.writeInt(_flags);
    }
	
	public Pattern getPattern() {
		return this.pattern;
	}

    public RubyFixnum hash() {
        return getRuntime().newFixnum(this.pattern.toString().hashCode() + flags);
    }
}
