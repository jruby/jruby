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
//        rb_define_method(rb_cArray, "empty?", rb_ary_empty_p, 0);
//        rb_define_method(rb_cArray, "index", rb_ary_index, 1);
//        rb_define_method(rb_cArray, "rindex", rb_ary_rindex, 1);
//        rb_define_method(rb_cArray, "indexes", rb_ary_indexes, -1);
//        rb_define_method(rb_cArray, "indices", rb_ary_indexes, -1);
//        rb_define_method(rb_cArray, "clone", rb_ary_clone, 0);
        arrayClass.defineMethod("join", getRestArgsMethod("m_join"));
//        rb_define_method(rb_cArray, "reverse", rb_ary_reverse_m, 0);
//        rb_define_method(rb_cArray, "reverse!", rb_ary_reverse_bang, 0);

        arrayClass.defineMethod("sort", getMethod("m_sort"));
        arrayClass.defineMethod("sort!", getMethod("m_sort_bang"));
        
//        rb_define_method(rb_cArray, "collect", rb_ary_collect, 0);
//        rb_define_method(rb_cArray, "collect!", rb_ary_collect_bang, 0);
//        rb_define_method(rb_cArray, "map!", rb_ary_collect_bang, 0);
//        rb_define_method(rb_cArray, "filter", rb_ary_filter, 0);
//        rb_define_method(rb_cArray, "delete", rb_ary_delete, 1);
//        rb_define_method(rb_cArray, "delete_at", rb_ary_delete_at_m, 1);
//        rb_define_method(rb_cArray, "delete_if", rb_ary_delete_if, 0);
//        rb_define_method(rb_cArray, "reject!", rb_ary_reject_bang, 0);
//        rb_define_method(rb_cArray, "replace", rb_ary_replace_m, 1);
//        rb_define_method(rb_cArray, "clear", rb_ary_clear, 0);
//        rb_define_method(rb_cArray, "fill", rb_ary_fill, -1);

        arrayClass.defineMethod("include?", getMethod("m_includes", RubyObject.class));
    
//        rb_define_method(rb_cArray, "<=>", rb_ary_cmp, 1);

        arrayClass.defineMethod("slice", getRestArgsMethod("m_aref"));
//        rb_define_method(rb_cArray, "slice!", rb_ary_slice_bang, -1);

//        rb_define_method(rb_cArray, "assoc", rb_ary_assoc, 1);
//        rb_define_method(rb_cArray, "rassoc", rb_ary_rassoc, 1);

//        rb_define_method(rb_cArray, "+", rb_ary_plus, 1);
//        rb_define_method(rb_cArray, "*", rb_ary_times, 1);

//        rb_define_method(rb_cArray, "-", rb_ary_diff, 1);
//        rb_define_method(rb_cArray, "&", rb_ary_and, 1);
//        rb_define_method(rb_cArray, "|", rb_ary_or, 1);

//        rb_define_method(rb_cArray, "uniq", rb_ary_uniq, 0);
//        rb_define_method(rb_cArray, "uniq!", rb_ary_uniq_bang, 0);
        arrayClass.defineMethod("compact", getMethod("m_compact"));
        arrayClass.defineMethod("compact!", getMethod("m_compact_bang"));
//        rb_define_method(rb_cArray, "flatten", rb_ary_flatten, 0);
//        rb_define_method(rb_cArray, "flatten!", rb_ary_flatten_bang, 0);
//        rb_define_method(rb_cArray, "nitems", rb_ary_nitems, 0);*/
        
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
