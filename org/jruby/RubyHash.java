/*
 * RubyHash.java - No description
 * Created on 04. Juli 2001, 22:53
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

package org.jruby;

import java.util.*;

import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyHash extends RubyObject {
    private RubyMap valueMap;
    private RubyObject defaultValue;
    
    public RubyHash(Ruby ruby) {
        this(ruby, new RubyHashMap());
    }

    public RubyHash(Ruby ruby, Map valueMap) {
        this(ruby, new RubyHashMap(), null);
    }
    
    public RubyHash(Ruby ruby, RubyMap valueMap, RubyObject defaultValue) {
        super(ruby, ruby.getRubyClass("Hash"));
        this.valueMap = valueMap;
        this.defaultValue = defaultValue;
    }
    
    public RubyObject getDefautValue() {
        return this.defaultValue;
    }
    
    public void setDefautValue(RubyObject defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public RubyMap getValueMap() {
        return this.valueMap;
    }
    
    public void setValueMap(RubyMap valueMap) {
        this.valueMap = valueMap;
    }
    
    // Hash methods
    
    public static RubyHash m_newHash(Ruby ruby) {
        return m_new(ruby, (RubyClass)ruby.getRubyClass("Hash"), null);
    }
    
    public static RubyHash m_new(Ruby ruby, RubyClass rubyClass, RubyObject[] args) {
        RubyHash hash = new RubyHash(ruby);
        hash.setRubyClass(rubyClass);
        
        hash.callInit(args);
        
        return hash;
    }
    
    public RubyObject m_initialize(RubyObject[] args) {
        if (args.length > 0) {
            // modify();
            
            setDefautValue(args[0]);
        }
        return this;
    }
    
    public RubyObject m_aset(RubyObject key, RubyObject value) {
        // modify();
        
        // HACK +++
        valueMap.put(key, value);
        // HACK ---
            
        return this;
    }
}