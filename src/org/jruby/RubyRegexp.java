/*
 * RubyRegexp.java - No description
 * Created on 26. Juli 2001, 00:01
 * 
 * Copyright (C) 2001, 2002, 2004 Jan Arne Petersen, Alan Moore, Benoit Cerrina, David Corbin
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * David Corbin <dcorbin@users.sourceforge.net>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.TypeError;
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
	/** Class which represents the multibyte character set code.
	 * (should be an enum in Java 5.0).
	 * 
	 * Warning: THIS IS NOT REALLY SUPPORTED BY JRUBY. 
	 */
	private static final class Code {
		private static final Code NIL = new Code(null);
		private static final Code NONE = new Code("none");
		private static final Code UTF8 = new Code("utf8");
		private static final Code SJIS = new Code("sjis");

		private String kcode;

		private Code(String kcode) {
			this.kcode = kcode;
		}

		public static Code create(Ruby runtime, String lang) {
			if (lang == null) {
				return NIL;
			} else if (lang.charAt(0) == 'n' || lang.charAt(0) == 'N') {
				return NONE;
			} else if (lang.charAt(0) == 'u' || lang.charAt(0) == 'U') {
				return UTF8;
			} else if (lang.charAt(0) == 's' || lang.charAt(0) == 'S') {
				runtime.getErrorHandler().warn("JRuby supports only Unicode regexp.");
				return SJIS;
			}
			return NIL;
		}

		public IRubyObject kcode(Ruby runtime) {
			if (kcode == null) {
				return runtime.getNil();
			}
			return RubyString.newString(runtime, kcode);
		}

		public int flags() {
			if (this == UTF8) {
				return Pattern.UNICODE_CASE;
			}
			return 0;
		}
	}
	
    private Pattern pattern;
    private Code code;

    public RubyRegexp(Ruby runtime) {
        super(runtime, runtime.getClass("Regexp"));
    }

    public static RubyClass createRegexpClass(Ruby runtime) {
        RubyClass regexpClass = runtime.defineClass("Regexp", runtime.getClasses().getObjectClass());
        CallbackFactory callbackFactory = runtime.callbackFactory();
        
        regexpClass.defineConstant("IGNORECASE", RubyFixnum.newFixnum(runtime, RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", RubyFixnum.newFixnum(runtime, RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", RubyFixnum.newFixnum(runtime, RE_OPTION_MULTILINE));

        regexpClass.defineMethod("initialize", callbackFactory.getOptMethod(RubyRegexp.class, "initialize"));
        regexpClass.defineMethod("clone", callbackFactory.getMethod(RubyRegexp.class, "rbClone"));
        regexpClass.defineMethod("==", callbackFactory.getMethod(RubyRegexp.class, "equal", IRubyObject.class));
        regexpClass.defineMethod("===", callbackFactory.getMethod(RubyRegexp.class, "match", IRubyObject.class));
        regexpClass.defineMethod("=~", callbackFactory.getMethod(RubyRegexp.class, "match", IRubyObject.class));
        regexpClass.defineMethod("~", callbackFactory.getMethod(RubyRegexp.class, "match2"));
        regexpClass.defineMethod("match", callbackFactory.getMethod(RubyRegexp.class, "match_m", IRubyObject.class));
        regexpClass.defineMethod("inspect", callbackFactory.getMethod(RubyRegexp.class, "inspect"));
        regexpClass.defineMethod("source", callbackFactory.getMethod(RubyRegexp.class, "source"));
        regexpClass.defineMethod("casefold?", callbackFactory.getMethod(RubyRegexp.class, "casefold"));
        regexpClass.defineMethod("kcode", callbackFactory.getMethod(RubyRegexp.class, "kcode"));
        regexpClass.defineMethod("to_s", callbackFactory.getMethod(RubyRegexp.class, "to_s"));

        regexpClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod(RubyRegexp.class, "newInstance"));
        regexpClass.defineSingletonMethod("compile", callbackFactory.getOptSingletonMethod(RubyRegexp.class, "newInstance"));
        regexpClass.defineSingletonMethod("quote", callbackFactory.getSingletonMethod(RubyRegexp.class, "quote", RubyString.class));
        regexpClass.defineSingletonMethod("escape", callbackFactory.getSingletonMethod(RubyRegexp.class, "quote", RubyString.class));
        regexpClass.defineSingletonMethod("last_match", callbackFactory.getSingletonMethod(RubyRegexp.class, "last_match_s"));

        return regexpClass;
    }

    public void initialize(String regex, int options) {
    	int flags = 0;
        if ((options & RE_OPTION_IGNORECASE) > 0) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        if ((options & RE_OPTION_EXTENDED) > 0) {
        	flags |= Pattern.COMMENTS;
        }
        if ((options & RE_OPTION_MULTILINE) > 0) {
        	flags |= Pattern.DOTALL;
        }
        pattern = Pattern.compile(regex, flags | this.code.flags());
    }

    public static String quote(String orig) {
        StringBuffer sb = new StringBuffer(orig.length() * 2);
        for (int i = 0; i < orig.length(); i++) {
            char c = orig.charAt(i);
            if ("[]{}()|-*.\\?+^$".indexOf(c) != -1) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void recompileIfNeeded() {
        checkInitialized();
    }

    private void checkInitialized() {
        if (pattern == null) {
            throw new TypeError(getRuntime(), "uninitialized Regexp");
        }
    }

    public static RubyRegexp regexpValue(IRubyObject obj) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp) obj;
        } else if (obj instanceof RubyString) {
            return newRegexp((RubyString) obj, 0, null);
        } else {
            throw new ArgumentError(obj.getRuntime(), "can't convert arg to Regexp");
        }
    }

    // Methods of the Regexp class (rb_reg_*):

    public static RubyRegexp newRegexp(RubyString str, int options, String lang) {
        return newRegexp(str.getRuntime(), str.getValue(), options, lang);
    }
    
    public static RubyRegexp newRegexp(Ruby runtime, String str, int options, String kcode) {
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
                ? ((RubyRegexp) args[0]).source().getValue()
                : RubyString.stringValue(args[0]).getValue();
        int opts = 0;
        if (args.length > 1) {
            if (args[1] instanceof RubyFixnum) {
                opts = (int) ((RubyFixnum) args[1]).getLongValue();
            } else if (args[1].isTrue()) {
                opts |= RE_OPTION_IGNORECASE;
            }
        }
        if (args.length > 2) {
        	code = Code.create(runtime, RubyString.stringValue (args[2]).getValue());
        } else {
        	code = Code.create(runtime, null);
        }

        initialize(pat, opts);
        return getRuntime().getNil();
    }

    /** rb_reg_s_quote
     * 
     */
    public static RubyString quote(IRubyObject recv, RubyString str) {
        return (RubyString) RubyString.newString(recv.getRuntime(), 
        		quote(str.toString())).infectBy(str);
    }

    /** 
     * 
     */
    public static IRubyObject last_match_s(IRubyObject recv) {
        return recv.getRuntime().getBackref();
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
        
        return runtime.getTrue();
    }

    /** rb_reg_match2
     * 
     */
    public IRubyObject match2() {
        IRubyObject target = getRuntime().getLastline();
        
        return target instanceof RubyString ? match(target) : getRuntime().getNil();
    }

    /** rb_reg_match
     * 
     */
    public IRubyObject match(IRubyObject target) {
        if (target.isNil()) {
            return getRuntime().getFalse();
        }
        int result = search(target, 0);
        
        return result < 0 ? getRuntime().getNil() :
        	RubyFixnum.newFixnum(getRuntime(), result);
    }

    /** rb_reg_match_m
     * 
     */
    public IRubyObject match_m(IRubyObject target) {
        if (target.isNil()) {
            return target;
        }
        IRubyObject result = match(target);
        return result.isNil() ? result : runtime.getBackref().rbClone();
    }

    /** rb_reg_source
     * 
     */
    public RubyString source() {
        checkInitialized();
        return RubyString.newString(getRuntime(), pattern.pattern());
    }

    public IRubyObject kcode() {
        return code.kcode(runtime);
    }

    /** rb_reg_casefold_p
     * 
     */
    public RubyBoolean casefold() {
        checkInitialized();
        return RubyBoolean.newBoolean(getRuntime(), (pattern.flags() & Pattern.CASE_INSENSITIVE) != 0);
    }

    /** rb_reg_nth_match
     *
     */
    public static IRubyObject nth_match(int n, IRubyObject match) {
        return match.isNil() ? match : ((RubyMatchData) match).group(n);
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
    public int search(IRubyObject target, int pos) {
        String str = RubyString.stringValue(target).getValue();
        if (pos > str.length()) {
            return -1;
        }
        recompileIfNeeded();

        // If nothing match then nil will be returned
        IRubyObject result = match(str, pos);
        getRuntime().getScope().setBackref(result);

        // If nothing match then -1 will be returned
        return result instanceof RubyMatchData ? ((RubyMatchData) result).matchStartPosition() : -1;
    }
    
    public IRubyObject search2(String str) {
    	return match(str, 0);
    }
    
    private IRubyObject match(String target, int startPos) {
    	Matcher matcher = pattern.matcher(target);
        if (matcher.find(startPos)) {
            int count = matcher.groupCount() + 1;
            int[] begin = new int[count];
            int[] end = new int[count];
            for (int i = 0; i < count; i++) {
                begin[i] = matcher.start(i);
                end[i] = matcher.end(i);
            }
            return new RubyMatchData(runtime, target, begin, end);
        }
        return runtime.getNil();
    }


    /** rb_reg_regsub
     *
     */
    public IRubyObject regsub(IRubyObject str, RubyMatchData match) {
        String repl = RubyString.stringValue(str).getValue();
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
        return RubyString.newString(getRuntime(), sb.toString());
    }

    // TODO: Could this be better hooked up to RubyObject#clone?
    public IRubyObject rbClone() {
    	RubyRegexp newObj = new RubyRegexp(runtime);
    	newObj.pattern = pattern;
    	newObj.code = code;
    	setupClone(newObj);
        return newObj;
    }

    /** rb_reg_inspect
     *
     */
    public RubyString inspect() {
        final String regex = pattern.pattern();
		final int length = regex.length();
        StringBuffer sb = new StringBuffer(length + 2);

        sb.append('/');
        for (int i = 0; i < length; i++) {
            char c = regex.charAt(i);

            if (RubyString.isAlnum(c)) {
                sb.append(c);
            } else if (c == '/') {
                sb.append('\\').append(c);
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

        return RubyString.newString(getRuntime(), sb.toString());
    }
    
    
    public RubyString to_s() {
      return new RubyString(getRuntime(), toString());
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
    	buffer.append(pattern.pattern());
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
        output.dumpInt(pattern.flags());
    }
}