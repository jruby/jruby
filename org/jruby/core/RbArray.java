/*
 * RbArray.java - No description
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
public class RbArray {
    
    public static RubyClass createArrayClass(Ruby ruby) {
        RubyClass arrayClass = ruby.defineClass("Array", ruby.getClasses().getObjectClass());
        
        //rb_include_module(rb_cArray, rb_mEnumerable);
        
        arrayClass.defineSingletonMethod("new", getRestArgsSingletonMethod("m_new"));
        arrayClass.defineSingletonMethod("[]", getRestArgsSingletonMethod("m_create"));
        arrayClass.defineMethod("initialize", getRestArgsMethod("m_initialize"));
     
        arrayClass.defineMethod("inspect", getMethod("m_inspect"));
/*    rb_define_method(rb_cArray, "to_s", rb_ary_to_s, 0);
    rb_define_method(rb_cArray, "inspect", rb_ary_inspect, 0);
    rb_define_method(rb_cArray, "to_a", rb_ary_to_a, 0);
    rb_define_method(rb_cArray, "to_ary", rb_ary_to_a, 0);
*/
        arrayClass.defineMethod("frozen?", getMethod("m_frozen"));

/*    rb_define_method(rb_cArray, "==", rb_ary_equal, 1);
    rb_define_method(rb_cArray, "eql?", rb_ary_eql, 1);
    rb_define_method(rb_cArray, "hash", rb_ary_hash, 0);
    rb_define_method(rb_cArray, "===", rb_ary_equal, 1);

 */
    arrayClass.defineMethod("[]", getRestArgsMethod("m_slice"));
//    rb_define_method(rb_cArray, "[]=", rb_ary_aset, -1);
    arrayClass.defineMethod("at", getMethod("m_at", RubyFixnum.class));
/*
    rb_define_method(rb_cArray, "first", rb_ary_first, 0);
    rb_define_method(rb_cArray, "last", rb_ary_last, 0);
    rb_define_method(rb_cArray, "concat", rb_ary_concat, 1);
 */
    arrayClass.defineMethod("<<", getMethod("m_push", RubyObject.class));
    arrayClass.defineMethod("push", getRestArgsMethod("m_push"));
    arrayClass.defineMethod("pop", getMethod("m_pop"));
    
    arrayClass.defineMethod("shift", getMethod("m_shift"));
    arrayClass.defineMethod("unshift", getRestArgsMethod("m_unshift"));
/*    rb_define_method(rb_cArray, "each", rb_ary_each, 0);
    rb_define_method(rb_cArray, "each_index", rb_ary_each_index, 0);
    rb_define_method(rb_cArray, "reverse_each", rb_ary_reverse_each, 0);
 */
    arrayClass.defineMethod("length", getMethod("m_length"));
    arrayClass.defineMethod("size", getMethod("m_length"));
/*    rb_define_method(rb_cArray, "empty?", rb_ary_empty_p, 0);
    rb_define_method(rb_cArray, "index", rb_ary_index, 1);
    rb_define_method(rb_cArray, "rindex", rb_ary_rindex, 1);
    rb_define_method(rb_cArray, "indexes", rb_ary_indexes, -1);
    rb_define_method(rb_cArray, "indices", rb_ary_indexes, -1);
    rb_define_method(rb_cArray, "clone", rb_ary_clone, 0);
    rb_define_method(rb_cArray, "join", rb_ary_join_m, -1);
    rb_define_method(rb_cArray, "reverse", rb_ary_reverse_m, 0);
    rb_define_method(rb_cArray, "reverse!", rb_ary_reverse_bang, 0);
 */
        arrayClass.defineMethod("sort", getMethod("m_sort"));
        arrayClass.defineMethod("sort!", getMethod("m_sort_bang"));
        
    /* rb_define_method(rb_cArray, "collect", rb_ary_collect, 0);
    rb_define_method(rb_cArray, "collect!", rb_ary_collect_bang, 0);
    rb_define_method(rb_cArray, "map!", rb_ary_collect_bang, 0);
    rb_define_method(rb_cArray, "filter", rb_ary_filter, 0);
    rb_define_method(rb_cArray, "delete", rb_ary_delete, 1);
    rb_define_method(rb_cArray, "delete_at", rb_ary_delete_at_m, 1);
    rb_define_method(rb_cArray, "delete_if", rb_ary_delete_if, 0);
    rb_define_method(rb_cArray, "reject!", rb_ary_reject_bang, 0);
    rb_define_method(rb_cArray, "replace", rb_ary_replace_m, 1);
    rb_define_method(rb_cArray, "clear", rb_ary_clear, 0);
    rb_define_method(rb_cArray, "fill", rb_ary_fill, -1);
*/
    arrayClass.defineMethod("include?", getMethod("m_includes", RubyObject.class));
    
    /*rb_define_method(rb_cArray, "<=>", rb_ary_cmp, 1);
*/
    arrayClass.defineMethod("slice", getRestArgsMethod("m_slice"));
/*    rb_define_method(rb_cArray, "slice!", rb_ary_slice_bang, -1);

    rb_define_method(rb_cArray, "assoc", rb_ary_assoc, 1);
    rb_define_method(rb_cArray, "rassoc", rb_ary_rassoc, 1);

    rb_define_method(rb_cArray, "+", rb_ary_plus, 1);
    rb_define_method(rb_cArray, "*", rb_ary_times, 1);

    rb_define_method(rb_cArray, "-", rb_ary_diff, 1);
    rb_define_method(rb_cArray, "&", rb_ary_and, 1);
    rb_define_method(rb_cArray, "|", rb_ary_or, 1);

    rb_define_method(rb_cArray, "uniq", rb_ary_uniq, 0);
    rb_define_method(rb_cArray, "uniq!", rb_ary_uniq_bang, 0);
    rb_define_method(rb_cArray, "compact", rb_ary_compact, 0);
    rb_define_method(rb_cArray, "compact!", rb_ary_compact_bang, 0);
    rb_define_method(rb_cArray, "flatten", rb_ary_flatten, 0);
    rb_define_method(rb_cArray, "flatten!", rb_ary_flatten_bang, 0);
    rb_define_method(rb_cArray, "nitems", rb_ary_nitems, 0);*/
        
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
