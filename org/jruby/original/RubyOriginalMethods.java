/*
 * RubyOriginalMethods.java - No description
 * Created on 09. Juli 2001, 21:38
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

package org.jruby.original;

import org.jruby.*;
import org.jruby.core.*;

/**
 *
 * @author  jpetersen
 */
public final class RubyOriginalMethods {
    private Ruby ruby;
    
    public RubyOriginalMethods(Ruby ruby) {
        this.ruby = ruby;
    }
    
    //
    // class.c
    //
    
    public st_table rb_class_tbl() {
        return null;
    }
    
    public VALUE rb_class_new(VALUE superClass) {
        return RubyClass.m_newClass(ruby, (RubyClass)superClass);
    }
    
    public VALUE rb_mod_clone(VALUE mod) {
        return ((RubyModule)mod).m_clone();
    }
    
    public VALUE rb_mod_dup(VALUE mod) {
        return ((RubyModule)mod).m_dup();
    }

    public VALUE rb_singleton_class_new(VALUE superClass) {
        return ((RubyClass)superClass).newSingletonClass();
    }

    public VALUE rb_singleton_class_clone(VALUE klass) {
        return ((RubyClass)klass).getSingletonClassClone();
    }
    
    public void rb_singleton_class_attached(VALUE klass, VALUE obj) {
        ((RubyClass)klass).attachSingletonClass((RubyObject)obj);
    }

    public VALUE rb_define_class_id(ID id, VALUE superClass) {
        return ruby.defineClassId((RubyId)id, (RubyClass)superClass);
    }
    
    public VALUE rb_define_class(String name, VALUE superClass) {
        return ruby.defineClass(name, (RubyClass)superClass);
    }

    public VALUE rb_define_class_under(VALUE outer, String name, VALUE superClass) {
        return ((RubyClass)outer).defineClassUnder(name, (RubyClass)superClass);
    }
    
    public VALUE rb_module_new() {
        return RubyModule.m_newModule(ruby);
    }

    public VALUE rb_define_module_id(ID id) {
        return ruby.defineModuleId((RubyId)id);
    }
    
    public VALUE rb_define_module(String name) {
        return ruby.defineModule(name);
    }

    public VALUE rb_define_module_under(VALUE outer, String name) {
        return ((RubyClass)outer).defineModuleUnder(name);
    }

    public VALUE include_class_new(VALUE module, VALUE superClass) {
        return null;
    }

    public void rb_include_module(VALUE klass, VALUE module) {
        ((RubyClass)klass).includeModule((RubyModule)module);
    }
    
    public VALUE rb_mod_included_modules(VALUE mod) {
        return ((RubyModule)mod).m_included_modules();
    }
    
    public VALUE rb_mod_ancestors(VALUE mod) {
        return ((RubyModule)mod).m_ancestors();
    }
    
    public VALUE rb_class_instance_methods(int argc, VALUE[] argv, VALUE mod) {
        return null;
    }

    public VALUE rb_class_protected_instance_methods(int argc, VALUE[] argv, VALUE mod) {
        return null;
    }

    public VALUE rb_class_private_instance_methods(int argc, VALUE[] argv, VALUE mod) {
        return null;
    }    

    public VALUE rb_obj_singleton_methods(VALUE obj) {
        return null;
    }

    public void rb_define_method_id(VALUE klass, ID name, VALUE func) {
        ((RubyModule)klass).defineMethodId((RubyId)name, (RubyCallbackMethod)func);
    }
    
    public void rb_define_method(VALUE klass, String name, VALUE func) {
        ((RubyModule)klass).defineMethod(name, (RubyCallbackMethod)func);
    }
    
    public void rb_define_protected_method(VALUE klass, String name, VALUE func) {
        ((RubyModule)klass).defineProtectedMethod(name, (RubyCallbackMethod)func);
    }

    public void rb_define_private_method(VALUE klass, String name, VALUE func) {
        ((RubyModule)klass).definePrivateMethod(name, (RubyCallbackMethod)func);
    }

