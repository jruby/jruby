/*
 * RubyString.java - No description
 * Created on 04. Juli 2001, 22:53
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

/**
 *
 * @author  jpetersen
 */
public class RubyString extends RubyObject {
    private String value;
    
    public RubyString(Ruby ruby) {
        this(ruby, null);
    }
    
    public RubyString(Ruby ruby, String str) {
        this(ruby, ruby.getClasses().getStringClass(), str);
    }
    
    public RubyString(Ruby ruby, RubyModule rubyClass, String str) {
        super(ruby, rubyClass);
        this.value = str;
    }
    
    public Class getJavaClass() {
        return String.class;
    }

    /**
     * @deprecated
     */
    public String getString() {
        return value;
    }
    
    /**
     * @deprecated
     */
    public void setString(String str) {
        value = str;
    }
    
    public String getValue() {
        return this.value;
    }
    
    public void setValue(String newValue) {
        value = newValue;
    }
    
    public String toString() {
        return "\"" + getValue() + "\"";
    }
    
    /**
     *
     */
    public RubyObject slice(int beg, int len) {
        // ...
        
        return m_newString(getRuby(), getValue().substring(beg, beg + len));
    }
    
    /** rb_str_cmp
     *
     */
    public int cmp(RubyString other) {
        /* use Java implementatiom */
        return getValue().compareTo(other.getValue());
    }
        
    /** rb_to_id
     *
     */
    public RubyId toId() {
        return getRuby().intern(getValue());
    }
        
    /** Create a new String which uses the same Ruby runtime and the same
     *  class like this String.
     *
     *  This method should be used to satisfy RCR #38.
     *
     */
    public RubyString newString(String s) {
        RubyModule klass = getRubyClass();
        while (klass.isIncluded() || klass.isSingleton()) {
            klass = klass.getSuperClass();
        }
        
        return new RubyString(getRuby(), klass, s);
    }

    // Methods of the String class (rb_str_*):
    
    /** rb_str_new2
     *
     */
    public static RubyString m_newString(Ruby ruby, String str) {
        return new RubyString(ruby, str);
    }
    
    /** rb_str_new
     *
     */
    public static RubyString m_newString(Ruby ruby, String str, int len) {
        return new RubyString(ruby, str.substring(0, len));
    }
    
    /** rb_str_dup
     *
     */
    public RubyObject m_dup() {
        return newString(getValue());
    }
    
    /** rb_str_clone
     *
     */
    public RubyObject m_clone() {
        RubyObject newObject = m_dup();
        
        newObject.setupClone(this);
        
        return newObject;
    }
    
    /** rb_str_cat
     *
     */
    public RubyString m_cat(String str) {
        value = value + str;
        return this;
    }
    
    /** rb_str_to_s
     *
     */
    public RubyString m_to_s() {
        return this;
    }
    
    /** rb_str_replace_m
     *
     */
    public RubyString m_replace(RubyString other) {
        if (this == other || getValue().equals(other.getValue())) {
            return this;
        }
        
        setString(other.getValue());
        
        // The other stuff isn't needed?
        
        return this;
    }
    
    /** rb_str_reverse
     *
     */
    public RubyString m_reverse() {
        StringBuffer sb = new StringBuffer(getValue().length());
        for (int i = getValue().length() - 1; i >= 0; i--) {
            sb.append(getValue().charAt(i));
        }
        
        return newString(sb.toString());
    }
    
    /**
     *
     */
    public RubyObject m_slice(RubyObject args[]) {
        if (args.length > 1) {
            RubyFixnum beg = (RubyFixnum)args[0];
            RubyFixnum len = (RubyFixnum)args[1];
            
            return slice((int)beg.getValue(), (int)len.getValue());
        } else {
            // ...
        }
        
        return getRuby().getNil();
    }
    
    /** rb_str_s_new
     *
     */
    public static RubyString m_new(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyString newString = m_newString(ruby, "");
        newString.setRubyClass((RubyModule)recv);
        
        newString.callInit(args);
        
        return newString;
    }
    
