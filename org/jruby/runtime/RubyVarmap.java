/*
 * RubyVarmap.java - No description
 * Created on 10. September 2001, 17:54
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

import java.util.*;

import org.jruby.*;
// import org.jruby.original.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyVarmap {
    private String      id          = null;
    private RubyObject  val         = null;
    private RubyVarmap  next        = null;
    
    private static Map oldMap       = new HashMap();

    /** Creates new RubyVarmap */
    public RubyVarmap(String id, RubyObject val, RubyVarmap next) {
        this.id             = id;
        this.val            = val;
        this.next           = next;
    }
    
    public RubyVarmap() {
    }
    
    /** PUSH_VARS
     *
     */
    public static void push(Ruby ruby) {
        ruby.varMapStack.push(ruby.getDynamicVars());
        ruby.setDynamicVars(null);
    }
    
    /** POP_VARS
     *
     */
    public static void pop(Ruby ruby) {
        ruby.setDynamicVars((RubyVarmap)ruby.varMapStack.pop());
    }
    
    /*public void push() {
        RubyVarmap varMap = new RubyVarmap(id, val, next);
        next = varMap;
    }
    
    public void pop() {
        if (next != null) {
            id = next.id;
            val = next.val;
            next = next.next;
        }
    }*/
    
    /** Getter for property id.
     * @return Value of property id.
     */
    public String getId() {
        return id;
    }
    
    /** Setter for property id.
     * @param id New value of property id.
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /** Getter for property next.
     * @return Value of property next.
     */
    public RubyVarmap getNext() {
        return next;
    }
    
    /** Setter for property next.
     * @param next New value of property next.
     */
    public void setNext(RubyVarmap next) {
        this.next = next;
    }
    
    /** Getter for property val.
     * @return Value of property val.
     */
    public RubyObject getVal() {
        return val;
    }
    
    /** Setter for property val.
     * @param val New value of property val.
     */
    public void setVal(RubyObject val) {
        this.val = val;
    }

    /** rb_dvar_defined
     *
     */
    public static boolean isDefined(Ruby ruby, String rubyId) {
        RubyVarmap vars = ruby.getDynamicVars();
        
        while (vars != null) {
            if (rubyId.equals(vars.id)) {
                return true;
            }
            vars = vars.next;
        }
        
        return false;
    }
    
    /** rb_dvar_curr
     *
     */
    public static boolean isCurrent(Ruby ruby, String rubyId) {
        RubyVarmap vars = ruby.getDynamicVars();
        
        while (vars != null) {
            if (vars.id == null) {
                break;
            }
            if (vars.id == rubyId) {
                return true;
            }
            vars = vars.next;
        }
        return false;
    }

    /** rb_dvar_ref
     *
     */
    public RubyObject getRef(Ruby ruby, String rubyId) {
        if (rubyId.equals(id)) {
            return getVal();
        } else if (next != null) {
            return next.getRef(ruby, rubyId);
        }
        return ruby.getNil();
    }
    
    /** rb_dvar_push
     *
     */
    public static void push(Ruby ruby, String rubyId, RubyObject val) {
        ruby.setDynamicVars(new RubyVarmap(rubyId, val, ruby.getDynamicVars()));
    }
    
    /** dvar_asgn_internal
     *
     */
    public static RubyVarmap assignInternal(RubyVarmap varMap, String id, RubyObject value, boolean current) {
        int n = 0;
        RubyVarmap tmpMap = varMap;

        while (tmpMap != null) {
            if (current && tmpMap.getId() == null) {
                n++;
                if (n == 2) {
                    break;
                }
            }
            if (id.equals(tmpMap.getId())) {
                tmpMap.setVal(value);
                return varMap;
            }
            tmpMap = tmpMap.getNext();
        }
        if (varMap == null) {
            return new RubyVarmap(id, value, null);
        } else {
            tmpMap = new RubyVarmap(id, value, varMap.getNext());
            varMap.setNext(tmpMap);
            return varMap;
        }
    }
    
    /** dvar_asgn
     *
     */
    public static void assign(Ruby ruby, String id, RubyObject value) {
        ruby.setDynamicVars(assignInternal(ruby.getDynamicVars(), id, value, false));
    }
    
    /** dvar_asgn_curr
     *
     */
    public static void assignCurrent(Ruby ruby, String id, RubyObject value) {
        ruby.setDynamicVars(assignInternal(ruby.getDynamicVars(), id, value, true));
    }
}