/*
 * RbArray.java - No description
 * Created on 10. September 2001, 17:56
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
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RbArray {
    
    public static RubyClass createArrayClass(Ruby ruby) {
        RubyClass arrayClass = ruby.defineClass("Array", ruby.getClasses().getObjectClass());
        
//        rb_include_module(rb_cArray, rb_mEnumerable);
        
        arrayClass.defineSingletonMethod("new", getRestArgsSingletonMethod("m_new"));
        arrayClass.defineSingletonMethod("[]", getRestArgsSingletonMethod("m_create"));
        arrayClass.defineMethod("initialize", getRestArgsMethod("m_initialize"));
     
        arrayClass.defineMethod("inspect", getMethod("m_inspect"));
        arrayClass.defineMethod("to_s", getMethod("m_to_s"));
        arrayClass.defineMethod("to_a", getMethod("m_to_a"));
        arrayClass.defineMethod("to_ary", getMethod("m_to_a"));
        arrayClass.defineMethod("frozen?", getMethod("m_frozen"));

        arrayClass.defineMethod("==", getMethod("m_equal", RubyObject.class));
        arrayClass.defineMethod("eql?", getMethod("m_eql", RubyObject.class));
//        rb_define_method(rb_cArray, "hash", rb_ary_hash, 0);
        arrayClass.defineMethod("===", getMethod("m_equal", RubyObject.class));

        arrayClass.defineMethod("[]", getRestArgsMethod("m_aref"));
        arrayClass.defineMethod("[]=", getRestArgsMethod("m_aset"));
        arrayClass.defineMethod("at", getMethod("m_at", RubyFixnum.class));

        arrayClass.defineMethod("first", getMethod("m_first"));
        arrayClass.defineMethod("last", getMethod("m_last"));
        arrayClass.defineMethod("concat", getMethod("m_concat", RubyObject.class));

        arrayClass.defineMethod("<<", getMethod("m_push", RubyObject.class));
        arrayClass.defineMethod("push", getRestArgsMethod("m_push"));
        arrayClass.defineMethod("pop", getMethod("m_pop"));
    
        arrayClass.defineMethod("shift", getMethod("m_shift"));
        arrayClass.defineMethod("unshift", getRestArgsMethod("m_unshift"));
        arrayClass.defineMethod("each", getMethod("m_each"));
        arrayClass.defineMethod("each_index", getMethod("m_each_index"));
        arrayClass.defineMethod("reverse_each", getMethod("m_reverse_each"));

        arrayClass.defineMethod("length", getMethod("m_length"));
        arrayClass.defineMethod("size", getMethod("m_length"));
        arrayClass.defineMethod("empty?", getMethod("m_empty_p"));
        arrayClass.defineMethod("index", getMethod("m_index", RubyObject.class));
        arrayClass.defineMethod("rindex", getMethod("m_rindex", RubyObject.class));
        arrayClass.defineMethod("indexes", getRestArgsMethod("m_indexes"));
        arrayClass.defineMethod("indices", getRestArgsMethod("m_indexes"));
        arrayClass.defineMethod("clone", getMethod("m_clone"));
        arrayClass.defineMethod("join", getRestArgsMethod("m_join"));
        arrayClass.defineMethod("reverse", getMethod("m_reverse"));
        arrayClass.defineMethod("reverse!", getMethod("m_reverse_bang"));

        arrayClass.defineMethod("sort", getMethod("m_sort"));
        arrayClass.defineMethod("sort!", getMethod("m_sort_bang"));
        
        arrayClass.defineMethod("collect", getMethod("m_collect"));
        arrayClass.defineMethod("collect!", getMethod("m_collect_bang"));
        arrayClass.defineMethod("map!", getMethod("m_collect_bang"));
        arrayClass.defineMethod("filter", getMethod("m_collect_bang"));
        arrayClass.defineMethod("delete", getMethod("m_delete", RubyObject.class));
        arrayClass.defineMethod("delete_at", getMethod("m_delete_at", RubyObject.class));
        arrayClass.defineMethod("delete_if", getMethod("m_delete_if"));
        arrayClass.defineMethod("reject!", getMethod("m_reject_bang"));
        arrayClass.defineMethod("replace", getMethod("m_replace", RubyObject.class));
        arrayClass.defineMethod("clear", getMethod("m_clear"));
        arrayClass.defineMethod("fill", getRestArgsMethod("m_fill"));

        arrayClass.defineMethod("include?", getMethod("m_includes", RubyObject.class));
    
        arrayClass.defineMethod("<=>", getMethod("op_cmp", RubyObject.class));

        arrayClass.defineMethod("slice", getRestArgsMethod("m_aref"));
        arrayClass.defineMethod("slice!", getRestArgsMethod("m_slice_bang"));

        arrayClass.defineMethod("assoc", getMethod("m_assoc", RubyObject.class));
        arrayClass.defineMethod("rassoc", getMethod("m_rassoc", RubyObject.class));

//        rb_define_method(rb_cArray, "+", rb_ary_plus, 1);
//        rb_define_method(rb_cArray, "*", rb_ary_times, 1);

//        rb_define_method(rb_cArray, "-", rb_ary_diff, 1);
//        rb_define_method(rb_cArray, "&", rb_ary_and, 1);
//        rb_define_method(rb_cArray, "|", rb_ary_or, 1);

//        rb_define_method(rb_cArray, "uniq", rb_ary_uniq, 0);
//        rb_define_method(rb_cArray, "uniq!", rb_ary_uniq_bang, 0);
        arrayClass.defineMethod("compact", getMethod("m_compact"));
        arrayClass.defineMethod("compact!", getMethod("m_compact_bang"));
        arrayClass.defineMethod("flatten", getMethod("m_flatten"));
        arrayClass.defineMethod("flatten!", getMethod("m_flatten_bang"));
        arrayClass.defineMethod("nitems", getMethod("m_nitems"));
        
        return arrayClass;
    }
    
    public static RubyCallbackMethod getRestArgsMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyArray.class, methodName, RubyObject[].class, true);
    }
    
    public static RubyCallbackMethod getRestArgsSingletonMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyArray.class, methodName, RubyObject[].class, true, true);
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyArray.class, methodName, arg1);
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(RubyArray.class, methodName, new Class[] {arg1, arg2});
    }
    
    public static RubyCallbackMethod getMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyArray.class, methodName);
    }
}