    /** rb_str_cmp_m
     *
     */
    public RubyFixnum op_cmp(RubyObject other) {
        return RubyFixnum.m_newFixnum(getRuby(), cmp(get_str(other)));
    }

    /** rb_str_equal
     *
     */
    public RubyBoolean m_equal(RubyObject other) {
        if (other == this) {
            return getRuby().getTrue();
        } else if (!(other instanceof RubyString)) {
            return getRuby().getFalse();
        }
        /* use Java implementation */
        return getValue().equals(((RubyString)other).getValue()) ? getRuby().getTrue() : getRuby().getFalse();
    }

    /** rb_str_match
     *
     */
    public RubyObject m_match(RubyObject other) {
        if (other instanceof RubyRegexp) {
            return ((RubyRegexp)other).m_match(this);
        } else if (other instanceof RubyString) {
            return RubyRegexp.m_newRegexp(getRuby(), (RubyString)other, 0).m_match(this);
        }
        return other.funcall(getRuby().intern("=~"), this);
    }

    /** rb_str_match2
     *
     */
    public RubyObject m_match2() {
        return RubyRegexp.m_newRegexp(getRuby(), this, 0).m_match2();
    }

    /** rb_str_capitalize
     *
     */
    public RubyString m_capitalize() {
        final int length = getValue().length();
        
        StringBuffer sb = new StringBuffer(length);
        if (length > 0) {
            sb.append(Character.toUpperCase(getValue().charAt(0)));
        }
        if (length > 1) {
            sb.append(getValue().toLowerCase().substring(1));
        }
        
        return newString(sb.toString());
    }

    /** rb_str_capitalize_bang
     *
     */
    public RubyString m_capitalize_bang() {
        final int length = getValue().length();
        
        StringBuffer sb = new StringBuffer(length);
        if (length > 0) {
            sb.append(Character.toUpperCase(getValue().charAt(0)));
        }
        if (length > 1) {
            sb.append(getValue().toLowerCase().substring(1));
        }
        
        setString(sb.toString());
        
        return this;
    }

    /** rb_str_upcase
     *
     */
    public RubyString m_upcase() {
        return newString(getValue().toUpperCase());
    }
    
    /** rb_str_upcase_bang
     *
     */
    public RubyString m_upcase_bang() {
        setString(getValue().toUpperCase());
        
        return this;
    }

    /** rb_str_downcase
     *
     */
    public RubyString m_downcase() {
        return newString(getValue().toLowerCase());
    }
    
    /** rb_str_downcase_bang
     *
     */
    public RubyString m_downcase_bang() {
        setString(getValue().toLowerCase());
        
        return this;
    }

    /** rb_str_swapcase
     *
     */
    public RubyString m_swapcase() {
        RubyString newString = newString(getValue());
        
        return newString.m_swapcase_bang();
    }
    
    /** rb_str_swapcase_bang
     *
     */
    public RubyString m_swapcase_bang() {
        char[] chars = getValue().toCharArray();
        StringBuffer sb = new StringBuffer(chars.length);
        
        for (int i = 0; i < chars.length; i++) {
            if (!Character.isLetter(chars[i])) {
                sb.append(chars[i]);
            } else if (Character.isLowerCase(chars[i])) {
                sb.append(Character.toUpperCase(chars[i]));
            } else {
                sb.append(Character.toLowerCase(chars[i]));
            }
        }
        setString(getValue().toLowerCase());
        
        return this;
    }
    
