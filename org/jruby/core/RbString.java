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
public class RbString {
    public static RubyClass createStringClass(Ruby ruby) {
        RubyClass stringClass = ruby.defineClass("String", ruby.getClasses().getObjectClass());
        
        stringClass.includeModule(ruby.getRubyClass("Comparable"));
        stringClass.includeModule(ruby.getRubyClass("Enumerable"));
        
        stringClass.defineSingletonMethod("new", getSingletonMethod("m_new", true));
        stringClass.defineMethod("initialize", getMethod("m_replace", RubyObject.class));
        stringClass.defineMethod("clone", getMethod("m_clone", false));
        stringClass.defineMethod("dup", getMethod("m_dup", false));
        
        stringClass.defineMethod("<=>", getMethod("op_cmp", RubyObject.class));
        stringClass.defineMethod("==", getMethod("m_equal", RubyObject.class));
        stringClass.defineMethod("===", getMethod("m_equal", RubyObject.class));
        stringClass.defineMethod("eql?", getMethod("m_equal", RubyObject.class));
        
        stringClass.defineMethod("hash", getMethod("m_hash", false));
        stringClass.defineMethod("+", getMethod("op_plus", RubyObject.class));
        stringClass.defineMethod("*", getMethod("op_mul", RubyInteger.class));
        stringClass.defineMethod("%", getMethod("m_format", RubyObject.class));
        stringClass.defineMethod("[]", getMethod("m_aref", true));
        stringClass.defineMethod("[]=", getMethod("m_aset", true));
        stringClass.defineMethod("length", getMethod("m_length", false));
        stringClass.defineMethod("size", getMethod("m_length", false));
        stringClass.defineMethod("empty?", getMethod("m_empty", false));
        stringClass.defineMethod("=~", getMethod("m_match", RubyObject.class));
        stringClass.defineMethod("~", getMethod("m_match2", false));
        stringClass.defineMethod("succ", getMethod("m_succ", false));
        stringClass.defineMethod("succ!", getMethod("m_succ_bang", false));
        stringClass.defineMethod("next", getMethod("m_succ", false));
        stringClass.defineMethod("next!", getMethod("m_succ_bang", false));
        stringClass.defineMethod("upto", getMethod("m_upto", RubyObject.class));
        stringClass.defineMethod("index", getMethod("m_index", true));
        stringClass.defineMethod("rindex", getMethod("m_rindex", true));
        stringClass.defineMethod("replace", getMethod("m_replace", RubyObject.class));

//    rb_define_method(rb_cString, "to_i", rb_str_to_i, 0);

        
        stringClass.defineMethod("to_s", getMethod("m_to_s", false));
        stringClass.defineMethod("to_str", getMethod("m_to_s", false));
        stringClass.defineMethod("inspect", getMethod("m_inspect", false));
//    rb_define_method(rb_cString, "dump", rb_str_dump, 0);

        stringClass.defineMethod("upcase", getMethod("m_upcase", false));
        stringClass.defineMethod("downcase", getMethod("m_downcase", false));
        stringClass.defineMethod("capitalize", getMethod("m_capitalize", false));
        stringClass.defineMethod("swapcase", getMethod("m_swapcase", false));

        stringClass.defineMethod("upcase!", getMethod("m_upcase_bang", false));
        stringClass.defineMethod("downcase!", getMethod("m_downcase_bang", false));
        stringClass.defineMethod("capitalize!", getMethod("m_capitalize_bang", false));
        stringClass.defineMethod("swapcase!", getMethod("m_swapcase_bang", false));

//    rb_define_method(rb_cString, "hex", rb_str_hex, 0);
//    rb_define_method(rb_cString, "oct", rb_str_oct, 0);
//    rb_define_method(rb_cString, "split", rb_str_split_m, -1);*/
        stringClass.defineMethod("reverse", getMethod("m_reverse", false));
        stringClass.defineMethod("reverse!", getMethod("m_reverse_bang", false));
        stringClass.defineMethod("concat", getMethod("m_concat", RubyObject.class));
        stringClass.defineMethod("<<", getMethod("m_concat", RubyObject.class));
//    rb_define_method(rb_cString, "crypt", rb_str_crypt, 1);
//    rb_define_method(rb_cString, "intern", rb_str_intern, 0);

//    rb_define_method(rb_cString, "include?", rb_str_include, 1);

//    rb_define_method(rb_cString, "scan", rb_str_scan, 1);

//    rb_define_method(rb_cString, "ljust", rb_str_ljust, 1);
//    rb_define_method(rb_cString, "rjust", rb_str_rjust, 1);
//    rb_define_method(rb_cString, "center", rb_str_center, 1);*/

        stringClass.defineMethod("sub", getMethod("m_sub", true));
        stringClass.defineMethod("gsub", getMethod("m_gsub", true));
//    rb_define_method(rb_cString, "chop", rb_str_chop, 0);
//    rb_define_method(rb_cString, "chomp", rb_str_chomp, -1);
//    rb_define_method(rb_cString, "strip", rb_str_strip, 0);*/

        stringClass.defineMethod("sub!", getMethod("m_sub_bang", true));
        stringClass.defineMethod("gsub!", getMethod("m_gsub_bang", true));
//    rb_define_method(rb_cString, "strip!", rb_str_strip_bang, 0);
//    rb_define_method(rb_cString, "chop!", rb_str_chop_bang, 0);
//    rb_define_method(rb_cString, "chomp!", rb_str_chomp_bang, -1);

//    rb_define_method(rb_cString, "tr", rb_str_tr, 2);
//    rb_define_method(rb_cString, "tr_s", rb_str_tr_s, 2);
//    rb_define_method(rb_cString, "delete", rb_str_delete, -1);
//    rb_define_method(rb_cString, "squeeze", rb_str_squeeze, -1);
//    rb_define_method(rb_cString, "count", rb_str_count, -1);

//    rb_define_method(rb_cString, "tr!", rb_str_tr_bang, 2);
//    rb_define_method(rb_cString, "tr_s!", rb_str_tr_s_bang, 2);
//    rb_define_method(rb_cString, "delete!", rb_str_delete_bang, -1);
//    rb_define_method(rb_cString, "squeeze!", rb_str_squeeze_bang, -1);

//    rb_define_method(rb_cString, "each_line", rb_str_each_line, -1);
//    rb_define_method(rb_cString, "each", rb_str_each_line, -1);
//    rb_define_method(rb_cString, "each_byte", rb_str_each_byte, 0);

//    rb_define_method(rb_cString, "sum", rb_str_sum, -1);

//    rb_define_global_function("sub", rb_f_sub, -1);
//    rb_define_global_function("gsub", rb_f_gsub, -1);

//    rb_define_global_function("sub!", rb_f_sub_bang, -1);
//    rb_define_global_function("gsub!", rb_f_gsub_bang, -1);

//    rb_define_global_function("chop", rb_f_chop, 0);
//    rb_define_global_function("chop!", rb_f_chop_bang, 0);

//    rb_define_global_function("chomp", rb_f_chomp, -1);
//    rb_define_global_function("chomp!", rb_f_chomp_bang, -1);

//    rb_define_global_function("split", rb_f_split, -1);
//    rb_define_global_function("scan", rb_f_scan, 1);

    stringClass.defineMethod("slice", getMethod("m_aref", true));
    stringClass.defineMethod("slice!", getMethod("m_slice_bang", true));

//    id_to_s = rb_intern("to_s");

//    rb_fs = Qnil;
//    rb_define_hooked_variable("$;", &rb_fs, 0, rb_str_setter);
//    rb_define_hooked_variable("$-F", &rb_fs, 0, rb_str_setter);
        
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
