/*
 * Stack.java - description
 * Created on 22.03.2002, 16:03:17
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
package org.jruby.internal.util.collections;

import org.jruby.util.collections.IStack;
import org.jruby.util.collections.StackEmptyException;
import org.jruby.internal.util.Utils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Stack implements IStack {
    private ArrayList list = new ArrayList();

    /**
     * Constructor for Stack.
     */
    public Stack() {
        super();
    }

    /**
     * @see IStack#isEmpty()
     */
    public boolean isEmpty() {
        return list.size() == 0;
    }

    /**
     * @see IStack#peek()
     */
    public Object peek() {
        synchronized (list) {
            if (isEmpty()) {
                return null;
            }
            return list.get(list.size() - 1);
        }
    }

    protected Object previous() {
        synchronized (list) {
            if (list.size() < 2) {
                return null;
            }
            return list.get(list.size() - 2);
        }
    }

    /**
     * @see IStack#pop()
     */
    public Object pop() {
        synchronized (list) {

            if (isEmpty()) {
                throw new StackEmptyException("Stack is empty.");
            } else {
                return list.remove(list.size() - 1);
            }
        }
    }

    /**
     * @see IStack#push(Object)
     */
    public IStack push(Object anObject) {
        synchronized (list) {
            list.add(anObject);
        }
        return this;
    }

    /**
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        return (obj instanceof Stack) && Utils.isEquals(list, ((Stack) obj).list);
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return Utils.getHashcode(list);
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append('[');
        synchronized (list) {
            for (int i = list.size(); i > 0; i--) {
                sb.append(list.get(i - 1));
                if (i > 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append(']');
        return super.toString();
    }

    /**
     * @see IStack#iterator()
     */
    public Iterator iterator() {
        synchronized (list) {
            ArrayList copy = new ArrayList(list.size());
            for (int i = list.size(); i > 0; i--) {
                copy.add(list.get(i - 1));
            }
            return copy.iterator();
        }
    }
}