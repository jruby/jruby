/*
 * RubyIter.java - No description
 * Created on 17. September 2001, 15:08
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

package org.jruby.runtime;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyIter {
    public static final int ITER_NOT = 0;
    public static final int ITER_PRE = 1;
    public static final int ITER_CUR = 2;
    
    private int iter;
    private int count = 0;
    private RubyIter prev;

    public RubyIter() {
        this(ITER_NOT, null);
    }

    private RubyIter(int iter, RubyIter prev) {
        this.iter = iter;
        this.prev = prev;
    }
    
    public void push(int newIter) {
        if (newIter == iter) {
            count ++;
        } else {
            prev = new RubyIter(iter, prev);
            iter = newIter;
        }
    }
    
    public void pop() {
        if (count > 0) {
            count --;
        } else {
            iter = prev.iter;
            prev = prev.prev;
        }
    }
    
    public int getIter() {
        return iter;
    }
    
    public void setIter(int newIter) {
        if (count > 0) {
            prev = new RubyIter(iter, prev);
            prev.count --;
            count = 0;
        }
        iter = newIter;
    }
}