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

import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyStack {
    private List elements;

    public RubyStack() {
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
}
