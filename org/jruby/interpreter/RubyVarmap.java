/*
 * RubyVarmap.java - No description
 * Created on 10. September 2001, 17:54
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby.interpreter;

import org.jruby.*;
import org.jruby.original.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyVarmap {
    private RubyObject  superObject = null;
    private RubyId      id          = null;
    private VALUE       val         = null;
    private RubyVarmap  next        = null;

    /** Creates new RubyVarmap */
    public RubyVarmap(RubyObject superObject, RubyId id, VALUE val, RubyVarmap next) {
        this.superObject    = superObject;
        this.id             = id;
        this.val            = val;
        this.next           = next;
    }
    
    public RubyVarmap() {
    }
    
    public void push() {
        RubyVarmap varMap = new RubyVarmap(superObject, id, val, next);
        next = varMap;
    }
    
    public void pop() {
        if (next != null) {
            superObject = next.superObject;
            id = next.id;
            val = next.val;
            next = next.next;
        }
    }
    
    /** Getter for property id.
     * @return Value of property id.
     */
    public RubyId getId() {
        return id;
    }
    
    /** Setter for property id.
     * @param id New value of property id.
     */
    public void setId(RubyId id) {
        this.id = id;
    }
    
    /** Getter for property next.
     * @return Value of property next.
     */
    public org.jruby.interpreter.RubyVarmap getNext() {
        return next;
    }
    
    /** Setter for property next.
     * @param next New value of property next.
     */
    public void setNext(org.jruby.interpreter.RubyVarmap next) {
        this.next = next;
    }
    
    /** Getter for property superObject.
     * @return Value of property superObject.
     */
    public RubyObject getSuperObject() {
        return superObject;
    }
    
    /** Setter for property superObject.
     * @param superObject New value of property superObject.
     */
    public void setSuperObject(RubyObject superObject) {
        this.superObject = superObject;
    }
    
    /** Getter for property val.
     * @return Value of property val.
     */
    public VALUE getVal() {
        return val;
    }
    
    /** Setter for property val.
     * @param val New value of property val.
     */
    public void setVal(VALUE val) {
        this.val = val;
    }
    
}