/*
 * RubyIdPointer.java - No description
 * Created on 02. November 2001, 15:52
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
public class RubyIdPointer extends DefaultPointer {

    public RubyIdPointer() {
        this(new ArrayList(), 0, true, null);
    }
    
    public RubyIdPointer(int size) {
        this(new ArrayList(Collections.nCopies(size, null)), 0, true, null);
    }
    
    public RubyIdPointer(boolean autoResize) {
        this(new ArrayList(), 0, autoResize, null);
    }
    
    public RubyIdPointer(Object autoResizeObject, int size) {
        this(new ArrayList(Collections.nCopies(size, autoResizeObject)), 0, true, autoResizeObject);
    }
    
    public RubyIdPointer(ArrayList delegate) {
        this(delegate, 0, true, null);
    }

    public RubyIdPointer(ArrayList delegate, Object autoResizeObject) {
        this(delegate, 0, true, autoResizeObject);
    }

    public RubyIdPointer(ArrayList delegate, boolean autoResize) {
        this(delegate, 0, autoResize, null);
    }

    protected RubyIdPointer(ArrayList delegate, int position, boolean autoResize, Object autoResizeObject) {
        super(delegate, position, autoResize, autoResizeObject);
    }

    public RubyId getId(int index) {
        return (RubyId)get(index);
    }
    
    public RubyId[] toIdArray() {
        return (RubyId[])toArray(new RubyId[size()]);
    }

    public RubyId[] toIdArray(RubyId[] array) {
        return (RubyId[])toArray(array);
    }
}