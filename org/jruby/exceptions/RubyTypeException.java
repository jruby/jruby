/*
 * RubyTypeException.java - No description
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

package org.jruby.exceptions;

/**
 *
 * @author  jpetersen
 */
public class RubyTypeException extends java.lang.RuntimeException {

    /**
     * Creates new <code>RubyTypeException</code> without detail message.
     */
    public RubyTypeException() {
    }


    /**
     * Constructs an <code>RubyTypeException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public RubyTypeException(String msg) {
        super(msg);
    }
}


