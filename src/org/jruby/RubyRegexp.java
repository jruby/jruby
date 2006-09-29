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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.parser.ReOptions;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.util.PrintfFormat;

/**
 *
 * @author  amoore
 */
public class RubyRegexp extends RubyObject implements ReOptions {
    private static final RegexpTranslator REGEXP_TRANSLATOR = new RegexpTranslator();

    // \013 is a vertical tab. Java does not support the \v notation used by
    // Ruby.
    private static final Pattern SPECIAL_CHARS = Pattern.compile("([\\\t\\\n\\\f\\\r\\ \\#\\\013\\+\\[\\]\\.\\?\\*\\(\\)\\{\\}\\|\\\\\\^\\$])");    

	/** Class which represents the multibyte character set code.
	 * (should be an enum in Java 5.0).
	 * 
	 * Warning: THIS IS NOT REALLY SUPPORTED BY JRUBY. 
	 */

	public static final class Code {
		private static final Code NIL = new Code(null);
		private static final Code NONE = new Code("none");
		private static final Code UTF8 = new Code("utf8");
		private static final Code SJIS = new Code("sjis");

		private String kcode;

		private Code(String kcode) {
			this.kcode = kcode;
		}

		public static Code create(IRuby runtime, String lang) {
			if (lang == null) {
				return NIL;
			} else if (lang.charAt(0) == 'n' || lang.charAt(0) == 'N') {
				return NONE;
			} else if (lang.charAt(0) == 'u' || lang.charAt(0) == 'U') {
				return UTF8;
			} else if (lang.charAt(0) == 's' || lang.charAt(0) == 'S') {
				runtime.getWarnings().warn("JRuby supports only Unicode regexp.");
				return SJIS;
			}
			return NIL;
		}

		public IRubyObject kcode(IRuby runtime) {
			if (kcode == null) {
				return runtime.getNil();
			}
			return runtime.newString(kcode);
		}

		public int flags() {
            int flags = 0;
			if (this == UTF8) {
				flags |= Pattern.UNICODE_CASE;
			}
            flags |= Pattern.UNIX_LINES;
            
			return flags;
		}
	}
	
    private Pattern pattern;
    private Code code;
	
	// lastTarget and matcher currently only used by searchAgain
	private String lastTarget = null;
	private Matcher matcher = null;

    public RubyRegexp(IRuby runtime) {
        super(runtime, runtime.getClass("Regexp"));
    }

    public static RubyClass createRegexpClass(IRuby runtime) {
        RubyClass regexpClass = runtime.defineClass("Regexp", runtime.getObject());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyRegexp.class);
        
        regexpClass.defineConstant("IGNORECASE", runtime.newFixnum(RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", runtime.newFixnum(RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", runtime.newFixnum(RE_OPTION_MULTILINE));

        regexpClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        regexpClass.defineMethod("clone", callbackFactory.getMethod("rbClone"));
        regexpClass.defineMethod("==", callbackFactory.getMethod("equal", IRubyObject.class));
        regexpClass.defineMethod("===", callbackFactory.getMethod("match", IRubyObject.class));
        regexpClass.defineMethod("=~", callbackFactory.getMethod("match", IRubyObject.class));
        regexpClass.defineMethod("~", callbackFactory.getMethod("match2"));
        regexpClass.defineMethod("match", callbackFactory.getMethod("match_m", IRubyObject.class));
        regexpClass.defineMethod("inspect", callbackFactory.getMethod("inspect"));
        regexpClass.defineMethod("source", callbackFactory.getMethod("source"));
        regexpClass.defineMethod("casefold?", callbackFactory.getMethod("casefold"));
        regexpClass.defineMethod("kcode", callbackFactory.getMethod("kcode"));
        regexpClass.defineMethod("to_s", callbackFactory.getMethod("to_s"));

        regexpClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        regexpClass.defineSingletonMethod("compile", callbackFactory.getOptSingletonMethod("newInstance"));
        regexpClass.defineSingletonMethod("quote", callbackFactory.getSingletonMethod("quote", RubyString.class));
        regexpClass.defineSingletonMethod("escape", callbackFactory.getSingletonMethod("quote", RubyString.class));
        regexpClass.defineSingletonMethod("last_match", callbackFactory.getSingletonMethod("last_match_s"));
        regexpClass.defineSingletonMethod("union", callbackFactory.getOptSingletonMethod("union"));

        return regexpClass;
    }

    public void initialize(String regex, int options) {
        pattern = REGEXP_TRANSLATOR.translate(regex, options, code.flags());
    }

    public static String escapeSpecialChars(String original) {
    	return SPECIAL_CHARS.matcher(original).replaceAll("\\\\$1");
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
        return newRegexp(str.getRuntime(), str.toString(), options, lang);
    }
    
