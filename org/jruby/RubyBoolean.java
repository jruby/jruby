/*
 * RubyBoolean.java - No description
 * Created on 09. Juli 2001, 21:38
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
public class RubyBoolean extends RubyObject {
    private boolean value;   

    public RubyBoolean(Ruby ruby, boolean value) {
        super(ruby);
        this.value = value;
    }

    public RubyModule getRubyClass() {
        if (value) {
            return getRuby().getTrueClass();
        } else {
            return getRuby().getFalseClass();
        }
    }
    
    public boolean isTrue() {
        return value;
    }
    
    public boolean isFalse() {
        return !value;
    }
    
    public static RubyBoolean m_newBoolean(Ruby ruby, boolean value) {
        if (value) {
            return ruby.getTrue();
        } else {
            return ruby.getFalse();
        }
    }
    
    // Methods of the False class (false_*)
    
    
    /** false_to_s
     *  true_to_s
     *
     */
    public RubyString m_to_s() {
        if (isFalse()) {
            return RubyString.m_newString(getRuby(), "false");
        } else {
            return RubyString.m_newString(getRuby(), "true");
        }
    }
    
    /** false_type
     *  true_type
     *
     */
    public RubyModule m_type() {
        return (RubyClass)getRubyClass();
    }
    
    /** false_and
     *  true_and
     *
     */
    public RubyBoolean op_and(RubyBoolean obj) {
        if (isTrue() && obj.isTrue()) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
    }
    
    /** false_or
     *  true_or
     *
     */
    public RubyBoolean op_or(RubyBoolean obj) {
        if (isFalse() && obj.isFalse()) {
            return getRuby().getFalse();
        } else {
            return getRuby().getTrue();
        }
    }

    /** false_xor
     *  true_xor
     *
     */
    public RubyBoolean op_xor(RubyBoolean obj) {
        if ((isTrue() && obj.isFalse()) || (isFalse() && obj.isTrue())) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
    }

}