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

import org.jruby.parser.ReOptions;
import org.jruby.runtime.regexp.IRegexpAdapter;
import org.jruby.util.Asserts;
import org.jruby.util.PrintfFormat;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.IndexCallable;
import org.jruby.internal.runtime.builtin.definitions.RegexpDefinition;
import org.jruby.exceptions.TypeError;
import org.jruby.exceptions.ArgumentError;

/**
 *
 * @author  amoore
 * @version $Revision$
 */
public class RubyRegexp extends RubyObject implements ReOptions, IndexCallable {
    private IRegexpAdapter matcher;
    private String pattern;
    private int options;

    public RubyRegexp(Ruby ruby) {
        super(ruby, ruby.getClass("Regexp"));
        try {
            matcher = (IRegexpAdapter) ruby.getRegexpAdapterClass().newInstance();
        } catch (Exception ex) {
            // can't happen if JRuby is invoked via Main class
            Asserts.notReached("Couldn't create regexp adapter");
        }
    }

    public static RubyClass createRegexpClass(Ruby runtime) {
        RubyClass regexpClass = new RegexpDefinition(runtime).getType();

        regexpClass.defineConstant("IGNORECASE", RubyFixnum.newFixnum(runtime, RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", RubyFixnum.newFixnum(runtime, RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", RubyFixnum.newFixnum(runtime, RE_OPTION_MULTILINE));

        return regexpClass;
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case RegexpDefinition.INITIALIZE :
                return initialize(args);
            case RegexpDefinition.RBCLONE :
                return rbClone();
            case RegexpDefinition.EQUAL :
                return equal(args[0]);
            case RegexpDefinition.MATCH :
                return match(args[0]);
            case RegexpDefinition.MATCH2 :
                return match2();
            case RegexpDefinition.MATCH_M :
                return match_m(args[0]);
            case RegexpDefinition.INSPECT :
                return inspect();
            case RegexpDefinition.SOURCE :
                return source();
            case RegexpDefinition.CASEFOLD :
                return casefold();
            default :
                return super.callIndexed(index, args);
        }
    }

    public void initialize(String pat, int opts) {
        pattern = pat;
        options = opts;
        if ((options & RE_OPTION_IGNORECASE) > 0) {
            matcher.setCasefold(true);
        }
        if ((options & RE_OPTION_EXTENDED) > 0) {
            matcher.setExtended(true);
        }
        if ((options & RE_OPTION_MULTILINE) > 0) {
            matcher.setMultiline(true);
        }
        matcher.compile(getRuntime(), pattern);
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
        if (matcher.getCasefold() && (options & RE_OPTION_IGNORECASE) == 0) {
            initialize(pattern, options);
        }
    }

    private void checkInitialized() {
        if (matcher == null) {
            throw new TypeError(getRuntime(), "uninitialized Regexp");
        }
    }

    public static RubyRegexp regexpValue(IRubyObject obj) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp) obj;
        } else if (obj instanceof RubyString) {
            return newRegexp((RubyString) obj, 0);
        } else {
            throw new ArgumentError(obj.getRuntime(), "can't convert arg to Regexp");
        }
    }

    // Methods of the Regexp class (rb_reg_*):

    public static RubyRegexp newRegexp(RubyString str, int options) {
        return newRegexp(str.getRuntime(), str.getValue(), options);
    }

    public static RubyRegexp newRegexp(Ruby ruby, String str, int options) {
        RubyRegexp re = new RubyRegexp(ruby);
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

        initialize(pat, opts);
        return getRuntime().getNil();
    }

    /** rb_reg_s_quote
     * 
     */
    public static RubyString quote(IRubyObject recv, IRubyObject str) {
        String orig = str.toString();
        RubyString newStr = RubyString.newString(recv.getRuntime(), quote(orig));
        newStr.infectBy(str);
        return newStr;
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
    public RubyBoolean equal(IRubyObject other) {
        if (other == this) {
            return getRuntime().getTrue();
        }
        if (!(other instanceof RubyRegexp)) {
            return getRuntime().getFalse();
        }
        RubyRegexp re = (RubyRegexp) other;
        checkInitialized();
        if (!re.source().getValue().equals(pattern)) {
            return getRuntime().getFalse();
        }
        if (matcher.getCasefold() ^ re.matcher.getCasefold()) {
            return getRuntime().getFalse();
        }
        return getRuntime().getTrue();
    }

    /** rb_reg_match2
     * 
     */
    public IRubyObject match2() {
        IRubyObject target = getRuntime().getLastline();
        if (!(target instanceof RubyString)) {
            return getRuntime().getNil();
        }
        return match(target);
    }

    /** rb_reg_match
     * 
     */
    public IRubyObject match(IRubyObject target) {
        if (target.isNil()) {
            return getRuntime().getFalse();
        }
        int result = search(target, 0);
        if (result < 0) {
            return getRuntime().getNil();
        }
        return RubyFixnum.newFixnum(getRuntime(), result);
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
        return RubyString.newString(getRuntime(), pattern);
    }

    /** rb_reg_casefold_p
     * 
     */
    public RubyBoolean casefold() {
        checkInitialized();
        return matcher.getCasefold() ? getRuntime().getTrue() : getRuntime().getFalse();
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
        IRubyObject result = matcher.search(getRuntime(), str, pos);
        getRuntime().setBackref(result);

        // If nothing match then -1 will be returned
        return result instanceof RubyMatchData ? ((RubyMatchData) result).matchStartPosition() : -1;
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

    public IRubyObject rbClone() {
        return newRegexp(source(), options);
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

        return RubyString.newString(getRuntime(), sb.toString());
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('/');
        output.dumpString(pattern);
        output.dumpInt(options);
    }
}