    public void rb_undef_method(VALUE klass, String name) {
        ((RubyClass)klass).undefMethod(name);
    }
    
    public VALUE rb_singleton_class(VALUE obj) {
        return ((RubyObject)obj).getSingletonClass();
    }

    public void rb_define_singleton_method(VALUE obj, String name, VALUE func, int argc) {
    }

    public void rb_define_module_function(VALUE module, String name, VALUE func, int argc) {
    }

    public void rb_define_global_function(String name, VALUE func, int argc) {
    }

    public void rb_define_alias(VALUE klass, String name1, String name2) {
        ((RubyClass)klass).defineAlias(name1, name2);
    }

    public void rb_define_attr(VALUE klass, String name, int read, int write) {
        ((RubyClass)klass).defineAttribute(name, read != 0, write != 0);
    }
    
    //
    // object.c
    //
    
    public VALUE rb_mKernel() {
        return ruby.getKernelModule();
    }
    
    public VALUE rb_cObject() {
        return ruby.getObjectClass();
    }
    
    public VALUE rb_cModule() {
        return ruby.getModuleClass();
    }
    
    public VALUE rb_cClass() {
        return ruby.getClassClass();
    }
    
    public VALUE rb_cData() {
        return null;
    }

    public VALUE rb_cNilClass() {
        return ruby.getNilClass();
    }
    
    public VALUE rb_cTrueClass() {
        return ruby.getTrueClass();
    }
    
    public VALUE rb_cFalseClass() {
        return ruby.getFalseClass();
    }
    
    public VALUE rb_cSymbol() {
        return ruby.getSymbolClass();
    }

    public ID eq() {
        return ruby.intern("==");
    }
    
    public ID eql() {
        return ruby.intern("eql?");
    }
    
    public ID inspect() {
        return ruby.intern("inspect");
    }
    
    /** Change clone to _clone
     *
     */
    public ID _clone() {
        return ruby.intern("clone");
    }

    public VALUE rb_equal(VALUE obj1, VALUE obj2) {
        return null;
    }
    
    public int rb_eql(VALUE obj1, VALUE obj2) {
        return 0;
    }
    
    public VALUE rb_obj_equal(VALUE obj1, VALUE obj2) {
        return ((RubyObject)obj2).m_equal((RubyObject)obj2);
    }

    public VALUE rb_obj_id(VALUE obj) {
        return ((RubyObject)obj).m_id();
    }

    public VALUE rb_obj_type(VALUE obj) {
        return ((RubyObject)obj).m_type();
    }

    public VALUE rb_obj_clone(VALUE obj) {
        return ((RubyObject)obj).m_clone();
    }

    public VALUE rb_obj_dup(VALUE obj) {
        return ((RubyObject)obj).m_dup();
    }

    public VALUE rb_any_to_a(VALUE obj) {
        return null;
    }

    public VALUE rb_any_to_s(VALUE obj) {
        return null;
    }

    public VALUE rb_inspect(VALUE obj) {
        return null;
    }

    public VALUE rb_obj_inspect(VALUE obj) {
        return ((RubyObject)obj).m_inspect();
    }

    public VALUE rb_obj_is_instance_of(VALUE obj, VALUE c) {
        return ((RubyObject)obj).m_instance_of((RubyModule)c);
    }

    public VALUE rb_obj_is_kind_of(VALUE obj, VALUE c) {
        return ((RubyObject)obj).m_kind_of((RubyModule)c);
    }
    
    public VALUE rb_obj_dummy() {
        return ruby.getNil();
    }

    public VALUE rb_obj_tainted(VALUE obj) {
        return ((RubyObject)obj).m_tainted();
    }

    public VALUE rb_obj_taint(VALUE obj) {
        return ((RubyObject)obj).m_taint();
    }
    
    public VALUE rb_obj_untaint(VALUE obj) {
        return ((RubyObject)obj).m_untaint();
    }

    public VALUE rb_obj_freeze(VALUE obj) {
        return ((RubyObject)obj).m_freeze();
    }

    public VALUE rb_obj_frozen_p(VALUE obj) {
        return ((RubyObject)obj).m_frozen();
    }