    /** rb_str_inspect
     *
     */
    public RubyString m_inspect() {
        final int length = getValue().length();
        
        StringBuffer sb = new StringBuffer(length + 2 + (length / 100));
        
        sb.append('\"');
        
        for (int i = 0; i < length; i++) {
            char c = getValue().charAt(i);
            
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (c == '\"' || c == '\'' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append('\\').append('n');
            } else if (c == '\r') {
                sb.append('\\').append('r');
            } else if (c == '\t') {
                sb.append('\\').append('t');
            } else if (c == '\f') {
                sb.append('\\').append('f');
            } else if (c == '\u0013') {
                sb.append('\\').append('v');
            } else if (c == '\u0007') {
                sb.append('\\').append('a');
            } else if (c == '\u0033') {
                sb.append('\\').append('e');
                
            /* There may be other not printable characters. */
                
            } else {
                sb.append(c);
            }
        }
        
        sb.append('\"');
        
        return m_newString(getRuby(), sb.toString());
    }

    /** rb_str_plus
     *
     */
    public RubyString op_plus(RubyObject other) {
        RubyString str = get_str(other);
        
        RubyString newString = newString(getValue() + str.getValue());
        
        newString.setTaint(isTaint() || other.isTaint());
        
        return newString;
    }

    /** rb_str_mul
     *
     */
    public RubyString op_mul(RubyInteger other) {
        long len = other.getLongValue();
        
        if (len < 0) {
            throw new RubyArgumentException("negative argument");
        }
        
        if (Long.MAX_VALUE / len < getValue().length()) {
            throw new RubyArgumentException("argument too big");
        }
        StringBuffer sb = new StringBuffer((int)(getValue().length() * len));
        
        for (int i = 0; i < len; i++) {
            sb.append(getValue());
        }
        
        RubyString newString = newString(sb.toString());
        newString.setTaint(isTaint());
        return newString;
    }

    /** rb_str_hash_m
     *
     */
    public RubyFixnum m_hash() {
        return RubyFixnum.m_newFixnum(getRuby(), getValue().hashCode());
    }

    /** rb_str_length
     *
     */
    public RubyFixnum m_length() {
        return new RubyFixnum(getRuby(), getValue().length());
    }

    /** rb_str_empty
     *
     */
    public RubyBoolean m_empty() {
        return new RubyBoolean(getRuby(), getValue().length() == 0);
    }

    /** rb_str_append
     *
     */
    public RubyString m_append(RubyObject other) {
        return m_cat(get_str(other).getValue());
    }

    /** rb_str_concat
     *
     */
    public RubyString m_concat(RubyObject other) {
        if ((other instanceof RubyFixnum) && ((RubyFixnum)other).getLongValue() < 256) {
            char c = (char)((RubyFixnum)other).getLongValue();
            return m_cat("" + c);
        }
        return m_append(other);
    }

    private RubyString get_str(RubyObject other) {
        if (other instanceof RubyString) {
            return (RubyString)other;
        } else {
            try {
                return (RubyString)other.convertType(RubyString.class, "String", "to_str");
            } catch (Exception ex) {
                throw new RubyArgumentException("can't convert arg to String: " + ex.getMessage());
            }
        }
    }

    private RubyRegexp get_pat(RubyObject other) {
        if (other instanceof RubyRegexp) {
            return (RubyRegexp)other;
        } else if (other instanceof RubyString) {
            return RubyRegexp.m_newRegexp(getRuby(), (RubyString)other, 0);
        } else {
            throw new RubyArgumentException("can't convert arg to Regexp");
        }
    }

    /** rb_str_sub
     *
     */
    public RubyObject m_sub(RubyObject[] args) {
        return sub(args, false);
    }

    /** rb_str_sub_bang
     *
     */
    public RubyObject m_sub_bang(RubyObject[] args) {
        return sub(args, true);
    }

