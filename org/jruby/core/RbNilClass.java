/*
 * RbNilClass.java - No description
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
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 */
public class RbNilClass {
    public static RubyClass createNilClass(Ruby ruby) {
        RubyClass nilClass = ruby.defineClass("NilClass", ruby.getObjectClass());
        
        nilClass.defineMethod("type", getMethod("m_type"));
        nilClass.defineMethod("to_i", getMethod("m_to_"));
        nilClass.defineMethod("to_s", getMethod("m_to_s"));
        nilClass.defineMethod("to_a", getMethod("m_to_a"));
        nilClass.defineMethod("inspect", getMethod("m_inspect"));
        
        nilClass.defineMethod("&", getMethod("op_and", RubyObject.class));
        nilClass.defineMethod("|", getMethod("op_or", RubyObject.class));
        nilClass.defineMethod("^", getMethod("op_xor", RubyObject.class));
        nilClass.defineMethod("nil?", DefaultCallbackMethods.getMethodTrue());
        
        nilClass.getRubyClass().undefMethod("new");
        
        ruby.defineGlobalConstant("NIL", ruby.getNil());
        
        return nilClass;
    }
    
    public static RubyCallbackMethod getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyNil.class, methodName);
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyNil.class, methodName, arg1);
    }
}