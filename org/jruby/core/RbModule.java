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
public class RbModule {
    public static void initModuleClass(RubyClass moduleClass) {
        moduleClass.definePrivateMethod("attr", getMethod("m_attr", RubySymbol.class, true));
        moduleClass.definePrivateMethod("attr_reader", getMethod("m_attr_reader", true));
        moduleClass.definePrivateMethod("attr_writer", getMethod("m_attr_writer", true));
        moduleClass.definePrivateMethod("attr_accessor", getMethod("m_attr_accessor", true));
        
        moduleClass.definePrivateMethod("method_added", getDummyMethod());
    }
    
    public static RubyCallbackMethod getDummyMethod() {
        return new RubyCallbackMethod() {
            public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                return ruby.getNil();
            }
        };
    }
    
    public static RubyCallbackMethod getMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyModule.class, methodName, RubyObject[].class, true);
        } else {
            return new ReflectionCallbackMethod(RubyModule.class, methodName);
        }
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyModule.class, methodName, new Class[] {arg1, RubyObject[].class}, true);
        } else {
            return new ReflectionCallbackMethod(RubyModule.class, methodName, arg1);
        }
    }
    
/*    public static RubyCallbackMethod getSingletonMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyModule.class, methodName, RubyObject[].class, true, true);
        } else {
            return new ReflectionCallbackMethod(RubyModule.class, methodName, false, true);
        }
    }
    
    public static RubyCallbackMethod getSingletonMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyModule.class, methodName, arg1, false, true);
    }*/
}