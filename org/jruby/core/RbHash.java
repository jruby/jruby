/*
 * RbHash.java - No description
 * Created on 22. November 2001, 14:36
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
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
import org.jruby.exceptions.*;

/**
 *
 * @author  chadfowler
 * @version 
 */
public class RbHash {
    
    public static RubyClass createHashClass(Ruby ruby) {
        RubyClass hashClass = ruby.defineClass("Hash", ruby.getClasses().getObjectClass());
        hashClass.defineSingletonMethod("new", getRestArgsSingletonMethod("m_new"));
        hashClass.defineSingletonMethod("{}", getRestArgsSingletonMethod("m_newHash"));
        hashClass.defineMethod("initialize", getRestArgsMethod("m_initialize"));
     

        return hashClass;
    }
    
    public static RubyCallbackMethod getRestArgsMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyHash.class, methodName, RubyObject[].class, true);
    }
    
    public static RubyCallbackMethod getRestArgsSingletonMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyHash.class, methodName, RubyObject[].class, true, true);
    }
}
