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
public class IdPointer extends DefaultPointer {
    private int count;

    public IdPointer() {
        this(new ArrayList(), 0, true, null);
    }
    
    public IdPointer(int size) {
        this(new ArrayList(Collections.nCopies(size, null)), 0, true, null);
    }
    
    public IdPointer(boolean autoResize) {
        this(new ArrayList(), 0, autoResize, null);
    }
    
    public IdPointer(Object autoResizeObject, int size) {
        this(new ArrayList(Collections.nCopies(size, autoResizeObject)), 0, true, autoResizeObject);
    }
    
    public IdPointer(ArrayList delegate) {
        this(delegate, 0, true, null);
    }

    public IdPointer(ArrayList delegate, Object autoResizeObject) {
        this(delegate, 0, true, autoResizeObject);
    }

    public IdPointer(ArrayList delegate, boolean autoResize) {
        this(delegate, 0, autoResize, null);
    }

    protected IdPointer(ArrayList delegate, int position, boolean autoResize, Object autoResizeObject) {
        super(delegate, position, autoResize, autoResizeObject);
    }

    public String getId(int index) {
        return (String)get(index);
    }
    
    public String[] toIdArray() {
        return (String[])toArray(new String[size()]);
    }

    public String[] toIdArray(String[] array) {
        return (String[])toArray(array);
    }
    /**
     * Gets the count.
     * @return Returns a int
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count.
     * @param count The count to set
     */
    public void setCount(int count) {
        this.count = count;
    }
}