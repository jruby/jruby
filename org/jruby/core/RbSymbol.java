/*
 * RbSymbol.java - No description
 * Created on 20. September 2001, 15:04
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

package org.jruby.core;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 */
public class RbSymbol {
    public static RubyClass createSymbolClass(Ruby ruby) {
        RubyClass symbolClass = ruby.defineClass("Symbol", ruby.getClasses().getObjectClass());
        
        symbolClass.getRubyClass().undefMethod("new");
        symbolClass.defineMethod("to_i", getMethod("m_to_i"));
        symbolClass.defineMethod("to_int", getMethod("m_to_i"));
        symbolClass.defineMethod("inspect", getMethod("m_inspect"));
        symbolClass.defineMethod("to_s", getMethod("m_to_s"));
        symbolClass.defineMethod("id2name", getMethod("m_to_s"));
        symbolClass.defineMethod("hash", getMethod("m_hash"));
        symbolClass.defineMethod("==", getMethod("m_equal", RubyObject.class));
        
        return symbolClass;
    }
    
    public static RubyCallbackMethod getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubySymbol.class, methodName);
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubySymbol.class, methodName, arg1);
    }
}