/*
 * RubyRegexp.java - No description
 * Created on 26. Juli 2001, 00:01
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby;


import org.jruby.exceptions.*;
import org.jruby.parser.ReOptions;
import org.jruby.util.PrintfFormat;

/**
 *
 * @author  amoore
 * @version 
 */
public class RubyRegexp extends RubyObject implements ReOptions {

    private IRegexpAdapter matcher;
    private String pattern;
    private int options;
    
    public RubyRegexp(Ruby ruby) {
        super(ruby, ruby.getRubyClass("Regexp"));
        try {
            matcher = (IRegexpAdapter)ruby.getRegexpAdapterClass().newInstance();
        } catch (Exception ex) {
            // can't happen if JRuby is invoked via Main class
            throw new RubyBugException("Couldn't create regexp adapter");
        }
    }
    
    public void initialize(String pat, int opts) throws RubyException {
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
        matcher.compile(pattern);
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
        if ((globalCF && !matcher.getCasefold()) || 
            (matcher.getCasefold() && !globalCF && (options & RE_OPTION_IGNORECASE) == 0))
        {
            initialize(pattern, options);
        }
    }
    
    private void checkInitialized() throws RubyException {
        if (matcher == null) {
            throw new RubyTypeException("uninitialized Regexp");
        }
    }
    
