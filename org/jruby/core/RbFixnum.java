/*
 * RbFixnum.java - No description
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

package org.jruby.core;

import org.jruby.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 */
public class RbFixnum {
    public static RubyClass createFixnum(Ruby ruby) {
        RubyClass fixnumClass = ruby.defineClass("Fixnum", ruby.getIntegerClass());
        
        fixnumClass.defineMethod("to_i", getMethod("m_to_i"));
        fixnumClass.defineMethod("to_s", getMethod("m_to_s"));
        
        fixnumClass.defineMethod("+", getMethod("op_plus", RubyNumeric.class));
        fixnumClass.defineMethod("-", getMethod("op_minus", RubyNumeric.class));
        fixnumClass.defineMethod("*", getMethod("op_mul", RubyNumeric.class));
        fixnumClass.defineMethod("/", getMethod("op_div", RubyNumeric.class));
        fixnumClass.defineMethod("%", getMethod("op_mod", RubyNumeric.class));
        fixnumClass.defineMethod("**", getMethod("op_pow", RubyNumeric.class));
        
        fixnumClass.defineMethod("==", getMethod("op_equal", RubyNumeric.class));
        fixnumClass.defineMethod("<=>", getMethod("op_cmp", RubyNumeric.class));
        fixnumClass.defineMethod(">", getMethod("op_gt", RubyNumeric.class));
        fixnumClass.defineMethod(">=", getMethod("op_ge", RubyNumeric.class));
        fixnumClass.defineMethod("<", getMethod("op_lt", RubyNumeric.class));
        fixnumClass.defineMethod("<=", getMethod("op_le", RubyNumeric.class));
        
        return fixnumClass;
    }
    
    private static RubyCallbackMethod getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyFixnum.class, methodName);
    }
    
    private static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyFixnum.class, methodName, arg1);
    }
}