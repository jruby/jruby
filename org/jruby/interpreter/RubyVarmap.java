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

import java.util.*;

import org.jruby.*;
import org.jruby.original.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyVarmap {
    private RubyId      id          = null;
    private RubyObject  val         = null;
    private RubyVarmap  next        = null;
    
    private static Map oldMap       = new HashMap();

    /** Creates new RubyVarmap */
    public RubyVarmap(RubyId id, RubyObject val, RubyVarmap next) {
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
        // HACK +++
        if (oldMap.get(ruby) != null) {
            throw new RuntimeException("JRuby - BUG: Need Queue for oldMap in RubyVarmap");
        }
        // HACK ---
        
        oldMap.put(ruby, ruby.getInterpreter().getDynamicVars());
        ruby.getInterpreter().setDynamicVars(null);
    }
    
    /** POP_VARS
     *
     */
    public static void pop(Ruby ruby) {
        ruby.getInterpreter().setDynamicVars((RubyVarmap)oldMap.get(ruby));
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
    public boolean isDefined(RubyId rubyId) {
        if (rubyId.equals(id)) {
            return true;
        } else if (next != null) {
            return next.isDefined(rubyId);
        }
        return false;
    }
    
    /** rb_dvar_curr
     *
     */
    public boolean isCurrent(RubyId rubyId) {
        if (id == null || id.intValue() == 0) {
            return false;
        } else if (rubyId.equals(id)) {
            return true;
        } else if (next != null) {
            return next.isCurrent(rubyId);
        }
        return false;
    }

    /** rb_dvar_ref
     *
     */
    public RubyObject getRef(RubyId rubyId) {
        if (rubyId.equals(id)) {
            return getVal();
        } else if (next != null) {
            return next.getRef(rubyId);
        }
        return rubyId.getRuby().getNil();
    }
    
    /** rb_dvar_push
     *
     */
    public void push(RubyId rubyId, RubyObject value) {
        RubyVarmap varMap = new RubyVarmap(id, val, next);
        setId(rubyId);
        setVal(value);
        setNext(varMap);
    }
    
    /** dvar_asgn_internal
     *
     */
    public static RubyVarmap assignInternal(RubyVarmap varMap, RubyId id, RubyObject value, boolean current) {
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
    public static void assign(Ruby ruby, RubyId id, RubyObject value) {
        ruby.getInterpreter().setDynamicVars(assignInternal(
                ruby.getInterpreter().getDynamicVars(), id, value, false));
    }
    
    /** dvar_asgn_curr
     *
     */
    public static void assignCurrent(Ruby ruby, RubyId id, RubyObject value) {
        ruby.getInterpreter().setDynamicVars(assignInternal(
                ruby.getInterpreter().getDynamicVars(), id, value, true));
    }
}