    public VALUE rb_true(VALUE obj) {
        return ruby.getTrue();
    }
    
    public VALUE rb_false(VALUE obj) {
        return ruby.getFalse();
    }

    public VALUE rb_obj_alloc(VALUE klass) {
        return null;
    }

    public VALUE rb_mod_to_s(VALUE klass) {
        return ((RubyModule)klass).m_to_s();
    }

    public VALUE rb_mod_eqq(VALUE mod, VALUE arg) {
        return ((RubyModule)mod).op_eqq((RubyObject)arg);
    }

    public VALUE rb_mod_le(VALUE mod, VALUE arg) {
        return ((RubyModule)mod).op_le((RubyObject)arg);
    }

    public VALUE rb_mod_lt(VALUE mod, VALUE arg) {
        return ((RubyModule)mod).op_lt((RubyObject)arg);
    }

    public VALUE rb_mod_ge(VALUE mod, VALUE arg) {
        return ((RubyModule)mod).op_ge((RubyObject)arg);
    }

    public VALUE rb_mod_gt(VALUE mod, VALUE arg) {
        return ((RubyModule)mod).op_gt((RubyObject)arg);
    }

    public VALUE rb_mod_cmp(VALUE mod, VALUE arg) {
        return ((RubyModule)mod).op_cmp((RubyObject)arg);
    }

    public VALUE rb_mod_initialize(VALUE[] argv) {
        return ruby.getNil();
    }

    public VALUE rb_module_s_new(VALUE klass) {
        return RubyModule.m_newModule(ruby, (RubyClass)klass);
    }

    public VALUE rb_class_s_new(VALUE[] argv) {
        return null;
    }

    public VALUE rb_class_s_inherited() {
        return null;
    }

    public VALUE rb_class_superclass(VALUE klass) {
        return null;
    }

    public ID rb_to_id(VALUE name) {
        return null;
    }

    public VALUE rb_mod_attr(VALUE[] argv, VALUE klass) {
        return ((RubyModule)klass).m_attr((RubySymbol)argv[0], argv.length > 1 ? (RubyBoolean)argv[1] : ruby.getFalse());
    }

    public VALUE rb_mod_attr_reader(VALUE[] argv, VALUE klass) {
        return ((RubyModule)klass).m_attr_reader((RubyObject[])argv);
    }

    public VALUE rb_mod_attr_writer(VALUE[] argv, VALUE klass) {
        return ((RubyModule)klass).m_attr_writer((RubyObject[])argv);
    }

    public VALUE rb_mod_attr_accessor(VALUE[] argv, VALUE klass) {
        return ((RubyModule)klass).m_attr_accessor((RubyObject[])argv);
    }

    public VALUE rb_mod_const_get(VALUE mod, VALUE name) {
        return ((RubyModule)mod).m_const_get((RubySymbol)name);
    }

    public VALUE rb_mod_const_set(VALUE mod, VALUE name, VALUE value) {
        return ((RubyModule)mod).m_const_set((RubySymbol)name, (RubyObject)value);
    }

    public VALUE rb_mod_const_defined(VALUE mod, VALUE name) {
        return ((RubyModule)mod).m_const_defined((RubySymbol)name);
    }

    public VALUE rb_obj_methods(VALUE obj) {
        return ((RubyObject)obj).m_methods();
    }

    public VALUE rb_obj_protected_methods(VALUE obj) {
        return ((RubyObject)obj).m_protected_methods();
    }

    public VALUE rb_obj_private_methods(VALUE obj) {
        return ((RubyObject)obj).m_private_methods();
    }

    public VALUE rb_convert_type(VALUE val, int type, String tname, String method) {
        return null;
    }

    public VALUE rb_to_integer(VALUE val, String method) {
        return null;
    }

    public VALUE rb_to_int(VALUE val) {
        return null;
    }

    public VALUE rb_Integer(VALUE val) {
        return null;
    }

    public VALUE rb_f_integer(VALUE obj, VALUE arg) {
        return null;
    }

    public VALUE rb_Float(VALUE val) {
        return null;
    }