    private RubyObject sub(RubyObject[] args, boolean bang) {
        RubyRegexp pat;
        RubyObject repl = getRuby().getNil();
        boolean iter = false;
        if (args.length == 1 && getRuby().getInterpreter().isBlockGiven()) {
            pat = get_pat(args[0]);
            iter = true;
        } else if (args.length == 2) {
            pat = get_pat(args[0]);
            repl = args[1];
        } else {
            throw new RubyArgumentException("wrong number of arguments");
        }
        if (pat.m_search(this, 0) >= 0) {
            RubyMatchData match = (RubyMatchData)getRuby().getBackRef();
            RubyString newStr = (RubyString)match.m_pre_match();
            newStr.m_append((RubyString)(iter ? getRuby().yield(match.group(0)) 
                                              : pat.m_regsub(repl, match)));
            newStr.m_append((RubyString)match.m_post_match());
            if (bang) {
                setValue(newStr.getValue());
                return this;
            }
            return newStr;
        }
        if (bang) {
            return getRuby().getNil();
        }
        return this;
    }

    /** rb_str_gsub
     *
     */
    public RubyObject m_gsub(RubyObject[] args) {
        return gsub(args, false);
    }

    /** rb_str_gsub_bang
     *
     */
    public RubyObject m_gsub_bang(RubyObject[] args) {
        return gsub(args, true);
    }

    private RubyObject gsub(RubyObject[] args, boolean bang) {
        RubyRegexp pat;
        RubyObject repl = getRuby().getNil();
        RubyMatchData match;
        boolean iter = false;
        if (args.length == 1 && getRuby().getInterpreter().isBlockGiven()) {
            pat = get_pat(args[0]);
            iter = true;
        } else if (args.length == 2) {
            pat = get_pat(args[0]);
            repl = args[1];
        } else {
            throw new RubyArgumentException("wrong number of arguments");
        }
        int beg = pat.m_search(this, 0);
        if (beg < 0) {
            return bang ? getRuby().getNil() : m_dup();
        }
        StringBuffer sbuf = new StringBuffer();
        String str = getValue();
        RubyObject newStr;
        int offset = 0;
        while (beg >= 0) {
            match = (RubyMatchData)getRuby().getBackRef();
            sbuf.append(str.substring(offset, beg));
            newStr = iter ? getRuby().yield(match.group(0)) : pat.m_regsub(repl, match);
            sbuf.append(((RubyString)newStr).getValue());
            offset = match.matchEndPosition();
            beg = pat.m_search(this, (offset == beg ? beg + 1 : offset));
        }
        sbuf.append(str.substring(offset, str.length()));
        if (bang) {
            setValue(sbuf.toString());
            return this;
        }
        return m_newString(getRuby(), sbuf.toString());
    }

    /** rb_str_index_m
     *
     */
    public RubyObject m_index(RubyObject[] args) {
        return index(args, false);
    }
    
    /** rb_str_rindex_m
     *
     */
    public RubyObject m_rindex(RubyObject[] args) {
        return index(args, true);
    }

    private RubyObject index(RubyObject[] args, boolean reverse) {
        int pos = 0;
        if (count_args(args, 1, 2) == 2) {
            // pos = RubyNumeric.fix2int(args[1]);
            pos = (int)((RubyFixnum)args[1]).getLongValue();
        }
        if (pos < 0) {
            pos += getValue().length();
            if (pos < 0) {
                return getRuby().getNil();
            }
        }
        if (args[0] instanceof RubyRegexp) {
            pos = ((RubyRegexp)args[0]).m_search(this, pos);
            int dummy = pos;
            while (reverse && dummy > -1) {
                pos = dummy;
                dummy = ((RubyRegexp)args[0]).m_search(this, pos + 1);
            }
        }
        else if (args[0] instanceof RubyString) {
            String sub = ((RubyString)args[0]).getValue();
            pos = reverse ? getValue().lastIndexOf(sub) : getValue().indexOf(sub);
        }
        else if (args[0] instanceof RubyFixnum) {
            char c = (char)((RubyFixnum)args[0]).getLongValue();
            pos = reverse ? getValue().lastIndexOf(c) : getValue().indexOf(c);
        }
        else {
            throw new RubyArgumentException("wrong type of argument");
        }
        
        if (pos == -1) {
            return getRuby().getNil();
        }
        return RubyFixnum.m_newFixnum(getRuby(), pos);
    }
}
