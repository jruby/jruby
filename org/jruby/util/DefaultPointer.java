/*
 * RubyPointer.java - No description
 * Created on 02. November 2001, 14:16
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

/**
 *
 * @author  jpetersen
 * @version 
 */
public class DefaultPointer extends AbstractList implements Pointer {
    private ArrayList delegate;
    
    private int position;
    private boolean autoResize;
    private Object autoResizeObject;
    
    public DefaultPointer() {
        this(new ArrayList(), 0, true, null);
    }
    
    public DefaultPointer(int size) {
        this(new ArrayList(Collections.nCopies(size, null)), 0, true, null);
    }
    
    public DefaultPointer(boolean autoResize) {
        this(new ArrayList(), 0, autoResize, null);
    }
    
    public DefaultPointer(Object autoResizeObject, int size) {
        this(null, 0, true, autoResizeObject);
        
        this.delegate = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            delegate.add(autoResizeObject);
        }
    }
    
    public DefaultPointer(ArrayList delegate) {
        this(delegate, 0, true, null);
    }

    public DefaultPointer(ArrayList delegate, Object autoResizeObject) {
        this(delegate, 0, true, autoResizeObject);
    }

    public DefaultPointer(ArrayList delegate, boolean autoResize) {
        this(delegate, 0, autoResize, null);
    }

    protected DefaultPointer(ArrayList delegate, int position, boolean autoResize, Object autoResizeObject) {
        super();
        
        this.delegate = delegate;
        this.position = position;
        this.autoResize = autoResize;
        this.autoResizeObject = autoResizeObject;
        
        autoResize(position);
    }
    
    public ArrayList getDelegate() {
        if (position == 0) {
            return delegate;
        } else {
            return new ArrayList(delegate.subList(position, delegate.size() - 1));
        }
    }
    
    protected void autoResize(int index) {
        if (!autoResize) {
            return;
        } else if (index < 0) {
            delegate.addAll(0, Collections.nCopies(-index, autoResizeObject));
            
            position += -index;
        } else if (index >= delegate.size()) {
            delegate.ensureCapacity(index);
            for (int i = delegate.size(); i <= index; i++) {
                delegate.add(autoResizeObject);
            }
        }
    }

    public int size() {
        return delegate.size() - position;
    }
    
    public Object get(int index) {
        autoResize(index + position);

        return delegate.get(index + position);
    }
    
    public Object set(int index, Object element) {
        autoResize(index + position);

        return delegate.set(index + position, element);
    }

    public void add(int index, Object element) {
        autoResize(index + position);

        delegate.add(index + position, element);
    }
    
    public void dec(int index) {
        inc(-index);
    }
    
    public void inc(int index) {
        position += index;
        
        autoResize(position);
    }
    
    public void dec() {
        inc(-1);
    }
    
    public void inc() {
        inc(1);
    }
    
    public void remove() {
        delegate.remove(position);
    }
    
    public Object remove(int index) {
        autoResize(position);
        
        return delegate.remove(position + index);
    }
    
    public Object next() {
        return delegate.get(position);
    }
    
    public Pointer getPointer(int index) {
        return new RubyPointer(delegate, position + index, autoResize, autoResizeObject);
    }
    
    public boolean hasNext() {
        return position < delegate.size();
    }
    
    /** Getter for property autoResize.
     * @return Value of property autoResize.
     */
    public boolean isAutoResize() {
        return autoResize;
    }
    
    /** Setter for property autoResize.
     * @param autoResize New value of property autoResize.
     */
    public void setAutoResize(boolean autoResize) {
        this.autoResize = autoResize;
    }
    
    public void set(int index, Pointer pointer, int len) {
        for (int i = 0; i < len; i++) {
            set(index + i, pointer.get(i));
        }
    }
    
    public void set(Pointer pointer, int len) {
        set(0, pointer, len);
    }
}