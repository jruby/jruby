/*
 * RubyId.java - No description
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

import org.jruby.original.*;
import org.jruby.parser.*;

/**
 *
 * @author  jpetersen
 */
public class RubyId extends ID {
    private Ruby ruby;

    public RubyId(Ruby ruby) {
        this(ruby, 0);
    }
    
    public RubyId(Ruby ruby, int value) {
        super();
        this.ruby = ruby;
	this.value = value;
    }
    
    public RubySymbol toSymbol() {
        return null;
    }
    
    public String toName() {
        return ID.rb_id2name(getRuby(), this);
    }
    
    public boolean isConstId() {
        return is_const_id();
    }
    
    public boolean isClassId() {
        return is_class_id();
    }
    
    public boolean isGlobalId() {
        return is_global_id();
    }
    
    public boolean equals(Object object) {
        return (object instanceof RubyId) && 
               ((RubyId)object).value == value;
    }

    public int hashCode() {
        return value;
    }
    
    /** Getter for property ruby.
     * @return Value of property ruby.
     */
    public org.jruby.Ruby getRuby() {
        return ruby;
    }
    
    /** Setter for property ruby.
     * @param ruby New value of property ruby.
     */
    public void setRuby(org.jruby.Ruby ruby) {
        this.ruby = ruby;
    }
    
}