    public static RubyRegexp newRegexp(IRuby runtime, Pattern pattern, String lang) {
        RubyRegexp re = new RubyRegexp(runtime);
        re.code = Code.create(runtime, lang);
        re.pattern = pattern;
        return re;
    }
    
    public static RubyRegexp newRegexp(IRuby runtime, String str, int options, String kcode) {
        RubyRegexp re = new RubyRegexp(runtime);
        re.code = Code.create(runtime, kcode);
        re.initialize(str, options);
        return re;
    }
    
    public static RubyRegexp newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyRegexp re = new RubyRegexp(recv.getRuntime());
        re.setMetaClass((RubyClass) recv);
        re.initialize(args);
        return re;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        String pat =
            (args[0] instanceof RubyRegexp)
                ? ((RubyRegexp) args[0]).source().toString()
                : RubyString.stringValue(args[0]).toString();
        int opts = 0;
        if (args.length > 1) {
            if (args[1] instanceof RubyFixnum) {
                opts = (int) ((RubyFixnum) args[1]).getLongValue();
            } else if (args[1].isTrue()) {
                opts |= RE_OPTION_IGNORECASE;
            }
        }
        if (args.length > 2) {
        	code = Code.create(getRuntime(), RubyString.stringValue (args[2]).toString());
        } else {
        	code = Code.create(getRuntime(), null);
        }