    public static RubyRegexp regexpValue(RubyObject obj) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp)obj;
        } else if (obj instanceof RubyString) {
            return m_newRegexp(obj.getRuby(), (RubyString)obj, 0);
        } else {
            throw new RubyArgumentException("can't convert arg to Regexp");
        }
    }

    // Methods of the Regexp class (rb_reg_*):
    
    public static RubyRegexp m_newRegexp(Ruby ruby, RubyString str, int options) {
        return m_newRegexp(ruby, str.getValue(), options);
    }
    
    public static RubyRegexp m_newRegexp(Ruby ruby, String str, int options) {
        RubyRegexp re = new RubyRegexp(ruby);
        re.initialize(str, options);
        return re;
    }
    
    public static RubyRegexp m_new(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyRegexp re = new RubyRegexp(ruby);
        re.setRubyClass((RubyModule)recv);
        re.m_initialize(args);
        return re;
    }
    
    public RubyObject m_initialize(RubyObject[] args) {
        String pat = (args[0] instanceof RubyRegexp)
                   ? ((RubyRegexp)args[0]).m_source().getValue()
                   : RubyString.stringValue(args[0]).getValue();
        int opts = 0;
        if (args.length > 1) {
            if (args[1] instanceof RubyFixnum) {
                opts = (int)((RubyFixnum)args[1]).getLongValue();
            }
            else if (args[1].isTrue()) {
                opts |= RE_OPTION_IGNORECASE;
            }
        }
        
        initialize(pat, opts);
        return getRuby().getNil();
    }
    
    /** rb_reg_s_quote
     * 
     */
    public static RubyString m_quote(Ruby ruby, RubyObject recv, RubyString str) {
        String orig = str.getValue();
        RubyString newStr = RubyString.m_newString(ruby, quote(orig));
        newStr.infectObject(str);
        return newStr;
    }
    
    /** 
     * 
     */
    public static RubyObject m_last_match(Ruby ruby, RubyObject recv) {
        return ruby.getBackRef();
    }

    /** rb_reg_equal
     * 
     */
    public RubyBoolean m_equal(RubyObject other) {
        if (other == this) {
            return getRuby().getTrue();
        }
        if (!(other instanceof RubyRegexp)) {
            return getRuby().getFalse();
        }
        RubyRegexp re = (RubyRegexp)other;
        checkInitialized();
        if (!re.m_source().getValue().equals(pattern)) {
            return getRuby().getFalse();
        }
        if (re.m_casefold().op_xor(m_casefold()).isTrue()) {
            return getRuby().getFalse();
        }
        return getRuby().getTrue();
    }

    /** rb_reg_match2
     * 
     */
    public RubyObject m_match2() {
        RubyObject target = getRuby().getLastLine();
        if (!(target instanceof RubyString)) {
            return getRuby().getNil();
        }
        return m_match(target);
    }
    
    /** rb_reg_match
     * 
     */
    public RubyObject m_match(RubyObject target) {
        if (target.isNil()) {
            return getRuby().getFalse();
        }
        int result = m_search(target, 0);
        if (result < 0) {
            return getRuby().getNil();
        }
        return RubyFixnum.m_newFixnum(getRuby(), result);
    }
    
    /** rb_reg_match_m
     * 
     */
    public RubyObject m_match_m(RubyObject target) {
        if (target.isNil()) {
            return target;
        }
        RubyObject result = m_match(target);
        return result.isNil() ? result : ((RubyMatchData)result).m_clone();
    }
    
    /** rb_reg_source
     * 
     */
    public RubyString m_source() {
        checkInitialized();
        return RubyString.m_newString(getRuby(), pattern);
    }
    
    /** rb_reg_casefold_p
     * 
     */
    public RubyBoolean m_casefold() {
        checkInitialized();
        return matcher.getCasefold() ? getRuby().getTrue() : getRuby().getFalse();
    }

    /** rb_reg_nth_match
     *
     */
    public static RubyObject m_nth_match(int n, RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData)match).group(n);
    }

    /** rb_reg_last_match
     *
     */
    public static RubyObject m_last_match(RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData)match).group(0);
    }

    /** rb_reg_match_pre
     *
     */
    public static RubyObject m_match_pre(RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData)match).m_pre_match();
    }

    /** rb_reg_match_post
     *
     */
    public static RubyObject m_match_post(RubyObject match) {
        return match.isNil() ? match : ((RubyMatchData)match).m_post_match();
    }

    /** rb_reg_match_last
     *
     */
    public static RubyObject m_match_last(RubyObject match) {
        if (match.isNil()) {
            return match;
        }
        RubyMatchData md = (RubyMatchData)match;
        for (long i = md.size() - 1; i > 0; i--) {
            if (!md.group(i).isNil()) {
                return md.group(i);
            }
        }
        return md.getRuby().getNil();
    }

    /** rb_reg_search
     *
     */
    public int m_search(RubyObject target, int pos) {
        String str = RubyString.stringValue(target).getValue();
        if (pos > str.length()) {
            return -1;
        }
        recompileIfNeeded();
        
        // If nothing match then nil will be returned
        RubyObject result = matcher.search(getRuby(), str, pos);
        getRuby().setBackRef(result);
        
        // If nothing match then -1 will be returned
        return result instanceof RubyMatchData ? 
                ((RubyMatchData)result).matchStartPosition() : -1;
    }

    /** rb_reg_regsub
     *
     */
    public RubyObject m_regsub(RubyObject str, RubyMatchData match) {
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
                switch(c) {
                    case '0': case '1': case '2': case '3': case '4': 
                    case '5': case '6': case '7': case '8': case '9':
                        ins = match.group(c - '0');
                        break;
                    case '&':
                        ins = match.group(0);
                        break;
                    case '`':
                        ins = match.m_pre_match();
                        break;
                    case '\'':
                        ins = match.m_post_match();
                        break;
                    case '+':
                        ins = m_match_last(match);
                        break;
                    case '\\':
                        sb.append(c);
                        continue;
                    default:
                        sb.append('\\').append(c);
                        continue;
                }
                if (!ins.isNil()) {
                    sb.append(((RubyString)ins).getValue());
                    ins = getRuby().getNil();
                }
            } else {
                sb.append(c);
            }
        }
        return RubyString.m_newString(getRuby(), sb.toString());
    }

    public RubyObject m_clone() {
        return m_newRegexp(getRuby(), m_source(), options);
    }

    /** rb_reg_inspect
     *
     */
    public RubyString m_inspect() {
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
        
        return RubyString.m_newString(getRuby(), sb.toString());
    }
}

