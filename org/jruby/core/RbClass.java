/*
 * RbClass.java - No description
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

package org.jruby.core;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 */
public class RbClass {
    public static void initClassClass(RubyClass classClass) {
        classClass.defineSingletonMethod("new", getSingletonMethod("m_new", true));
        
        classClass.defineMethod("new", getMethod("m_new", true));
        classClass.defineMethod("superclass", getMethod("m_superclass", false));
        
        classClass.defineSingletonMethod("inherited", getSingletonMethod("m_superclass", RubyClass.class));
        
        classClass.undefMethod("module_function");
    }
    
    public static RubyCallbackMethod getMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyClass.class, methodName, RubyObject[].class, true);
        } else {
            return new ReflectionCallbackMethod(RubyClass.class, methodName);
        }
    }
    
    public static RubyCallbackMethod getSingletonMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyClass.class, methodName, RubyObject[].class, true, true);
        } else {
            return new ReflectionCallbackMethod(RubyClass.class, methodName, false, true);
        }
    }
    
    public static RubyCallbackMethod getSingletonMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyClass.class, methodName, arg1, false, true);
    }
}