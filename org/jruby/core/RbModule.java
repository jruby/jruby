/*
 * RbModule.java - No description
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
public class RbModule {
    public static void initModuleClass(RubyClass moduleClass) {
        moduleClass.defineMethod("===", getMethod("op_eqq", RubyObject.class, false));
        moduleClass.defineMethod("<=>", getMethod("op_cmp", RubyObject.class, false));
        moduleClass.defineMethod("<", getMethod("op_lt", RubyObject.class, false));
        moduleClass.defineMethod("<=", getMethod("op_le", RubyObject.class, false));
        moduleClass.defineMethod(">", getMethod("op_gt", RubyObject.class, false));
        moduleClass.defineMethod(">=", getMethod("op_ge", RubyObject.class, false));
        
        moduleClass.defineMethod("clone", getMethod("m_clone", false));
        moduleClass.defineMethod("dup", getMethod("m_dup", false));
        moduleClass.defineMethod("to_s", getMethod("m_to_s", false));
        moduleClass.defineMethod("included_modules", getMethod("m_included_modules", false));
        moduleClass.defineMethod("name", getMethod("m_name", false));
        moduleClass.defineMethod("ancestors", getMethod("m_ancestors", false));

        moduleClass.definePrivateMethod("attr", getMethod("m_attr", RubySymbol.class, true));
        moduleClass.definePrivateMethod("attr_reader", getMethod("m_attr_reader", true));
        moduleClass.definePrivateMethod("attr_writer", getMethod("m_attr_writer", true));
        moduleClass.definePrivateMethod("attr_accessor", getMethod("m_attr_accessor", true));
        
        moduleClass.defineSingletonMethod("new", getSingletonMethod("m_new", false));
        moduleClass.defineMethod("initialize", getMethod("m_initialize", true));
        moduleClass.defineMethod("instance_methods", getMethod("m_instance_methods", true));
        moduleClass.defineMethod("public_instance_methods", getMethod("m_instance_methods", true));
        moduleClass.defineMethod("protected_instance_methods", getMethod("m_protected_instance_methods", true));
        moduleClass.defineMethod("private_instance_methods", getMethod("m_private_instance_methods", true));
        
        moduleClass.defineMethod("constants", getMethod("m_constants", false));
        moduleClass.defineMethod("const_get", getMethod("m_const_get", RubySymbol.class, false));
        moduleClass.defineMethod("const_set", getMethod("m_const_set", RubySymbol.class, RubyObject.class));
        moduleClass.defineMethod("const_defined?", getMethod("m_const_defined", RubySymbol.class, false));
        moduleClass.definePrivateMethod("method_added", getDummyMethod());
        moduleClass.defineMethod("class_variables", getMethod("m_class_variables", false));
        moduleClass.definePrivateMethod("remove_class_variable", getMethod("m_remove_class_variable", RubyObject.class, false));
        
        moduleClass.definePrivateMethod("append_features", getMethod("m_remove_append_features", RubyModule.class, false));
        moduleClass.definePrivateMethod("extend_object", getMethod("m_extend_object", RubyObject.class, false));
        moduleClass.definePrivateMethod("include", getMethod("m_include", true));
        moduleClass.definePrivateMethod("public", getMethod("m_public", true));
        moduleClass.definePrivateMethod("protected", getMethod("m_protected", true));
        moduleClass.definePrivateMethod("private", getMethod("m_private", true));
        moduleClass.definePrivateMethod("module_function", getMethod("m_module_function", true));
        
        moduleClass.defineMethod("method_defined?", getMethod("m_method_defined", RubyObject.class, false));
        moduleClass.defineMethod("public_class_method", getMethod("m_public_class_method", true));
        moduleClass.defineMethod("private_class_method", getMethod("m_private_class_method", true));
    /*
    rb_define_method(rb_cModule, "module_eval", rb_mod_module_eval, -1);
    rb_define_method(rb_cModule, "class_eval", rb_mod_module_eval, -1);
	*/
		moduleClass.defineMethod("remove_method", getMethod("remove_method", RubyObject.class, false));
		moduleClass.defineMethod("undef_method", getMethod("undef_method", RubyObject.class, false));
		moduleClass.defineMethod("alias_method", getMethod("alias_method", RubyObject.class, RubyObject.class));
    /*rb_define_private_method(rb_cModule, "define_method", rb_mod_define_method, -1);

    rb_define_singleton_method(rb_cModule, "nesting", rb_mod_nesting, 0);
    rb_define_singleton_method(rb_cModule, "constants", rb_mod_s_constants, 0);*/

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
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(RubyModule.class, methodName, new Class[] {arg1, arg2});
    }
    
    public static RubyCallbackMethod getSingletonMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyModule.class, methodName, RubyObject[].class, true, true);
        } else {
            return new ReflectionCallbackMethod(RubyModule.class, methodName, false, true);
        }
    }
    
    /*public static RubyCallbackMethod getSingletonMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyModule.class, methodName, arg1, false, true);
    }*/
}
