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

import java.util.*;

import org.jruby.internal.util.*;
import org.jruby.util.collections.*;
import org.jruby.util.collections.IStack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Stack implements IStack {
    protected LinkedObject top;

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
        return top == null;
    }

    /**
     * @see IStack#peek()
     */
    public Object peek() {
        return top != null ? top.data : null;
    }

    /**
     * @see IStack#pop()
     */
    public Object pop() {
        if (top == null) {
            throw new StackEmptyException("Stack is empty.");
        } else {
            Object data = top.data;
            top = top.next;
            return data;
        }
    }

    /**
     * @see IStack#push(Object)
     */
    public IStack push(Object anObject) {
        top = new LinkedObject(anObject, top);
        return this;
    }

    /**
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        return (obj instanceof Stack) &&
               Utils.isEquals(top, ((Stack)obj).top);
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return Utils.getHashcode(top);
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        LinkedObject next = top;

        StringBuffer sb = new StringBuffer(100);
        sb.append('[');
        while (next != null) {
            sb.append(next);
            if (next.next != null) {
                sb.append(", ");
            }
            next = next.next;
        }
        sb.append(']');
        return super.toString();
    }

    /**
     * @see IStack#iterator()
     */
    public Iterator iterator() {
        return new LinkedObjectIterator(top);
    }
}