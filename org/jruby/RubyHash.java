/*
 * RubyHash.java - No description
 * Created on 04. Juli 2001, 22:53
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

package org.jruby;

import java.util.*;

import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyHash extends RubyObject {
    private RubyMap valueMap;
    private RubyObject defautValue;
    
    public RubyHash(Ruby ruby) {
        this(ruby, new RubyHashMap());
    }

    public RubyHash(Ruby ruby, Map valueMap) {
        this(ruby, new RubyHashMap(), null);
    }
    
    public RubyHash(Ruby ruby, RubyMap valueMap, RubyObject defaultValue) {
        super(ruby, ruby.getRubyClass("Hash"));
        this.valueMap = valueMap;
        this.defautValue = defautValue;
    }
    
    public RubyObject getDefautValue() {
        return this.defautValue;
    }
    
    public void setDefautValue(RubyObject defautValue) {
        this.defautValue = defautValue;
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