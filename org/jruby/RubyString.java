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
        return "\"" + getString() + "\"";
    }
    
    /**
     *
     */
    public RubyObject slice(int beg, int len) {
        // ...
        
        return m_newString(getRuby(), getString().substring(beg, beg + len));
    }
    
    /** rb_str_cmp
     *
     */
    public int cmp(RubyObject other) {
        /* use Java implementatiom */
        return getString().compareTo(((RubyString)other).getString());
    }
        
    /** rb_to_id
     *
     */
    public RubyId toId() {
        return getRuby().intern(getString());
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
        return newString(getString());
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
        if (this == other || getString() == other.getString()) {
            return this;
        }
        
        setString(other.getString());
        
        // The other stuff isn't needed?
        
        return this;
    }
    
    /** rb_str_reverse
     *
     */
    public RubyString m_reverse() {
        StringBuffer sb = new StringBuffer(getString().length());
        for (int i = getString().length() - 1; i >= 0; i--) {
            sb.append(getString().charAt(i));
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
        if (!(other instanceof RubyString)) {
            other = other.convertType(RubyString.class, "String", "to_str");
        }
        
        return RubyFixnum.m_newFixnum(getRuby(), cmp(other));
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
        return getString().equals(((RubyString)other).getString()) ? getRuby().getTrue() : getRuby().getFalse();
    }

    /** rb_str_capitalize
     *
     */
    public RubyString m_capitalize() {
        final int length = getString().length();
        
        StringBuffer sb = new StringBuffer(length);
        if (length > 0) {
            sb.append(Character.toUpperCase(getString().charAt(0)));
        }
        if (length > 1) {
            sb.append(getString().toLowerCase().substring(1));
        }
        
        return newString(sb.toString());
    }

    /** rb_str_capitalize_bang
     *
     */
    public RubyString m_capitalize_bang() {
        final int length = getString().length();
        
        StringBuffer sb = new StringBuffer(length);
        if (length > 0) {
            sb.append(Character.toUpperCase(getString().charAt(0)));
        }
        if (length > 1) {
            sb.append(getString().toLowerCase().substring(1));
        }
        
        setString(sb.toString());
        
        return this;
    }

    /** rb_str_upcase
     *
     */
    public RubyString m_upcase() {
        return newString(getString().toUpperCase());
    }
    
    /** rb_str_upcase_bang
     *
     */
    public RubyString m_upcase_bang() {
        setString(getString().toUpperCase());
        
        return this;
    }

    /** rb_str_downcase
     *
     */
    public RubyString m_downcase() {
        return newString(getString().toLowerCase());
    }
    
    /** rb_str_downcase_bang
     *
     */
    public RubyString m_downcase_bang() {
        setString(getString().toLowerCase());
        
        return this;
    }

    /** rb_str_swapcase
     *
     */
    public RubyString m_swapcase() {
        RubyString newString = newString(getString());
        
        return newString.m_swapcase_bang();
    }
    
    /** rb_str_swapcase_bang
     *
     */
    public RubyString m_swapcase_bang() {
        char[] chars = getString().toCharArray();
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
        setString(getString().toLowerCase());
        
        return this;
    }
    
    /** rb_str_inspect
     *
     */
    public RubyString m_inspect() {
        final int length = getString().length();
        
        StringBuffer sb = new StringBuffer(length + 2 + (length / 100));
        
        sb.append('\"');
        
        for (int i = 0; i < length; i++) {
            char c = getString().charAt(i);
            
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
        if (!(other instanceof RubyString)) {
            other = other.convertType(RubyString.class, "String", "to_str");
        }
        
        RubyString newString = newString(getString() + ((RubyString)other).getString());
        
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
        
        if (Long.MAX_VALUE / len < getString().length()) {
            throw new RubyArgumentException("argument too big");
        }
        StringBuffer sb = new StringBuffer((int)(getString().length() * len));
        
        for (int i = 0; i < len; i++) {
            sb.append(getString());
        }
        
        RubyString newString = newString(sb.toString());
        newString.setTaint(isTaint());
        return newString;
    }

    /** rb_str_hash_m
     *
     */
    public RubyFixnum m_hash() {
        return RubyFixnum.m_newFixnum(getRuby(), getString().hashCode());
    }
}
