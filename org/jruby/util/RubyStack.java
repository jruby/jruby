/*
 * Stack.java - No description
 * Created on 17. September 2001, 15:50
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

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyStack extends AbstractList {
    private List delegate;

    public RubyStack() {
        this(new LinkedList());
    }
    
    public RubyStack(List delegate) {
        super();
        this.delegate = delegate;
    }

    public int size() {
        return delegate.size();
    }    
    
    public Object get(int index) {
        return delegate.get(index);
    }
    
    public void push(Object element) {
        delegate.add(element);
    }
    
    public Object pop() {
        return delegate.remove(delegate.size() - 1);
    }
    
    public Object peek() {
        return delegate.get(delegate.size() - 1);
    }
}