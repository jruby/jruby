/*
 * RubyGlobalEntry.java - No description
 * Created on 16. September 2001, 17:26
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

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.original.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyGlobalEntry implements global_entry {
    private Ruby                    ruby    = null;
    
    private RubyId                  id      = null;
    private Object                  data    = null;
    
    private RubyGlobalEntryGetter   getter  = null;
    private RubyGlobalEntrySetter   setter  = null;

    private RubyGlobalEntry(Ruby ruby, RubyId id) {
        this.ruby   = ruby;
        this.id     = id;
        
        this.getter = new UndefMethods();
        this.setter = new UndefMethods();
    }
    
    /** Getter for property id.
     * @return Value of property id.
     */
    protected RubyId getId() {
        return id;
    }
    
    /** Setter for property id.
     * @param id New value of property id.
     */
    protected void setId(RubyId id) {
        this.id = id;
    }
    
    /** Getter for property data.
     * @return Value of property data.
     */
    protected Object getData() {
        return data;
    }
    
    /** Setter for property data.
     * @param data New value of property data.
     */
    protected void setData(Object data) {
        this.data = data;
    }
    
    /** Getter for property getter.
     * @return Value of property getter.
     */
    protected RubyGlobalEntryGetter getGetter() {
        return getter;
    }
    
    /** Setter for property getter.
     * @param getter New value of property getter.
     */
    protected void setGetter(RubyGlobalEntryGetter getter) {
        this.getter = getter;
    }
    
    /** Getter for property setter.
     * @return Value of property setter.
     */
    protected RubyGlobalEntrySetter getSetter() {
        return setter;
    }
    
    /** Setter for property setter.
     * @param setter New value of property setter.
     */
    protected void setSetter(RubyGlobalEntrySetter setter) {
        this.setter = setter;
    }

    /** rb_gvar_get
     *
     */
    public RubyObject get() {
        return getGetter().get(getId(), (RubyObject)getData(), this);
    }
    
    /** rb_gvar_set
     *
     */
    public RubyObject set(RubyObject value) {
        if (ruby.getSecurityLevel() >= 4) {
            throw new RubySecurityException("Insecure: can't change global variable value");
        }
        
        getSetter().set(value, getId(), (RubyObject)getData(), this);
        
        return value;
    }
    
    public boolean isDefined() {
        return !(getter instanceof UndefMethods);
    }
    
    /** rb_alias_variable
     *
     */
    public void alias(RubyId newId) {
        if (ruby.getSecurityLevel() >= 4) {
            throw new RubySecurityException("Insecure: can't alias global variable");
        }
        
        RubyGlobalEntry entry = getGlobalEntry(newId);
        entry.data = data;
        entry.getter = getter;
        entry.setter = setter;
    }
    
    /** rb_global_entry
     *
     */
    public static RubyGlobalEntry getGlobalEntry(RubyId id) {
        Ruby ruby = id.getRuby();
        
        RubyGlobalEntry entry = (RubyGlobalEntry)ruby.getGlobalMap().get(id);
        
        if (entry == null) {
            entry = new RubyGlobalEntry(ruby, id);
            ruby.getGlobalMap().put(id, entry);
        }
        
        return entry;
    }
}

class UndefMethods implements RubyGlobalEntryGetter, RubyGlobalEntrySetter {
    public RubyObject get(RubyId id, RubyObject value, RubyGlobalEntry entry) {
        Ruby ruby = id.getRuby();
        
        // HACK +++
        /*if (ruby.isVerbose()) {
            ruby.warn("global variable '" + id.toName() + "' not initialized");
        }*/
        // HACK ---
        
        return ruby.getNil();
    }
    
    public void set(RubyObject value, RubyId id, Object data, RubyGlobalEntry entry) {
        entry.setData(value);
        
        entry.setGetter(new ValueMethods());
        entry.setSetter(new ValueMethods());
    }
}


class ValueMethods implements RubyGlobalEntryGetter, RubyGlobalEntrySetter {
    public RubyObject get(RubyId id, RubyObject value, RubyGlobalEntry entry) {
        return value;
    }
    
    public void set(RubyObject value, RubyId id, Object data, RubyGlobalEntry entry) {
        entry.setData(value);
    }
}