        initialize(pat, opts);
        return getRuntime().getNil();
    }

    /** rb_reg_s_quote
     * 
     */
    public static RubyString quote(IRubyObject recv, RubyString str) {
        return (RubyString) recv.getRuntime().newString(escapeSpecialChars(str.toString())).infectBy(str);
    }

    /** 
     * 
     */
    public static IRubyObject last_match_s(IRubyObject recv) {
        return recv.getRuntime().getCurrentContext().getBackref();
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
        if (!(re.pattern.pattern().equals(pattern.pattern()) && 
        	  re.pattern.flags() == pattern.flags())) {
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
    	
    	String string = RubyString.stringValue(target).toString();
    	
    	if (string.length() == 0) {
    		string = "\n";
    	}
    	
        int result = search(string, 0);
        
        return result < 0 ? getRuntime().getNil() :
        	getRuntime().newFixnum(result);
    }

    /** rb_reg_match_m
     * 
     */
    public IRubyObject match_m(IRubyObject target) {
        if (target.isNil()) {
            return target;
        }
        IRubyObject result = match(target);
        return result.isNil() ? result : getRuntime().getCurrentContext().getBackref().rbClone();
    }

    /** rb_reg_source
     * 
     */
    public RubyString source() {
        checkInitialized();
        return getRuntime().newString(pattern.pattern());
    }

    public IRubyObject kcode() {
        return code.kcode(getRuntime());
    }

    /** rb_reg_casefold_p
     * 
     */
    public RubyBoolean casefold() {
        checkInitialized();
        return getRuntime().newBoolean((pattern.flags() & Pattern.CASE_INSENSITIVE) != 0);
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
        return match.isNil() ? match : ((RubyMatchData) match).group(0);
    }

    /** rb_reg_match_pre
     *
     */
    public static IRubyObject match_pre(IRubyObject match) {
        return match.isNil() ? match : ((RubyMatchData) match).pre_match();
    }

    /** rb_reg_match_post
     *
     */
    public static IRubyObject match_post(IRubyObject match) {
        return match.isNil() ? match : ((RubyMatchData) match).post_match();
    }

    /** rb_reg_match_last
     *
     */
    public static IRubyObject match_last(IRubyObject match) {
        if (match.isNil()) {
            return match;
        }
        RubyMatchData md = (RubyMatchData) match;
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
    public int search(String target, int pos) {
        if (pos > target.length()) {
            return -1;
        }
        recompileIfNeeded();

        // If nothing match then nil will be returned
        IRubyObject result = match(target, pos);
        getRuntime().getCurrentContext().setBackref(result);

        // If nothing match then -1 will be returned
        return result instanceof RubyMatchData ? ((RubyMatchData) result).matchStartPosition() : -1;
    }
    
    public IRubyObject search2(String str) {
        IRubyObject result = match(str, 0);
        
        getRuntime().getCurrentContext().setBackref(result);
        
    	return result;
    }
	
    public int searchAgain(String target) {
        if (matcher == null || !target.equals(lastTarget)) {
			matcher = pattern.matcher(target);
			lastTarget = target;
        }
			
	    if (!matcher.find()) {
			return -1;
        }
		
		int count = matcher.groupCount() + 1;
        int[] begin = new int[count];
        int[] end = new int[count];
        for (int i = 0; i < count; i++) {
            begin[i] = matcher.start(i);
            end[i] = matcher.end(i);
        }
		
		RubyMatchData match = new RubyMatchData(getRuntime(), target, begin, end);

		getRuntime().getCurrentContext().setBackref(match);
            
		return match.matchStartPosition(); 
    }
    
    public IRubyObject match(String target, int startPos) {
    	Matcher aMatcher = pattern.matcher(target);
    	
        if (aMatcher.find(startPos)) {
            int count = aMatcher.groupCount() + 1;
            int[] begin = new int[count];
            int[] end = new int[count];
            for (int i = 0; i < count; i++) {
                begin[i] = aMatcher.start(i);
                end[i] = aMatcher.end(i);
            }
            
            return new RubyMatchData(getRuntime(), target, begin, end);
        }
        return getRuntime().getNil();
    }


    /** rb_reg_regsub
     *
     */
    public IRubyObject regsub(IRubyObject str, RubyMatchData match) {
        String repl = RubyString.stringValue(str).toString();
        StringBuffer sb = new StringBuffer("");
        int pos = 0;
        int end = repl.length();
        char c;
        IRubyObject ins;
        while (pos < end) {
            c = repl.charAt(pos++);
            if (c == '\\' && pos < end) {
                c = repl.charAt(pos++);
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
                        ins = match.group(c - '0');
                        break;
                    case '&' :
                        ins = match.group(0);
                        break;
                    case '`' :
                        ins = match.pre_match();
                        break;
                    case '\'' :
                        ins = match.post_match();
                        break;
                    case '+' :
                        ins = match_last(match);
                        break;
                    case '\\' :
                        sb.append(c);
                        continue;
                    default :
                        sb.append('\\').append(c);
                        continue;
                }
                if (!ins.isNil()) {
                    sb.append(((RubyString) ins).getValue());
                }
            } else {
                sb.append(c);
            }
        }
        return getRuntime().newString(sb.toString());
    }

    // TODO: Could this be better hooked up to RubyObject#clone?
    public IRubyObject rbClone() {
    	RubyRegexp newObj = new RubyRegexp(getRuntime());
    	newObj.pattern = pattern;
    	newObj.code = code;
    	newObj.setTaint(isTaint());
    	newObj.initCopy(this);
    	newObj.setFrozen(isFrozen());
        return newObj;
    }

    /** rb_reg_inspect
     *
     */
    public IRubyObject inspect() {
        final String regex = pattern.pattern();
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
                sb.append(new PrintfFormat("\\%.3o").sprintf(c));
            }
        }
        sb.append('/');
  
        if ((pattern.flags() & Pattern.CASE_INSENSITIVE) > 0) {
            sb.append('i');
        }
  
        if ((pattern.flags() & Pattern.DOTALL) > 0) {
            sb.append('m');
        }
        
        if ((pattern.flags() & Pattern.COMMENTS) > 0) {
            sb.append('x');
        }

        return getRuntime().newString(sb.toString());
    }
    
    /**
     * rb_reg_s_union
     */
    public static IRubyObject union(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            return newInstance(recv, new IRubyObject[] {recv.getRuntime().newString("(?!)")});
        }
        
        if (args.length == 1) {
            IRubyObject arg = args[0].convertToType("Regexp", "to_regexp", false);
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
        	IRubyObject arg = args[i].convertToType("Regexp", "to_regexp", false);
            if (arg.isNil()) {
                arg = quote(recv, args[i].convertToString());
            }
            buffer.append(arg.toString());
        }
        
        return newInstance(recv, new IRubyObject[] {recv.getRuntime().newString(buffer.toString())});
    }

    
    public IRubyObject to_s() {
        return getRuntime().newString(toString());
    }
    
    public String toString() {
    	StringBuffer buffer = new StringBuffer(100);
    	StringBuffer off = new StringBuffer(3);
    	
    	buffer.append("(?");
    	
    	flagToString(buffer, off, Pattern.DOTALL, 'm');
    	flagToString(buffer, off, Pattern.CASE_INSENSITIVE, 'i');
    	flagToString(buffer, off, Pattern.COMMENTS, 'x');

		if (off.length() > 0) {
			buffer.append('-').append(off);
		}

    	buffer.append(':');
        buffer.append(pattern.pattern().replaceAll("^/|([^\\\\])/", "$1\\\\/"));
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
		if ((pattern.flags() & flag) != 0) {
    		buffer.append(c);
    	} else {
    		off.append(c);
    	}
	}

	public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('/');
        output.dumpString(pattern.pattern());

        int flags = 0;
        if ((pattern.flags() & Pattern.DOTALL) > 0) {
            flags |= RE_OPTION_MULTILINE;
        }
        if ((pattern.flags() & Pattern.CASE_INSENSITIVE) > 0) {
            flags |= RE_OPTION_IGNORECASE;
        }
        if ((pattern.flags() & Pattern.COMMENTS) > 0) {
            flags |= RE_OPTION_EXTENDED;
        }
        output.dumpInt(flags);
    }
	
	public Pattern getPattern() {
		return this.pattern;
	}
}