    public VALUE rb_f_float(VALUE obj, VALUE arg) {
        return null;
    }

    public double rb_num2dbl(VALUE val) {
        return 0;
    };

    public String rb_str2cstr(VALUE str, int len) {
        return null;
    }

    public VALUE rb_String(VALUE val) {
        return null;
    }

    public VALUE rb_f_string(VALUE obj, VALUE arg) {
        return null;
    }

    public VALUE rb_Array(VALUE val) {
        return null;
    }

    public VALUE rb_f_array(VALUE obj, VALUE arg) {
        return null;
    }
    
    //
    // array.c
    //
    
    public VALUE rb_cArray() {
        //return ruby.getArrayClass();
        return null;
    }

    /**
     * @deprecated throw UnsupportedOperationException.
     */
    public void rb_mem_clear(VALUE mem, long size) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated throw UnsupportedOperationException.
     */
    public void memfill(VALUE mem, long size, VALUE val) {
        throw new UnsupportedOperationException();
    }

    public void rb_ary_modify(VALUE ary) {
        ((RubyArray)ary).modify();
    }

    public VALUE rb_ary_freeze(VALUE ary) {
        return rb_obj_freeze(ary);
    }

    public VALUE rb_ary_frozen_p(VALUE ary) {
        return ((RubyArray)ary).m_frozen();
    }

    public VALUE rb_ary_new2(long len) {
        return RubyArray.m_newArray(ruby, len);
    }

    public VALUE rb_ary_new() {
        return RubyArray.m_newArray(ruby);
    }

    public VALUE rb_ary_new3(long n, VALUE[] args) {
        return null;
    }

    public VALUE rb_ary_new4(long n, VALUE elts) {
        return null;
    }

    public VALUE rb_assoc_new(VALUE car, VALUE cdr) {
        return null;
    }

    public VALUE rb_ary_s_new(VALUE[] argv, VALUE klass) {
        return null;
    };

    public VALUE rb_ary_initialize(VALUE[] argv, VALUE ary) {
        return ((RubyArray)ary).m_initialize(argv.length > 0 ? (RubyFixnum)argv[0] : null, 
                                               argv.length > 1 ? (RubyObject)argv[1] : null);
    }

    public VALUE rb_ary_s_create(VALUE[] argv, VALUE klass) {
        return null;
    }

    public void rb_ary_store(VALUE ary, long idx, VALUE val) {
        ((RubyArray)ary).store(idx, (RubyObject)val);
    }

    public VALUE rb_ary_push(VALUE ary, VALUE item) {
        return ((RubyArray)ary).push((RubyObject)item);
    }

    public VALUE rb_ary_push_m(VALUE[] argv, VALUE ary) {
        return ((RubyArray)ary).m_push((RubyObject[])argv);
    }

    public VALUE rb_ary_pop(VALUE ary) {
        return ((RubyArray)ary).m_pop();
    }

    public VALUE rb_ary_shift(VALUE ary) {
        return ((RubyArray)ary).m_shift();
    }

    public VALUE rb_ary_unshift(VALUE ary, VALUE item) {
        return ((RubyArray)ary).unshift((RubyObject)item);
    }

    public VALUE rb_ary_unshift_m(VALUE[] argv, VALUE ary) {
        return ((RubyArray)ary).m_unshift((RubyObject[])argv);
    }

    public VALUE rb_ary_entry(VALUE ary, long offset) {
        return ((RubyArray)ary).entry(offset);
    }

    public VALUE rb_ary_subseq(VALUE ary, long beg, long len) {
        return ((RubyArray)ary).subseq(beg, len);
    }

    public VALUE rb_ary_aref(VALUE[] argv, VALUE ary) {
        return ((RubyArray)ary).m_slice((RubyObject[])argv);
    }

    public VALUE rb_ary_at(VALUE ary, VALUE pos) {
        return null;
    }

