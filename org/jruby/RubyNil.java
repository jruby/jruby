/*
 * RubyNil.java - No description
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
public class RubyNil extends RubyObject {

    public RubyNil(Ruby ruby) {
        super(ruby);
    }
    
    public RubyModule getRubyClass() {
        return getRuby().getNilClass();
    }

    public boolean isNil() {
        return true;
    }
    
    // Methods of the Nil Class (nil_*):
        
    /** nil_to_i
     *
     */
    public RubyFixnum m_to_i() {
        return RubyFixnum.m_newFixnum(getRuby(), 0);
    }

    /** nil_to_s
     *
     */
    public RubyString m_to_s() {
        return RubyString.m_newString(getRuby(), "");
    }
    
    /** nil_to_a
     *
     */
    public RubyArray m_to_a() {
        return RubyArray.m_newArray(getRuby(), 0);
    }
    
    /** nil_inspect
     *
     */
    public RubyString m_inspect() {
        return RubyString.m_newString(getRuby(), "nil");
    }
    
    /** nil_type
     *
     */
    public RubyModule m_type() {
        return (RubyClass)getRubyClass();        
    }
}