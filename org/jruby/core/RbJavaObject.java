/*
 * RbModule.java - No description
 * Created on 04. Juli 2001, 22:53
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

package org.jruby.core;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 */
public class RbJavaObject {
    public static void defineJavaObjectClass(Ruby ruby) {
        RubyClass javaObjectClass = ruby.defineClass("JavaObject", ruby.getObjectClass());
        
        javaObjectClass.defineMethod("to_s", getMethod("m_to_s"));
        javaObjectClass.defineMethod("eql?", getMethod("m_equal"));
        javaObjectClass.defineMethod("==", getMethod("m_equal"));
        javaObjectClass.defineSingletonMethod("load_class", getSingletonMethod("m_load_class", RubyString.class, true));
        
        javaObjectClass.getRubyClass().undefMethod("new");
        
    }
    
    public static RubyCallbackMethod getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyJavaObject.class, methodName);
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyJavaObject.class, methodName, arg1);
    }
    
    public static RubyCallbackMethod getSingletonMethod(String methodName, Class arg1, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyJavaObject.class, methodName, new Class[] {arg1, RubyObject[].class}, true, true);
        } else {
            return new ReflectionCallbackMethod(RubyJavaObject.class, methodName, false, true);
        }
    }
}