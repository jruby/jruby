/*
 * RbNumeric.java - No description
 * Created on 10. September 2001, 17:56
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
 * @version 
 */
public class RbInteger {
    
    public static RubyClass createIntegerClass(Ruby ruby) {
        RubyClass integerClass = ruby.defineClass("Integer", ruby.getNumericClass());
     
        integerClass.defineMethod("chr", new ReflectionCallbackMethod(RubyInteger.class, "m_chr"));
        integerClass.defineMethod("downto", new ReflectionCallbackMethod(RubyInteger.class, "m_downto", RubyNumeric.class));
        integerClass.defineMethod("integer?", new ReflectionCallbackMethod(RubyInteger.class, "m_int_p"));
        integerClass.defineMethod("next", new ReflectionCallbackMethod(RubyInteger.class, "m_succ"));
        integerClass.defineMethod("step", new ReflectionCallbackMethod(RubyInteger.class, "m_step", new Class[] {RubyNumeric.class, RubyNumeric.class}));
        integerClass.defineMethod("succ", new ReflectionCallbackMethod(RubyInteger.class, "m_succ"));
        integerClass.defineMethod("times", new ReflectionCallbackMethod(RubyInteger.class, "m_times"));
        integerClass.defineMethod("upto", new ReflectionCallbackMethod(RubyInteger.class, "m_upto", RubyNumeric.class));
        
/*    
    rb_define_method(rb_cInteger, "succ", int_succ, 0);
    rb_define_method(rb_cInteger, "next", int_succ, 0);
    rb_define_method(rb_cInteger, "chr", int_chr, 0);
    rb_define_method(rb_cInteger, "to_i", int_to_i, 0);
    rb_define_method(rb_cInteger, "to_int", int_to_i, 0);
    rb_define_method(rb_cInteger, "floor", int_to_i, 0);
    rb_define_method(rb_cInteger, "ceil", int_to_i, 0);
    rb_define_method(rb_cInteger, "round", int_to_i, 0);
    rb_define_method(rb_cInteger, "truncate", int_to_i, 0);*/

        return integerClass;
    }
}