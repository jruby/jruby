/*
 * RubyNil.java - No description
 * Created on 09. Juli 2001, 21:38
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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

/**
 *
 * @author  jpetersen
 */
public class RubyNil extends RubyObject {

    public RubyNil(Ruby ruby) {
        super(ruby, null);
    }
    
    public RubyClass getRubyClass() {
        return getRuby().getClasses().getNilClass();
    }

    public boolean isNil() {
        return true;
    }

    public boolean isFalse() {
        return true;
    }

    public boolean isTrue() {
        return false;
    }

    // Methods of the Nil Class (nil_*):
        
    /** nil_to_i
     *
     */
    public RubyFixnum to_i() {
        return RubyFixnum.m_newFixnum(getRuby(), 0);
    }

    /** nil_to_s
     *
     */
    public RubyString to_s() {
        return RubyString.newString(getRuby(), "");
    }
    
    /** nil_to_a
     *
     */
    public RubyArray to_a() {
        return RubyArray.m_newArray(getRuby(), 0);
    }
    
    /** nil_inspect
     *
     */
    public RubyString inspect() {
        return RubyString.newString(getRuby(), "nil");
    }
    
    /** nil_type
     *
     */
    public RubyClass type() {
        return getRubyClass();
    }
    
    /** nil_and
     *
     */
    public RubyBoolean op_and(RubyObject obj) {
        return getRuby().getFalse();
    }
    
    /** nil_or
     *
     */
    public RubyBoolean op_or(RubyObject obj) {
        if (obj.isFalse()) {
            return getRuby().getFalse();
        } else {
            return getRuby().getTrue();
        }
    }

    /** nil_xor
     *
     */
    public RubyBoolean op_xor(RubyObject obj) {
        if (obj.isTrue()) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
    }
}
