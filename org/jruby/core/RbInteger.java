/*
 * RbInteger.java - No description
 * Created on 10. September 2001, 17:56
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
 * @version 
 */
public class RbInteger {
    
    public static RubyClass createIntegerClass(Ruby ruby) {
        RubyClass integerClass = ruby.defineClass("Integer", ruby.getClasses().getNumericClass());
     
        integerClass.defineMethod("ceil", getIntegerMethod("m_to_i"));
        integerClass.defineMethod("chr", getIntegerMethod("m_chr"));
        integerClass.defineMethod("downto", getIntegerMethod("m_downto", RubyNumeric.class));
        integerClass.defineMethod("floor", getIntegerMethod("m_to_i"));
        integerClass.defineMethod("integer?", getIntegerMethod("m_int_p"));
        integerClass.defineMethod("next", getIntegerMethod("m_succ"));
        integerClass.defineMethod("round", getIntegerMethod("m_to_i"));
        integerClass.defineMethod("step", getIntegerMethod("m_step", RubyNumeric.class, RubyNumeric.class));
        integerClass.defineMethod("succ", getIntegerMethod("m_succ"));
        integerClass.defineMethod("times", getIntegerMethod("m_times"));
        integerClass.defineMethod("to_i", getIntegerMethod("m_to_i"));
        integerClass.defineMethod("to_int", getIntegerMethod("m_to_i"));
        integerClass.defineMethod("truncate", getIntegerMethod("m_to_i"));
        integerClass.defineMethod("upto", getIntegerMethod("m_upto", RubyNumeric.class));
        
        return integerClass;
    }
    
    public static RubyCallbackMethod getIntegerMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyInteger.class, methodName, arg1);
    }
    
    public static RubyCallbackMethod getIntegerMethod(String methodName, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(RubyInteger.class, methodName, new Class[] {arg1, arg2});
    }
    
    public static RubyCallbackMethod getIntegerMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyInteger.class, methodName);
    }
}