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

/**
 *
 * @author  jpetersen
 */
public class RubyString extends RubyObject {
    private String string;
    
    public RubyString(Ruby ruby) {
        this(ruby, null);
    }
    
    public RubyString(Ruby ruby, String string) {
        this(ruby, ruby.getStringClass(), string);
    }
    
    public RubyString(Ruby ruby, RubyModule rubyClass, String string) {
        super(ruby, rubyClass);
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
    
    public void setString(String string) {
        this.string = string;
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
        return new RubyString(getRuby(), getRubyClass(), s);
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
        // HACK +++
        return m_newString(getRuby(), getString());
        // HACK ---
    }
    
    /** rb_str_cat
     *
     */
    public RubyString m_cat(String value) {
        string = string + value;
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
            other = other.covertType(RubyString.class, "String", "to_str");
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
}