/*
 * ShiftableList.java - No description
 * Created on 15. September 2001, 22:03
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

/** Shiftable list implementation of the List interface.
 *
 * In this implementation you can shift the first Element of the List.
 * 
 *
 * @author  jpetersen
 * @version 
 */
public class ShiftableList extends AbstractList {
    private List delegate;
    
    private int start = 0;

    public ShiftableList(List delegate) {
        this.delegate = delegate;
    }
    
    public ShiftableList(Object[] args) {
        if (args == null) {
            this.delegate = new ArrayList();
        } else {
            this.delegate = new ArrayList(args.length);
            delegate.addAll(Arrays.asList(args));
        }
    }
    
    public void add(int index, Object element) {
        delegate.add(start + index, element);
    }
    
    public Object get(int index) {
        return delegate.get(start + index);
    }
    
    public ShiftableList getList() {
        return getList(0);
    }
    
    public ShiftableList getList(int shift) {
        ShiftableList newList = new ShiftableList(delegate);
        newList.shift(shift);
        return newList;
    }
    
    public Object remove(int index) {
        return delegate.remove(start + index);
    }
    
    public Object set(int index, Object element) {
        return delegate.set(start + index, element);
    }
    
    public void shiftLeft(int shift) {
        if (shift > 0) {
            start -= shift;
            if (start < 0) {
                for (int i = start; i < 0; i++) {
                    delegate.add(0, null);
                }
                start = 0;
            }
        } else if (shift < 0) {
            shift(-shift);
        }
    }
    
    public void shift(int shift) {
        if (shift > 0) {
            start += shift;
            for (int i = delegate.size(); i < start; i++) {
                delegate.add(null);
            }
        } else if (shift < 0) {
            shiftLeft(-shift);
        }
    }
    
    public int size() {
        return delegate.size() - start;
    }
}