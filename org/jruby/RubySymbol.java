/*
 * RubySymbol.java - No description
 * Created on 26. Juli 2001, 00:01
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
public class RubySymbol extends RubyObject {
    private RubyId id = null;
    
    public RubySymbol(Ruby ruby, RubyId id) {
        super(ruby, ruby.getClasses().getSymbolClass());
        this.id = id;
    }
    
    /** rb_to_id
     *
     */
    public RubyId toId() {
        return getId();
    }
    
    public String getName() {
        return getId().toName();
    }
    
    /** Getter for property id.
     * @return Value of property id.
     */
    public RubyId getId() {
        return id;
    }
    
    /** Setter for property id.
     * @param id New value of property id.
     */
    public void setId(RubyId id) {
        this.id = id;
    }
    
    public static RubySymbol m_newSymbol(Ruby ruby, RubyId rubyId) {
        return new RubySymbol(ruby, rubyId);
    }

    public RubyFixnum m_to_i() {
        return RubyFixnum.newFixnum(getRuby(), getId().intValue());
    }
    
    public RubyString m_inspect() {
        return RubyString.newString(getRuby(), ":" + getId().toName());
    }
    
    public RubyString m_to_s() {
        return RubyString.newString(getRuby(), getId().toName());
    }

    public RubyFixnum m_hash() {
        return RubyFixnum.newFixnum(getRuby(), (":" + getName()).hashCode());
    }

    public RubyBoolean m_equal(RubyObject other) {
        if ((other instanceof RubySymbol) && getName().equals(((RubySymbol) other).getName())) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
    }
}