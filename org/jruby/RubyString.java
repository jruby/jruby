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
import org.jruby.util.PrintfFormat;

/**
 *
 * @author  jpetersen
 */
public class RubyString extends RubyObject {

    private static RubyModule pfClass;
    
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
        RubyString newStr = newString(getValue());
        newStr.infectObject(this);
        return newStr;
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
    public RubyString m_replace(RubyObject other) {
        RubyString str = get_str(other);
        if (this == other || getValue().equals(str.getValue())) {
            return this;
        }
        setValue(str.getValue());
        infectObject(str);
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
    
    /** rb_str_reverse_bang
     *
     */
    public RubyString m_reverse_bang() {
        StringBuffer sb = new StringBuffer(getValue().length());
        for (int i = getValue().length() - 1; i >= 0; i--) {
            sb.append(getValue().charAt(i));
        }
        setValue(sb.toString());
        return this;
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
        
        setValue(sb.toString());
        
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
        setValue(getValue().toUpperCase());
        
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
        setValue(getValue().toLowerCase());
        
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
        setValue(getValue().toLowerCase());
        
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
            } else if (c == '\u000B') {
                sb.append('\\').append('v');
            } else if (c == '\u0007') {
                sb.append('\\').append('a');
            } else if (c == '\u001B') {
                sb.append('\\').append('e');
            } else if (isPrint(c)) {
                sb.append(c);
            } else {
                sb.append(new PrintfFormat("\\%.3o").sprintf(c));
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
        
        newString.infectObject(str);
        
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
        infectObject(other);
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

    /* rb_str_to_str */
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

    /* get_pat */
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
        RubyObject repl = getRuby().getNil();
        boolean iter = false;
        if (args.length == 1 && getRuby().getInterpreter().isBlockGiven()) {
            iter = true;
        } else if (args.length == 2) {
            repl = args[1];
        } else {
            throw new RubyArgumentException("wrong number of arguments");
        }
        RubyRegexp pat = get_pat(args[0]);

        if (pat.m_search(this, 0) >= 0) {
            RubyMatchData match = (RubyMatchData)getRuby().getBackRef();
            RubyString newStr = (RubyString)match.m_pre_match();
            newStr.m_append((RubyString)(iter ? getRuby().yield(match.group(0)) 
                                              : pat.m_regsub(repl, match)));
            newStr.m_append((RubyString)match.m_post_match());
            if (bang) {
                m_replace(newStr);
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
        RubyObject repl = getRuby().getNil();
        RubyMatchData match;
        boolean iter = false;
        if (args.length == 1 && getRuby().getInterpreter().isBlockGiven()) {
            iter = true;
        } else if (args.length == 2) {
            repl = args[1];
        } else {
            throw new RubyArgumentException("wrong number of arguments");
        }
        boolean taint = repl.isTaint();
        RubyRegexp pat = get_pat(args[0]);

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
            taint |= newStr.isTaint();
            sbuf.append(((RubyString)newStr).getValue());
            offset = match.matchEndPosition();
            beg = pat.m_search(this, (offset == beg ? beg + 1 : offset));
        }
        sbuf.append(str.substring(offset, str.length()));
        if (bang) {
            setTaint(isTaint() || taint);
            setValue(sbuf.toString());
            return this;
        }
        RubyString result = newString(sbuf.toString());
        result.setTaint(taint);
        return result;
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
            pos = RubyNumeric.fix2int(args[1]);
        }
        if (pos < 0) {
            pos += getValue().length();
            if (pos < 0) {
                return getRuby().getNil();
            }
        }
        if (args[0] instanceof RubyRegexp) {
            pos = ((RubyRegexp)args[0]).m_search(this, pos);
            // RubyRegexp doesn't (yet?) support reverse searches, so we
            // find all matches and use the last one--very inefficient.
            // XXX - find a better way
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
    
    /* rb_str_substr */
    private RubyObject substr(int beg, int len) {
        int strLen = getValue().length();
        if (len < 0 || beg >= strLen) {
            return getRuby().getNil();
        }
        if (beg < 0) {
            beg += strLen;
            if (beg < 0) {
                return getRuby().getNil();
            }
        }
        int end = Math.min(strLen, beg + len);
        RubyString newStr = newString(getValue().substring(beg, end));
        newStr.infectObject(this);
        return newStr;
    }

    /* rb_str_replace */
    private RubyObject replace(int beg, int len, RubyString repl) {
        int strLen = getValue().length();
        if (beg + len >= strLen) {
            len = strLen - beg;
        }
        setValue(getValue().substring(0, beg) + repl.getValue() + getValue().substring(beg + len));
        infectObject(repl);
        return this;
    }
    
    /** rb_str_aref, rb_str_aref_m
     *
     */
    public RubyObject m_aref(RubyObject[] args) {
        if (count_args(args, 1, 2) == 2) {
            int beg = RubyNumeric.fix2int(args[0]);
            int len = RubyNumeric.fix2int(args[1]);
            return substr(beg, len);
        }
        if (args[0] instanceof RubyFixnum) { // RubyNumeric?
            int idx = RubyNumeric.fix2int(args[0]); // num2int?
            if (idx < 0) {
                idx += getValue().length();
            }
            if (idx < 0 || idx >= getValue().length()) {
                return getRuby().getNil();
            }
            return new RubyFixnum(getRuby(), getValue().charAt(idx));
        }
        if (args[0] instanceof RubyRegexp) {
            if (get_pat(args[0]).m_search(this, 0) >= 0) {
                return RubyRegexp.m_last_match(getRuby().getBackRef());
            }
            return getRuby().getNil();
        }
        if (args[0] instanceof RubyString) {
            if (getValue().indexOf(get_str(args[0]).getValue()) != -1) {
                return args[0];
            }
            return getRuby().getNil();
        }
        if (args[0] instanceof RubyRange) {
            long[] idxs = ((RubyRange)args[0]).getBeginLength(getValue().length());
            return substr((int)idxs[0], (int)idxs[1]);
        }
        throw new RubyTypeException("wrong argument type");
    }
    
    /** rb_str_aset, rb_str_aset_m
     *
     */
    public RubyObject m_aset(RubyObject[] args) {
        int strLen = getValue().length();
        if (count_args(args, 2, 3) == 3) {
            RubyString repl = get_str(args[2]);
            int beg = RubyNumeric.fix2int(args[0]);
            int len = RubyNumeric.fix2int(args[1]);
            if (len < 0) {
                throw new RubyIndexException("negative length");
            }
            if (beg < 0) {
                beg += strLen;
            }
            if (beg < 0 || beg >= strLen) {
                throw new RubyIndexException("string index out of bounds");
            }
            if (beg + len > strLen) {
                len = strLen - beg;
            }
            replace(beg, len, repl);
            return repl;
        }
        if (args[0] instanceof RubyFixnum) { // RubyNumeric?
            int idx = RubyNumeric.fix2int(args[0]); // num2int?
            if (idx < 0) {
                idx += getValue().length();
            }
            if (idx < 0 || idx >= getValue().length()) {
                throw new RubyIndexException("string index out of bounds");
            }
            if (args[1] instanceof RubyFixnum) {
                char c = (char)RubyNumeric.fix2int(args[1]);
                setValue(getValue().substring(0, idx) + c + getValue().substring(idx + 1));
            } else {
                replace(idx, 1, get_str(args[1]));
            }
            return args[1];
        }
        if (args[0] instanceof RubyRegexp) {
            m_sub_bang(args);
            return args[1];
        }
        if (args[0] instanceof RubyString) {
            RubyString orig = get_str(args[0]);
            int beg = getValue().indexOf(orig.getValue());
            if (beg != -1) {
                replace(beg, orig.getValue().length(), get_str(args[1]));
            }
            return args[1];
        }
        if (args[0] instanceof RubyRange) {
            long[] idxs = ((RubyRange)args[0]).getBeginLength(getValue().length());
            replace((int)idxs[0], (int)idxs[1], get_str(args[1]));
            return args[1];
        }
        throw new RubyTypeException("wrong argument type");
    }
    
    /** rb_str_slice_bang
     *
     */
    public RubyObject m_slice_bang(RubyObject[] args) {
        int argc = count_args(args, 1, 2);
        RubyObject[] newArgs = new RubyObject[argc + 1];
        newArgs[0] = args[0];
        if (argc > 1) {
            newArgs[1] = args[1];
        }
        newArgs[argc] = newString("");
        RubyObject result = m_aref(args);
        m_aset(newArgs);
        return result;
    }
    
    /** rb_str_format
     *
     */
    public RubyObject m_format(RubyObject arg) {
        if (pfClass == null) {
            try {
                Class c = Class.forName("org.jruby.util.PrintfFormat");
                pfClass = RubyJavaObject.loadClass(getRuby(), c, null);
            } catch (ClassNotFoundException ex) {
                throw new RubyBugException("couldn't find PrintfFormat class");
            }
        }
        RubyObject pfObject = pfClass.funcall(getRuby().intern("new"), this);
        return pfObject.funcall(getRuby().intern("sprintf"), arg);
    }
    
    /** rb_str_succ
     *
     */
    public RubyObject m_succ() {
        return succ(false);
    }

    /** rb_str_succ_bang
     *
     */
    public RubyObject m_succ_bang() {
        return succ(true);
    }

    private RubyString succ(boolean bang) {
        if (getValue().length() == 0) {
            return bang ? this : (RubyString)m_dup();
        }
        StringBuffer sbuf = new StringBuffer(getValue());
        boolean alnumSeen = false;
        int pos = -1;
        char c = 0;
        char n = 0;
        for (int i = sbuf.length() - 1; i >= 0; i--) {
            c = sbuf.charAt(i);
            if (isAlnum(c)) {
                alnumSeen = true;
                if ((isDigit(c) && c < '9') || (isLower(c) && c < 'z') || (isUpper(c) && c < 'Z')) {
                    sbuf.setCharAt(i, (char)(c + 1));
                    pos = -1;
                    break;
                } else {
                    pos = i;
                    n = isDigit(c) ? '0' : (isLower(c) ? 'a' : 'A');
                    sbuf.setCharAt(i, n);
                }
            }
        }
        if (!alnumSeen) {
            for (int i = sbuf.length() - 1; i >= 0; i--) {
                c = sbuf.charAt(i);
                if (c < 0xff) {
                    sbuf.setCharAt(i, (char)(c + 1));
                    pos = -1;
                    break;
                } else {
                    pos = i;
                    n = '\u0001';
                    sbuf.setCharAt(i, '\u0000');
                }
            }
        }
        if (pos > -1) {
            sbuf.insert(pos, n);
        }

        if (bang) {
            setValue(sbuf.toString());
            return this;
        } else {
            RubyString newStr = (RubyString)m_dup();
            newStr.setValue(sbuf.toString());
            return newStr;
        }
    }
    
    /** rb_str_upto_m
     *
     */
    public RubyObject m_upto(RubyObject str) {
        return upto(str, false);
    }
    
    /* rb_str_upto */
    RubyObject upto(RubyObject str, boolean excl) {
        RubyString current = this;
        RubyString end = get_str(str);
        while (current.cmp(end) <= 0) {
            getRuby().yield(current);
            if (current.cmp(end) == 0) {
                break;
            }
            current = current.succ(false);
            if (excl && current.cmp(end) == 0) {
                break;
            }
            if (current.getValue().length() > end.getValue().length()) {
                break;
            }
        }
        return this;
    }
    
    public RubyBoolean m_include(RubyObject obj) {
        if (obj instanceof RubyFixnum) {
            char c = (char)RubyNumeric.fix2int(obj);
            return getValue().indexOf(c) == -1 ? getRuby().getFalse() : getRuby().getTrue();
        }
        String str = get_str(obj).getValue();
        return getValue().indexOf(str) == -1 ? getRuby().getFalse() : getRuby().getTrue();
    }
    
    public RubyObject m_to_i() {
        return RubyNumeric.str2inum(getRuby(), this, 10);
    }
    
    public RubyObject m_oct() {
        int base = 8;
        String str = getValue().trim();
        int pos = (str.charAt(0) == '-' || str.charAt(0) == '+') ? 1 : 0;
        if (str.indexOf("0x") == pos || str.indexOf("0X") == pos) {
            base = 16;
        } else if (str.indexOf("0b") == pos || str.indexOf("0B") == pos) {
            base = 2;
        }
        return RubyNumeric.str2inum(getRuby(), this, base);
    }
    
    public RubyObject m_hex() {
        return RubyNumeric.str2inum(getRuby(), this, 16);
    }
    
    public RubyObject m_to_f() {
        return RubyNumeric.str2d(getRuby(), this);
    }
    
    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    public static boolean isUpper(char c) {
        return c >= 'A' && c <= 'Z';
    }
    
    public static boolean isLower(char c) {
        return c >= 'a' && c <= 'z';
    }
    
    public static boolean isLetter(char c) {
        return isUpper(c) || isLower(c);
    }
    
    public static boolean isAlnum(char c) {
        return isUpper(c) || isLower(c) || isDigit(c);
    }
    
    public static boolean isPrint(char c) {
        return c >= 0x20 && c <= 0x7E;
    }
    
}
