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

//    rb_include_module(rb_cHash, rb_mEnumerable);

        hashClass.defineSingletonMethod("new", getRestArgsSingletonMethod("m_new"));
        hashClass.defineSingletonMethod("[]", getRestArgsSingletonMethod("m_create"));
        hashClass.defineMethod("initialize", getRestArgsMethod("m_initialize"));
     
//    rb_define_method(rb_cHash,"clone", rb_hash_clone, 0);
//    rb_define_method(rb_cHash,"rehash", rb_hash_rehash, 0);

        hashClass.defineMethod("to_hash", getMethod("m_to_hash"));
        hashClass.defineMethod("to_a", getMethod("m_to_a"));
        hashClass.defineMethod("to_s", getMethod("m_to_s"));
        hashClass.defineMethod("inspect", getMethod("m_inspect"));

//    rb_define_method(rb_cHash,"==", rb_hash_equal, 1);
        hashClass.defineMethod("[]", getMethod("m_aref", RubyObject.class));
//    rb_define_method(rb_cHash,"fetch", rb_hash_fetch, -1);
        hashClass.defineMethod("[]=", getMethod("m_aset", RubyObject.class, RubyObject.class));
        hashClass.defineMethod("store", getMethod("m_aset", RubyObject.class, RubyObject.class));
//    rb_define_method(rb_cHash,"default", rb_hash_default, 0);
//    rb_define_method(rb_cHash,"default=", rb_hash_set_default, 1);
//    rb_define_method(rb_cHash,"index", rb_hash_index, 1);
//    rb_define_method(rb_cHash,"indexes", rb_hash_indexes, -1);
//    rb_define_method(rb_cHash,"indices", rb_hash_indexes, -1);
        hashClass.defineMethod("size", getMethod("m_size"));
        hashClass.defineMethod("length", getMethod("m_size"));
        hashClass.defineMethod("empty?", getMethod("m_empty_p"));

//    rb_define_method(rb_cHash,"each", rb_hash_each_pair, 0);
//    rb_define_method(rb_cHash,"each_value", rb_hash_each_value, 0);
//    rb_define_method(rb_cHash,"each_key", rb_hash_each_key, 0);
//    rb_define_method(rb_cHash,"each_pair", rb_hash_each_pair, 0);
//    rb_define_method(rb_cHash,"sort", rb_hash_sort, 0);

//    rb_define_method(rb_cHash,"keys", rb_hash_keys, 0);
//    rb_define_method(rb_cHash,"values", rb_hash_values, 0);

//    rb_define_method(rb_cHash,"shift", rb_hash_shift, 0);
//    rb_define_method(rb_cHash,"delete", rb_hash_delete, 1);
//    rb_define_method(rb_cHash,"delete_if", rb_hash_delete_if, 0);
//    rb_define_method(rb_cHash,"reject", rb_hash_reject, 0);
//    rb_define_method(rb_cHash,"reject!", rb_hash_reject_bang, 0);
//    rb_define_method(rb_cHash,"clear", rb_hash_clear, 0);
//    rb_define_method(rb_cHash,"invert", rb_hash_invert, 0);
//    rb_define_method(rb_cHash,"update", rb_hash_update, 1);
//    rb_define_method(rb_cHash,"replace", rb_hash_replace, 1);

//    rb_define_method(rb_cHash,"include?", rb_hash_has_key, 1);
//    rb_define_method(rb_cHash,"member?", rb_hash_has_key, 1);
//    rb_define_method(rb_cHash,"has_key?", rb_hash_has_key, 1);
//    rb_define_method(rb_cHash,"has_value?", rb_hash_has_value, 1);
//    rb_define_method(rb_cHash,"key?", rb_hash_has_key, 1);
//    rb_define_method(rb_cHash,"value?", rb_hash_has_value, 1);

        return hashClass;
    }
    
    public static Callback getRestArgsMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyHash.class, methodName, RubyObject[].class, true);
    }
    
    public static Callback getRestArgsSingletonMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyHash.class, methodName, RubyObject[].class, true, true);
    }
    
    public static Callback getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyHash.class, methodName);
    }
    
    public static Callback getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyHash.class, methodName, arg1);
    }
    
    public static Callback getMethod(String methodName, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(RubyHash.class, methodName, new Class[] {arg1, arg2});
    }
}