    public VALUE rb_ary_first(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_last(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_index(VALUE ary, VALUE val) {
        return null;
    }

    public VALUE rb_ary_rindex(VALUE ary, VALUE val) {
        return null;
    }

    static VALUE rb_ary_indexes(VALUE[] argv, VALUE ary) {
        return null;
    }

    public void rb_ary_replace(VALUE ary, long beg, long len, VALUE rpl) {
    }

    public VALUE rb_ary_aset(VALUE[] argv, VALUE ary) {
        return null;
    }

    public VALUE rb_ary_each(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_each_index(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_reverse_each(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_length(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_empty_p(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_clone(VALUE ary) {
        return null;
    }

    public VALUE to_ary(VALUE ary) {
        return null;
    }
    
    public VALUE rb_output_fs() {
        return null;
    }

    public VALUE inspect_join(VALUE ary, VALUE[] arg) {
        return null;
    }

    public VALUE rb_ary_join(VALUE ary, VALUE sep) {
        return null;
    }

    public VALUE rb_ary_join_m(VALUE[] argv, VALUE ary) {
        return null;
    }

    public VALUE rb_ary_to_s(VALUE ary) {
        return null;
    }

    public VALUE inspect_ensure(VALUE obj) {
        return null;
    }

    public VALUE rb_protect_inspect(VALUE func, VALUE obj, VALUE arg) {
        return null;
    }

    public VALUE rb_inspecting_p(VALUE obj) {
        return null;
    }

    public VALUE inspect_ary(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_inspect(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_to_a(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_reverse(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_reverse_bang(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_reverse_m(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_sort_bang(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_sort(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_collect(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_collect_bang(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_filter(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_delete(VALUE ary, VALUE item) {
        return null;
    }

    public VALUE rb_ary_delete_at(VALUE ary, long pos) {
        return null;
    }

    public VALUE rb_ary_delete_at_m(VALUE ary, VALUE pos) {
        return null;
    }

    public VALUE rb_ary_slice_bang(VALUE[] argv, VALUE ary) {
        return null;
    }

    public VALUE rb_ary_reject_bang(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_delete_if(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_replace_m(VALUE ary, VALUE ary2) {
        return null;
    }

    public VALUE rb_ary_clear(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_fill(VALUE[] argv, VALUE ary) {
        return null;
    }

    public VALUE rb_ary_plus(VALUE x, VALUE y) {
        return null;
    }

    public VALUE rb_ary_concat(VALUE x, VALUE y) {
        return null;
    }

    public VALUE rb_ary_times(VALUE ary, VALUE times) {
        return null;
    }

    public VALUE rb_ary_assoc(VALUE ary, VALUE key) {
        return null;
    }

    public VALUE rb_ary_rassoc(VALUE ary, VALUE value) {
        return null;
    }

    public VALUE rb_ary_equal(VALUE ary1, VALUE ary2) {
        return null;
    }

    public VALUE rb_ary_eql(VALUE ary1, VALUE ary2) {
        return null;
    }

    public VALUE rb_ary_hash(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_includes(VALUE ary, VALUE item) {
        return ((RubyArray)ary).m_includes((RubyObject)item);
    }

    public VALUE rb_ary_cmp(VALUE ary, VALUE ary2) {
        return null;
    }

    public VALUE rb_ary_diff(VALUE ary1, VALUE ary2) {
        return null;
    }

    public VALUE ary_make_hash(VALUE ary1, VALUE ary2) {
        return null;
    }

    public VALUE rb_ary_and(VALUE ary1, VALUE ary2) {
        return null;
    }

    public VALUE rb_ary_or(VALUE ary1, VALUE ary2) {
        return null;
    }

    public VALUE rb_ary_uniq_bang(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_uniq(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_compact_bang(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_compact(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_nitems(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_flatten_bang(VALUE ary) {
        return null;
    }

    public VALUE rb_ary_flatten(VALUE ary) {
        return null;
    }
    
    // variable.c
    
    public VALUE rb_const_get(VALUE clazz, ID id) {
        return ((RubyModule)clazz).getConstant((RubyId)id);
    }
    
    public boolean rb_const_defined(VALUE clazz, ID id) {
        return ((RubyModule)clazz).isConstantDefined((RubyId)id);
    }
}