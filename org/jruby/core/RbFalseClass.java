/*
 * RbFalseClass.java - No description
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
public class RbFalseClass {
    public static RubyClass createFalseClass(Ruby ruby) {
        RubyClass falseClass = ruby.defineClass("FalseClass", ruby.getClasses().getObjectClass());
        
        falseClass.defineMethod("to_s", getMethod("m_to_s"));
        falseClass.defineMethod("type", getMethod("m_type"));
        
        falseClass.defineMethod("&", getMethod("op_and", RubyObject.class));
        falseClass.defineMethod("|", getMethod("op_or", RubyObject.class));
        falseClass.defineMethod("^", getMethod("op_xor", RubyObject.class));
        
        falseClass.getRubyClass().undefMethod("new");
        
        ruby.defineGlobalConstant("FALSE", ruby.getFalse());
        
        return falseClass;
    }
    
    public static RubyCallbackMethod getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyBoolean.class, methodName);
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyBoolean.class, methodName, arg1);
    }
}