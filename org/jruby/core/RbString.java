/*
 * RbString.java - No description
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
public class RbString {
    private static RubyCallbackMethod singletonMethodNew = null;
    private static RubyCallbackMethod methodInitialize = null;
    
    private static RubyCallbackMethod methodSlice = null;
    private static RubyCallbackMethod methodReverse = null;

    public static RubyClass createStringClass(Ruby ruby) {
        RubyClass stringClass = ruby.defineClass("String", ruby.getObjectClass());
        
        stringClass.includeModule(ruby.getRubyClass("Comparable"));
        // stringClass.includeModule(ruby.getModules().getEnumerable());
        
        stringClass.defineSingletonMethod("new", getSingletonMethod("m_new", true));
        stringClass.defineMethod("initialize", getMethod("m_replace", RubyString.class));
/*    rb_define_method(rb_cString, "clone", rb_str_clone, 0);
    rb_define_method(rb_cString, "dup", rb_str_dup, 0);
    rb_define_method(rb_cString, "<=>", rb_str_cmp_m, 1);
    rb_define_method(rb_cString, "==", rb_str_equal, 1);
    rb_define_method(rb_cString, "===", rb_str_equal, 1);
    rb_define_method(rb_cString, "eql?", rb_str_equal, 1);
    rb_define_method(rb_cString, "hash", rb_str_hash_m, 0);
    rb_define_method(rb_cString, "+", rb_str_plus, 1);
    rb_define_method(rb_cString, "*", rb_str_times, 1);
    rb_define_method(rb_cString, "%", rb_str_format, 1); */
        stringClass.defineMethod("[]", getMethod("m_slice", true));
    /*rb_define_method(rb_cString, "[]=", rb_str_aset_m, -1);
    rb_define_method(rb_cString, "length", rb_str_length, 0);
    rb_define_method(rb_cString, "size", rb_str_length, 0);
    rb_define_method(rb_cString, "empty?", rb_str_empty, 0);
    rb_define_method(rb_cString, "=~", rb_str_match, 1);
    rb_define_method(rb_cString, "~", rb_str_match2, 0);
    rb_define_method(rb_cString, "succ", rb_str_succ, 0);
    rb_define_method(rb_cString, "succ!", rb_str_succ_bang, 0);
    rb_define_method(rb_cString, "next", rb_str_succ, 0);
    rb_define_method(rb_cString, "next!", rb_str_succ_bang, 0);
    rb_define_method(rb_cString, "upto", rb_str_upto_m, 1);
    rb_define_method(rb_cString, "index", rb_str_index_m, -1);
    rb_define_method(rb_cString, "rindex", rb_str_rindex, -1);
    rb_define_method(rb_cString, "replace", rb_str_replace_m, 1);

    rb_define_method(rb_cString, "to_i", rb_str_to_i, 0);
    */
        
        stringClass.defineMethod("to_s", getMethod("m_to_s", false));
        stringClass.defineMethod("to_str", getMethod("m_to_s", false));

    /*
    rb_define_method(rb_cString, "inspect", rb_str_inspect, 0);
    rb_define_method(rb_cString, "dump", rb_str_dump, 0);

    rb_define_method(rb_cString, "upcase", rb_str_upcase, 0);
    rb_define_method(rb_cString, "downcase", rb_str_downcase, 0);
    rb_define_method(rb_cString, "capitalize", rb_str_capitalize, 0);
    rb_define_method(rb_cString, "swapcase", rb_str_swapcase, 0);

    rb_define_method(rb_cString, "upcase!", rb_str_upcase_bang, 0);
    rb_define_method(rb_cString, "downcase!", rb_str_downcase_bang, 0);
    rb_define_method(rb_cString, "capitalize!", rb_str_capitalize_bang, 0);
    rb_define_method(rb_cString, "swapcase!", rb_str_swapcase_bang, 0);

    rb_define_method(rb_cString, "hex", rb_str_hex, 0);
    rb_define_method(rb_cString, "oct", rb_str_oct, 0);
    rb_define_method(rb_cString, "split", rb_str_split_m, -1);*/
        stringClass.defineMethod("reverse", getMethod("m_reverse", false));
    /*rb_define_method(rb_cString, "reverse!", rb_str_reverse_bang, 0);
    rb_define_method(rb_cString, "concat", rb_str_concat, 1);
    rb_define_method(rb_cString, "<<", rb_str_concat, 1);
    rb_define_method(rb_cString, "crypt", rb_str_crypt, 1);
    rb_define_method(rb_cString, "intern", rb_str_intern, 0);

    rb_define_method(rb_cString, "include?", rb_str_include, 1);

    rb_define_method(rb_cString, "scan", rb_str_scan, 1);

    rb_define_method(rb_cString, "ljust", rb_str_ljust, 1);
    rb_define_method(rb_cString, "rjust", rb_str_rjust, 1);
    rb_define_method(rb_cString, "center", rb_str_center, 1);

    rb_define_method(rb_cString, "sub", rb_str_sub, -1);
    rb_define_method(rb_cString, "gsub", rb_str_gsub, -1);
    rb_define_method(rb_cString, "chop", rb_str_chop, 0);
    rb_define_method(rb_cString, "chomp", rb_str_chomp, -1);
    rb_define_method(rb_cString, "strip", rb_str_strip, 0);

    rb_define_method(rb_cString, "sub!", rb_str_sub_bang, -1);
    rb_define_method(rb_cString, "gsub!", rb_str_gsub_bang, -1);
    rb_define_method(rb_cString, "strip!", rb_str_strip_bang, 0);
    rb_define_method(rb_cString, "chop!", rb_str_chop_bang, 0);
    rb_define_method(rb_cString, "chomp!", rb_str_chomp_bang, -1);

    rb_define_method(rb_cString, "tr", rb_str_tr, 2);
    rb_define_method(rb_cString, "tr_s", rb_str_tr_s, 2);
    rb_define_method(rb_cString, "delete", rb_str_delete, -1);
    rb_define_method(rb_cString, "squeeze", rb_str_squeeze, -1);
    rb_define_method(rb_cString, "count", rb_str_count, -1);

    rb_define_method(rb_cString, "tr!", rb_str_tr_bang, 2);
    rb_define_method(rb_cString, "tr_s!", rb_str_tr_s_bang, 2);
    rb_define_method(rb_cString, "delete!", rb_str_delete_bang, -1);
    rb_define_method(rb_cString, "squeeze!", rb_str_squeeze_bang, -1);

    rb_define_method(rb_cString, "each_line", rb_str_each_line, -1);
    rb_define_method(rb_cString, "each", rb_str_each_line, -1);
    rb_define_method(rb_cString, "each_byte", rb_str_each_byte, 0);

    rb_define_method(rb_cString, "sum", rb_str_sum, -1);

    rb_define_global_function("sub", rb_f_sub, -1);
    rb_define_global_function("gsub", rb_f_gsub, -1);

    rb_define_global_function("sub!", rb_f_sub_bang, -1);
    rb_define_global_function("gsub!", rb_f_gsub_bang, -1);

    rb_define_global_function("chop", rb_f_chop, 0);
    rb_define_global_function("chop!", rb_f_chop_bang, 0);

    rb_define_global_function("chomp", rb_f_chomp, -1);
    rb_define_global_function("chomp!", rb_f_chomp_bang, -1);

    rb_define_global_function("split", rb_f_split, -1);
    rb_define_global_function("scan", rb_f_scan, 1);
*/
    stringClass.defineMethod("slice", getMethod("m_slice", true));
    /*rb_define_method(rb_cString, "slice!", rb_str_slice_bang, -1);

    id_to_s = rb_intern("to_s");

    rb_fs = Qnil;
    rb_define_hooked_variable("$;", &rb_fs, 0, rb_str_setter);
    rb_define_hooked_variable("$-F", &rb_fs, 0, rb_str_setter);*/
        
        return stringClass;
    }
    
    public static RubyCallbackMethod getMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyString.class, methodName, RubyObject[].class, true);
        } else {
            return new ReflectionCallbackMethod(RubyString.class, methodName);
        }
    }
    
    public static RubyCallbackMethod getMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyString.class, methodName, arg1);
    }
    
    public static RubyCallbackMethod getSingletonMethod(String methodName, boolean restArgs) {
        if (restArgs) {
            return new ReflectionCallbackMethod(RubyString.class, methodName, RubyObject[].class, true, true);
        } else {
            return new ReflectionCallbackMethod(RubyString.class, methodName, false, true);
        }
    }
    
    public static RubyCallbackMethod getSingletonMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyString.class, methodName, arg1, false, true);
    }
}