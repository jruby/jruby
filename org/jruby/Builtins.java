/*
 * Builtins.java - description
 * Created on 20.02.2002, 17:02:56
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

/** Builtins provides methods to create the built-ins classes and to convert
 * between the classes.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Builtins {
    private Ruby ruby;

    public Builtins(Ruby ruby) {
        this.ruby = ruby;
    }
    
    public RubyArray newArray() {
        return RubyArray.newArray(ruby, 0);
    }
    
    public RubySymbol toSymbol(String name) {
        return RubySymbol.newSymbol(ruby, name);
    }
    
    public RubyString toString(String value) {
        return RubyString.newString(ruby, value);
    }

	/** Converts a RubyObject into a RubyArray.
	 * 
	 * If value is a RubyArray return value else
	 * return a new one element array with value as the element.
	 * 
	 */
    public RubyArray toArray(RubyObject value) {
        return value instanceof RubyArray ? (RubyArray)value : RubyArray.newArray(ruby, value);
    }

    public RubyInteger toInteger(long value) {
        return RubyFixnum.newFixnum(ruby, value);
    }
}
