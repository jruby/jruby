/*
 * RubyRegexp.java - No description
 * Created on 26. Juli 2001, 00:01
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

import org.jruby.exceptions.*;
import org.jruby.parser.ReOptions;
import org.jruby.regexp.*;
import org.jruby.runtime.*;
import org.jruby.util.PrintfFormat;
import org.jruby.marshal.MarshalStream;

/**
 *
 * @author  amoore
 * @version $Revision$
 */
public class RubyRegexp extends RubyObject implements ReOptions {
    private IRegexpAdapter matcher;
    private String pattern;
    private int options;

    public RubyRegexp(Ruby ruby) {
        super(ruby, ruby.getRubyClass("Regexp"));
        try {
            matcher = (IRegexpAdapter) ruby.getRegexpAdapterClass().newInstance();
        } catch (Exception ex) {
            // can't happen if JRuby is invoked via Main class
            throw new RubyBugException("Couldn't create regexp adapter");
        }
    }

    public static RubyClass createRegexpClass(Ruby ruby) {
        RubyClass regexpClass = ruby.defineClass("Regexp", ruby.getClasses().getObjectClass());

        regexpClass.defineConstant("IGNORECASE", RubyFixnum.newFixnum(ruby, RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", RubyFixnum.newFixnum(ruby, RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", RubyFixnum.newFixnum(ruby, RE_OPTION_MULTILINE));

        regexpClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyRegexp.class, "newInstance"));
        regexpClass.defineSingletonMethod("compile", CallbackFactory.getOptSingletonMethod(RubyRegexp.class, "newInstance"));
        regexpClass.defineSingletonMethod(
            "quote",
            CallbackFactory.getSingletonMethod(RubyRegexp.class, "quote", RubyString.class));
        regexpClass.defineSingletonMethod(
            "escape",
            CallbackFactory.getSingletonMethod(RubyRegexp.class, "quote", RubyString.class));
        regexpClass.defineSingletonMethod("last_match", CallbackFactory.getSingletonMethod(RubyRegexp.class, "last_match"));

        regexpClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyRegexp.class, "initialize"));
        regexpClass.defineMethod("clone", CallbackFactory.getMethod(RubyRegexp.class, "rbClone"));
        regexpClass.defineMethod("==", CallbackFactory.getMethod(RubyRegexp.class, "equal", RubyObject.class));
        regexpClass.defineMethod("===", CallbackFactory.getMethod(RubyRegexp.class, "match", RubyObject.class));
        regexpClass.defineMethod("=~", CallbackFactory.getMethod(RubyRegexp.class, "match", RubyObject.class));
        regexpClass.defineMethod("~", CallbackFactory.getMethod(RubyRegexp.class, "match2"));
        regexpClass.defineMethod("match", CallbackFactory.getMethod(RubyRegexp.class, "match_m", RubyObject.class));
        regexpClass.defineMethod("inspect", CallbackFactory.getMethod(RubyRegexp.class, "inspect"));
        regexpClass.defineMethod("source", CallbackFactory.getMethod(RubyRegexp.class, "source"));
        regexpClass.defineMethod("casefold?", CallbackFactory.getMethod(RubyRegexp.class, "casefold"));
        //        regexpClass.defineMethod("kcode", getMethod("m_kcode"));

        return regexpClass;
    }

    public void initialize(String pat, int opts) {
        pattern = pat;
        options = opts;
        if ((options & RE_OPTION_IGNORECASE) > 0 || getRuby().getGlobalVar("$=").isTrue()) {
            matcher.setCasefold(true);
        }
        if ((options & RE_OPTION_EXTENDED) > 0) {
            matcher.setExtended(true);
        }
        if ((options & RE_OPTION_MULTILINE) > 0) {
            matcher.setMultiline(true);
        }
        matcher.compile(getRuby(), pattern);
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
        boolean globalCF = getRuby().getGlobalVar("$=").isTrue();
        if ((globalCF && !matcher.getCasefold())
            || (matcher.getCasefold() && !globalCF && (options & RE_OPTION_IGNORECASE) == 0)) {
            initialize(pattern, options);
        }
    }

    private void checkInitialized() {
        if (matcher == null) {
            throw new TypeError(getRuby(), "uninitialized Regexp");
        }
    }

    public static RubyRegexp regexpValue(RubyObject obj) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp) obj;
        } else if (obj instanceof RubyString) {
            return newRegexp(obj.getRuby(), (RubyString) obj, 0);
        } else {
            throw new ArgumentError(obj.getRuby(), "can't convert arg to Regexp");
        }
    }

    // Methods of the Regexp class (rb_reg_*):

    public static RubyRegexp newRegexp(Ruby ruby, RubyString str, int options) {
        return newRegexp(ruby, str.getValue(), options);
    }

    public static RubyRegexp newRegexp(Ruby ruby, String str, int options) {
        RubyRegexp re = new RubyRegexp(ruby);
        re.initialize(str, options);
        return re;
    }

    public static RubyRegexp newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyRegexp re = new RubyRegexp(ruby);
        re.setRubyClass((RubyClass) recv);
        re.initialize(args);
        return re;
    }

    public RubyObject initialize(RubyObject[] args) {
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

        initialize(pat, opts);
        return getRuby().getNil();
    }

    /** rb_reg_s_quote
     * 
     */
    public static RubyString quote(Ruby ruby, RubyObject recv, RubyString str) {
        String orig = str.getValue();
        RubyString newStr = RubyString.newString(ruby, quote(orig));
        newStr.infectObject(str);
        return newStr;
    }

    /** 
     * 
     */
    public static RubyObject last_match(Ruby ruby, RubyObject recv) {
        return ruby.getBackref();
    }

    /** rb_reg_equal
     * 
     */
    public RubyBoolean equal(RubyObject other) {
        if (other == this) {
            return getRuby().getTrue();
        }
        if (!(other instanceof RubyRegexp)) {
            return getRuby().getFalse();
        }
        RubyRegexp re = (RubyRegexp) other;
        checkInitialized();
        if (!re.source().getValue().equals(pattern)) {
            return getRuby().getFalse();
        }
        if (re.casefold().op_xor(casefold()).isTrue()) {
            return getRuby().getFalse();
        }
        return getRuby().getTrue();
    }

    /** rb_reg_match2
     * 
     */
    public RubyObject match2() {
        RubyObject target = getRuby().getLastline();
        if (!(target instanceof RubyString)) {
            return getRuby().getNil();
        }
        return match(target);
    }

    /** rb_reg_match
     * 
     */
    public RubyObject match(RubyObject target) {
        if (target.isNil()) {
            return getRuby().getFalse();
        }
        int result = search(target, 0);
        if (result < 0) {
            return getRuby().getNil();
        }
        return RubyFixnum.newFixnum(getRuby(), result);
    }

    /** rb_reg_match_m
     * 
     */
    public RubyObject match_m(RubyObject target) {
        if (target.isNil()) {
            return target;
        }
        RubyObject result = match(target);
        return result.isNil() ? result : ((RubyMatchData) ruby.getBackref()).rbClone();
    }

    /** rb_reg_source
     * 
     */
    public RubyString source() {
        checkInitialized();
        return RubyString.newString(getRuby(), pattern);
    }

    /** rb_reg_casefold_p
     * 
     */
    public RubyBoolean casefold() {
        checkInitialized();
        return matcher.getCasefold() ? getRuby().getTrue() : getRuby().getFalse();
    }

    /** rb_reg_nth_match
     *
     */
    public static RubyObject nth_match(int n, RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData) match).group(n);
    }

    /** rb_reg_last_match
     *
     */
    public static RubyObject last_match(RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData) match).group(0);
    }

    /** rb_reg_match_pre
     *
     */
    public static RubyObject match_pre(RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData) match).pre_match();
    }

    /** rb_reg_match_post
     *
     */
    public static RubyObject match_post(RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData) match).post_match();
    }

    /** rb_reg_match_last
     *
     */
    public static RubyObject match_last(RubyObject match) {
        if (match.isNil()) {
            return match;
        }
        RubyMatchData md = (RubyMatchData) match;
        for (long i = md.getSize() - 1; i > 0; i--) {
            if (!md.group(i).isNil()) {
                return md.group(i);
            }
        }
        return md.getRuby().getNil();
    }

    /** rb_reg_search
     *
     */
    public int search(RubyObject target, int pos) {
        String str = RubyString.stringValue(target).getValue();
        if (pos > str.length()) {
            return -1;
        }
        recompileIfNeeded();

        // If nothing match then nil will be returned
        RubyObject result = matcher.search(getRuby(), str, pos);
        getRuby().setBackref(result);

        // If nothing match then -1 will be returned
        return result instanceof RubyMatchData ? ((RubyMatchData) result).matchStartPosition() : -1;
    }

    /** rb_reg_regsub
     *
     */
    public RubyObject regsub(RubyObject str, RubyMatchData match) {
        String repl = RubyString.stringValue(str).getValue();
        StringBuffer sb = new StringBuffer("");
        int pos = 0;
        int end = repl.length();
        char c;
        RubyObject ins = getRuby().getNil();
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
                    ins = getRuby().getNil();
                }
            } else {
                sb.append(c);
            }
        }
        return RubyString.newString(getRuby(), sb.toString());
    }

    public RubyObject rbClone() {
        return newRegexp(getRuby(), source(), options);
    }

    /** rb_reg_inspect
     *
     */
    public RubyString inspect() {
        final int length = pattern.length();
        StringBuffer sb = new StringBuffer(length + 2);

        sb.append('/');
        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);

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

        if ((options & RE_OPTION_IGNORECASE) > 0) {
            sb.append('i');
        }
        if ((options & RE_OPTION_MULTILINE) > 0) {
            sb.append('m');
        }
        if ((options & RE_OPTION_EXTENDED) > 0) {
            sb.append('x');
        }

        return RubyString.newString(getRuby(), sb.toString());
    }


    public void marshalTo(MarshalStream output) throws java.io.IOException {
	output.write('/');
	output.dumpString(pattern);
	output.dumpInt(options);
    }
}
