/*
 * RubyPointer.java - No description
 * Created on 02. November 2001, 15:55
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

package org.jruby.util;

import java.util.*;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyPointer extends DefaultPointer {

    public RubyPointer() {
        this(new ArrayList(), 0, true, null);
    }
    
    public RubyPointer(int size) {
        this(new ArrayList(Collections.nCopies(size, null)), 0, true, null);
    }
    
    public RubyPointer(boolean autoResize) {
        this(new ArrayList(), 0, autoResize, null);
    }
    
    public RubyPointer(Object autoResizeObject, int size) {
        this(new ArrayList(Collections.nCopies(size, autoResizeObject)), 0, true, autoResizeObject);
    }
    
    public RubyPointer(List delegate) {
        this(delegate, 0, true, null);
    }

    public RubyPointer(List delegate, Object autoResizeObject) {
        this(delegate, 0, true, autoResizeObject);
    }

    public RubyPointer(List delegate, boolean autoResize) {
        this(delegate, 0, autoResize, null);
    }

    protected RubyPointer(List delegate, int position, boolean autoResize, Object autoResizeObject) {
        super(delegate, position, autoResize, autoResizeObject);
    }

    public RubyObject getRuby(int index) {
        return (RubyObject)get(index);
    }
    
    public RubyObject[] toRubyArray() {
        return (RubyObject[])toArray(new RubyObject[size()]);
    }

    public RubyObject[] toRubyArray(RubyObject[] array) {
        return (RubyObject[])toArray(array);
    }
}