/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.util.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ArrayStack /*implements Cloneable*/ {
    private List elements;

    public ArrayStack() {
        this.elements = new ArrayList();
    }
    
    public void push(Object element) {
        elements.add(element);
    }
    
    public Object pop() {
        return elements.remove(elements.size() - 1);
    }
    
    public Object peek() {
        return elements.get(elements.size() - 1);
    }

    public int depth() {
        return elements.size();
    }

    public Object clone() {
        ArrayStack clone = new ArrayStack();
        clone.elements = new ArrayList(elements);
        return clone;
    }

    public Iterator contents() {
        ArrayList reverseElements = new ArrayList(elements.size());
        for (int i = elements.size() - 1; i >= 0; i--) {
            reverseElements.add(elements.get(i));
        }
        return reverseElements.iterator();
    }
}