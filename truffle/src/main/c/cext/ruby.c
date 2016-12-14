/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * This file contains code that is based on the Ruby API headers and implementation,
 * copyright (C) Yukihiro Matsumoto, licensed under the 2-clause BSD licence
 * as described in the file BSDL included with JRuby+Truffle.
 */

#include <stdlib.h>
#include <stdarg.h>
#include <stdio.h>

#include <truffle.h>

#include <ruby.h>

// Helpers

VALUE rb_f_notimplement(int args_count, const VALUE *args, VALUE object) {
  rb_jt_error("rb_f_notimplement");
  abort();
}

// Memory

void *rb_alloc_tmp_buffer(VALUE *buffer_pointer, long length) {
  rb_jt_error("rb_alloc_tmp_buffer not implemented");
  abort();
}

void *rb_alloc_tmp_buffer2(VALUE *buffer_pointer, long count, size_t size) {
  rb_jt_error("rb_alloc_tmp_buffer2 not implemented");
  abort();
}

void rb_free_tmp_buffer(VALUE *buffer_pointer) {
  rb_jt_error("rb_free_tmp_buffer not implemented");
  abort();
}

// Types

int rb_type(VALUE value) {
  return truffle_invoke_i(RUBY_CEXT, "rb_type", value);
}

bool RB_TYPE_P(VALUE value, int type) {
  return truffle_invoke_b(RUBY_CEXT, "RB_TYPE_P", value, type);
}

void rb_check_type(VALUE value, int type) {
  truffle_invoke(RUBY_CEXT, "rb_check_type", value);
}

VALUE rb_obj_is_instance_of(VALUE object, VALUE ruby_class) {
  return truffle_invoke(RUBY_CEXT, "rb_obj_is_instance_of", object, ruby_class);
}

VALUE rb_obj_is_kind_of(VALUE object, VALUE ruby_class) {
  return truffle_invoke((void *)object, "kind_of?", ruby_class);
}

void rb_check_frozen(VALUE object) {
  if (OBJ_FROZEN(object)){
    rb_jt_error("rb_check_frozen failure case not implemented");
    abort();
  }
}

void rb_check_safe_obj(VALUE object) {
  rb_jt_error("rb_check_safe_obj not implemented");
  abort();
}

bool SYMBOL_P(VALUE value) {
  return truffle_invoke_b(RUBY_CEXT, "SYMBOL_P", value);
}

// Constants

VALUE rb_jt_get_Qundef(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "Qundef");
}

VALUE rb_jt_get_Qfalse(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "Qfalse");
}

VALUE rb_jt_get_Qtrue(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "Qtrue");
}

VALUE rb_jt_get_Qnil(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "Qnil");
}

VALUE rb_jt_get_cObject(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cObject");
}

VALUE rb_jt_get_cArray(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cArray");
}

VALUE rb_jt_get_cHash(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cHash");
}

VALUE rb_jt_get_cProc(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cProc");
}

VALUE rb_jt_get_cTime(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cTime");
}

VALUE rb_jt_get_mKernel(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_mKernel");
}

VALUE rb_jt_get_mEnumerable(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_mEnumerable");
}

VALUE rb_jt_get_mWaitReadable(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_mWaitReadable");
}

VALUE rb_jt_get_mWaitWritable(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_mWaitWritable");
}

VALUE rb_jt_get_mComparable(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_mComparable");
}

VALUE rb_jt_get_eException(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eException");
}

VALUE rb_jt_get_eRuntimeError(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eRuntimeError");
}

VALUE rb_jt_get_eStandardError(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eStandardError");
}

VALUE rb_jt_get_eNoMemError(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eNoMemError");
}

VALUE rb_jt_get_eTypeError(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eTypeError");
}

VALUE rb_jt_get_eArgError(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eArgError");
}

VALUE rb_jt_get_eRangeError(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eRangeError");
}

VALUE rb_jt_get_eNotImpError(void) {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eNotImpError");
}

// Conversions

VALUE CHR2FIX(char ch) {
  return INT2FIX((unsigned char) ch);
}

int NUM2INT(VALUE value) {
  return truffle_invoke_i(RUBY_CEXT, "NUM2INT", value);
}

unsigned int NUM2UINT(VALUE value) {
  return (unsigned int) truffle_invoke_i(RUBY_CEXT, "NUM2UINT", value);
}

long NUM2LONG(VALUE value) {
  return truffle_invoke_l(RUBY_CEXT, "NUM2LONG", value);
}

unsigned long NUM2ULONG(VALUE value) {
  // TODO CS 24-Jul-16 _invoke_l but what about the unsigned part?
  return truffle_invoke_l(RUBY_CEXT, "NUM2ULONG", value);
}

double NUM2DBL(VALUE value) {
  return truffle_invoke_d(RUBY_CEXT, "NUM2DBL", value);
}

int FIX2INT(VALUE value) {
  return truffle_invoke_i(RUBY_CEXT, "FIX2INT", value);
}

unsigned int FIX2UINT(VALUE value) {
  return (unsigned int) truffle_invoke_i(RUBY_CEXT, "FIX2UINT", value);
}

long FIX2LONG(VALUE value) {
  return truffle_invoke_l(RUBY_CEXT, "FIX2LONG", value);
}

VALUE INT2NUM(long value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "INT2NUM", value);
}

VALUE INT2FIX(long value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "INT2FIX", value);
}

VALUE UINT2NUM(unsigned int value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "UINT2NUM", value);
}

VALUE LONG2NUM(long value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "LONG2NUM", value);
}

VALUE ULONG2NUM(long value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "ULONG2NUM", value);
}

VALUE LONG2FIX(long value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "LONG2FIX", value);
}

int rb_fix2int(VALUE value) {
  return truffle_invoke_i(RUBY_CEXT, "rb_fix2int", value);
}

unsigned long rb_fix2uint(VALUE value) {
  return truffle_invoke_l(RUBY_CEXT, "rb_fix2uint", value);
}

int rb_long2int(long value) {
  return truffle_invoke_l(RUBY_CEXT, "rb_long2int", value);
}

ID SYM2ID(VALUE value) {
  return (ID) value;
}

VALUE ID2SYM(ID value) {
  return (VALUE) value;
}

// Type checks

int RB_NIL_P(VALUE value) {
  return truffle_invoke_b(RUBY_CEXT, "RB_NIL_P", value);
}

int RB_FIXNUM_P(VALUE value) {
  return truffle_invoke_b(RUBY_CEXT, "RB_FIXNUM_P", value);
}

int RTEST(VALUE value) {
  return truffle_invoke_b(RUBY_CEXT, "RTEST", value);
}

// Kernel

VALUE rb_require(const char *feature) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_require", rb_str_new_cstr(feature));
}

// Object

VALUE rb_obj_dup(VALUE object) {
  return (VALUE) truffle_invoke((void *)object, "dup");
}

VALUE rb_jt_obj_taint(VALUE object) {
  return (VALUE) truffle_invoke((void *)object, "taint");
}

bool rb_jt_obj_taintable_p(VALUE object) {
  return truffle_invoke_b(RUBY_CEXT, "RB_OBJ_TAINTABLE", object);
}

bool rb_jt_obj_tainted_p(VALUE object) {
  return truffle_invoke_b((void *)object, "tainted?");
}

VALUE rb_obj_freeze(VALUE object) {
  return (VALUE) truffle_invoke((void *)object, "freeze");
}

bool rb_jt_obj_frozen_p(VALUE object) {
  return truffle_invoke_b((void *)object, "frozen?");
}

// Integer

VALUE rb_Integer(VALUE value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_Integer", value);
}

int rb_integer_pack(VALUE value, void *words, size_t numwords, size_t wordsize, size_t nails, int flags) {
  rb_jt_error("rb_integer_pack not implemented");
  abort();
}

VALUE rb_integer_unpack(const void *words, size_t numwords, size_t wordsize, size_t nails, int flags) {
  rb_jt_error("rb_integer_unpack not implemented");
  abort();
}

size_t rb_absint_size(VALUE value, int *nlz_bits_ret) {
  rb_jt_error("rb_absint_size not implemented");
  abort();
}

VALUE rb_cstr_to_inum(const char* string, int base, int raise) {
  rb_jt_error("rb_cstr_to_inum not implemented");
  abort();
}

// Float

VALUE rb_float_new(double value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_float_new", value);
}

VALUE rb_Float(VALUE value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_Float", value);
}

double RFLOAT_VALUE(VALUE value){
  return truffle_invoke_d(RUBY_CEXT, "RFLOAT_VALUE", value);
}

// String

char *RSTRING_PTR(VALUE string) {
  return (char *)truffle_invoke(RUBY_CEXT, "RSTRING_PTR", string);
}

int rb_str_len(VALUE string) {
  return truffle_invoke_i((void *)string, "bytesize");
}

VALUE rb_str_new(const char *string, long length) {
  if (string == NULL) {
    return (VALUE) truffle_invoke(RUBY_CEXT, "rb_str_new_nul", length);
  } else if (truffle_is_truffle_object((VALUE) string)) {
    return (VALUE) truffle_invoke(RUBY_CEXT, "rb_str_new", string, length);
  } else {
    return (VALUE) truffle_invoke(RUBY_CEXT, "rb_str_new_cstr", truffle_read_n_string(string, length));
  }
}

VALUE rb_str_new_cstr(const char *string) {
  if (truffle_is_truffle_object((VALUE) string)) {
    return (VALUE) truffle_invoke(RUBY_CEXT, "to_ruby_string", string);
  } else {
    return (VALUE) truffle_invoke(RUBY_CEXT, "rb_str_new_cstr", truffle_read_string(string));
  }
}

VALUE rb_intern_str(VALUE string) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_intern_str", string);
}

VALUE rb_str_cat(VALUE string, const char *to_concat, long length) {
  truffle_invoke((void *)string, "concat", rb_str_new(to_concat, length));
  return string;
}

VALUE rb_str_cat2(VALUE string, const char *to_concat) {
  truffle_invoke((void *)string, "concat", rb_str_new_cstr(to_concat));
  return string;
}

VALUE rb_str_to_str(VALUE string) {
  return (VALUE) truffle_invoke((void *)string, "to_str");
}

VALUE rb_str_buf_new(long capacity) {
  return rb_str_new_cstr("");
}

VALUE rb_sprintf(const char *format, ...) {
  va_list args;
  va_start(args, format);
  VALUE *string = rb_vsprintf(format, args);
  va_end(args);
  return string;
}

VALUE rb_vsprintf(const char *format, va_list args) {
  rb_jt_error("rb_vsprintf not implemented");
  abort();
}

VALUE rb_str_append(VALUE string, VALUE to_append) {
  rb_jt_error("rb_str_append not implemented");
  abort();
}

void rb_str_set_len(VALUE string, long length) {
  rb_jt_error("rb_str_set_len not implemented");
  abort();
}

VALUE rb_str_new_frozen(VALUE value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_str_new_frozen", value);
}

VALUE rb_String(VALUE value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_String", value);
}

VALUE rb_str_resize(VALUE string, long length) {
  rb_jt_error("rb_str_resize not implemented");
  abort();
}

VALUE rb_str_split(VALUE string, const char *split) {
  return (VALUE) truffle_invoke(string, "split", rb_str_new_cstr(split));
}

void rb_str_modify(VALUE string) {
  // Does nothing because writing to the string pointer will cause necessary invalidations anyway
}

// Symbol

ID rb_intern(const char *string) {
  return (ID) truffle_invoke(RUBY_CEXT, "rb_intern", rb_str_new_cstr(string));
}

VALUE rb_sym2str(VALUE string) {
  return (VALUE) truffle_invoke((void *)string, "to_str");
}

// Array

int RARRAY_LEN(VALUE array) {
  return truffle_get_size(array);
}

int RARRAY_LENINT(VALUE array) {
  return truffle_get_size(array);
}

VALUE *RARRAY_PTR(VALUE array) {
  return (VALUE*) truffle_invoke(RUBY_CEXT, "RARRAY_PTR", array);
}

VALUE RARRAY_AREF(VALUE array, long index) {
  return truffle_read_idx(array, (int) index);
}

VALUE rb_Array(VALUE array) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_Array", array);
}

VALUE rb_ary_new() {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_ary_new");
}

VALUE rb_ary_new_capa(long capacity) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_ary_new_capa", capacity);
}

VALUE rb_ary_new_from_args(long n, ...) {
  VALUE array = rb_ary_new_capa(n);
  for (int i = 0; i < n; i++) {
    rb_ary_store(array, i, (VALUE) truffle_get_arg(1+i));
  }
  return array;
}

VALUE rb_ary_new4(long n, const VALUE *values) {
  VALUE array = rb_ary_new_capa(n);
  for (int i = 0; i < n; i++) {
    rb_ary_store(array, i, values[i]);
  }
  return array;
}

VALUE rb_ary_push(VALUE array, VALUE value) {
  truffle_invoke((void *)array, "push", value);
  return array;
}

VALUE rb_ary_pop(VALUE array) {
  return (VALUE) truffle_invoke((void *)array, "pop");
}

void rb_ary_store(VALUE array, long index, VALUE value) {
  truffle_write_idx(array, (int) index, value);
}

VALUE rb_ary_entry(VALUE array, long index) {
  return truffle_read_idx(array, (int) index);
}

VALUE rb_ary_dup(VALUE array) {
  return (VALUE) truffle_invoke((void *)array, "dup");
}

VALUE rb_ary_each(VALUE array) {
  rb_jt_error("rb_ary_each not implemented");
  abort();
}

VALUE rb_ary_unshift(VALUE array, VALUE value) {
  return (VALUE) truffle_invoke((void *)array, "unshift", value);
}

VALUE rb_check_array_type(VALUE array) {
  rb_jt_error("rb_check_array_type not implemented");
  abort();
}

// Hash

VALUE rb_hash_new() {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_hash_new");
}

VALUE rb_hash_aref(VALUE hash, VALUE key) {
  return truffle_read(hash, key);
}

VALUE rb_hash_aset(VALUE hash, VALUE key, VALUE value) {
  return (VALUE) truffle_invoke((void *)hash, "[]=", key, value);
  return value;
}

VALUE rb_hash_lookup(VALUE hash, VALUE key) {
  return (VALUE) truffle_invoke((void *)hash, "[]", key);
}

VALUE rb_hash_lookup2(VALUE hash, VALUE key, VALUE default_value) {
  return (VALUE) truffle_invoke((void *)hash, "fetch", key, default_value);
}

VALUE rb_hash_set_ifnone(VALUE hash, VALUE if_none) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_hash_set_ifnone", hash, if_none);
}

st_index_t rb_memhash(const void *data, long length) {
  // Not a proper hash - just something that produces a stable result for now

  long hash = 0;

  for (long n = 0; n < length; n++) {
    hash = (hash << 1) ^ ((uint8_t*) data)[n];
  }

  return (st_index_t) hash;
}

// Class

const char* rb_class2name(VALUE module) {
  return RSTRING_PTR(truffle_invoke((void *)module, "name"));
}

VALUE rb_class_real(VALUE ruby_class) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_class_real", ruby_class);
}

VALUE rb_class_superclass(VALUE ruby_class) {
  return (VALUE) truffle_invoke((void *)ruby_class, "superclass");
}

VALUE rb_obj_class(VALUE object) {
  return rb_class_real(rb_class_of(object));
}

VALUE CLASS_OF(VALUE object) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "CLASS_OF", object);
}

VALUE rb_class_of(VALUE object) {
  return (VALUE) truffle_invoke((void *)object, "class");
}

VALUE rb_obj_alloc(VALUE ruby_class) {
  return (VALUE) truffle_invoke((void *)ruby_class, "allocate");
}

VALUE rb_class_path(VALUE ruby_class) {
  rb_jt_error("rb_class_path not implemented");
  abort();
}

VALUE rb_path2class(const char *string) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_path2class", rb_str_new_cstr(string));
}

// Proc

VALUE rb_proc_new(void *function, VALUE value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_proc_new", truffle_address_to_function(function), value);
}

// Utilities

void rb_warn(const char *format, ...) {
  if (!truffle_invoke_b(truffle_invoke(RUBY_CEXT, "verbose"), "nil?")) {
    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
  }
}

void rb_warning(const char *format, ...) {
  if (truffle_invoke(RUBY_CEXT, "verbose") == Qtrue) {
    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
  }
}

int rb_scan_args(int argc, VALUE *argv, const char *format, ...) {
  rb_jt_error("generic rb_scan_args not implemented - use a specialisation such as rb_jt_scan_args_02");
  abort();
}

// Calls

int rb_respond_to(VALUE object, ID name) {
  return truffle_invoke_b((void *)object, "respond_to?", name);
}

VALUE rb_funcallv(VALUE object, ID name, int args_count, const VALUE *args) {
  rb_jt_error("rb_funcallv not implemented");
  abort();
}

VALUE rb_funcallv_public(VALUE object, ID name, int args_count, const VALUE *args) {
  rb_jt_error("rb_funcallv_public not implemented");
  abort();
}

VALUE rb_apply(VALUE object, ID name, VALUE args) {
  rb_jt_error("rb_apply not implemented");
  abort();
}

VALUE rb_block_call(VALUE object, ID name, int args_count, const VALUE *args, rb_block_call_func_t block_call_func, VALUE data) {
  rb_jt_error("rb_block_call not implemented");
  abort();
}

VALUE rb_call_super(int args_count, const VALUE *args) {
  rb_jt_error("rb_call_super not implemented");
  abort();
}

int rb_block_given_p() {
  return truffle_invoke_i(RUBY_CEXT, "rb_block_given_p");
}

VALUE rb_block_proc(void) {
  rb_jt_error("rb_block_proc not implemented");
  abort();
}

VALUE rb_yield(VALUE value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_yield", value);
}

// Instance variables

VALUE rb_iv_get(VALUE object, const char *name) {
  return truffle_invoke(RUBY_CEXT, "rb_ivar_get", object, rb_str_new_cstr(name));
}

VALUE rb_iv_set(VALUE object, const char *name, VALUE value) {
  truffle_invoke(RUBY_CEXT, "rb_ivar_set", object, rb_str_new_cstr(name), value);
  return value;
}

VALUE rb_ivar_get(VALUE object, ID name) {
  return truffle_invoke(RUBY_CEXT, "rb_ivar_get", object, name);
}

VALUE rb_ivar_set(VALUE object, ID name, VALUE value) {
  truffle_invoke(RUBY_CEXT, "rb_ivar_set", object, name, value);
  return value;
}

VALUE rb_ivar_lookup(VALUE object, const char *name, VALUE default_value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_ivar_lookup", name, default_value);
}

VALUE rb_attr_get(VALUE object, const char *name) {
  return rb_ivar_lookup((void *)object, name, Qnil);
}

// Accessing constants

int rb_const_defined(VALUE module, ID name) {
  return truffle_invoke_b((void *)module, "const_defined?", name);
}

int rb_const_defined_at(VALUE module, ID name) {
  return truffle_invoke_b((void *)module, "const_defined?", name, Qfalse);
}

VALUE rb_const_get(VALUE module, ID name) {
  return (VALUE) truffle_invoke((void *)module, "const_get", name);
}

VALUE rb_const_get_at(VALUE module, ID name) {
  return (VALUE) truffle_invoke((void *)module, "const_get", name, Qfalse);
}

VALUE rb_const_get_from(VALUE module, ID name) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_const_get_from", module, name);
}

VALUE rb_const_set(VALUE module, ID name, VALUE value) {
  return (VALUE) truffle_invoke((void *)module, "const_set", name, value);
}

VALUE rb_define_const(VALUE module, const char *name, VALUE value) {
  return rb_const_set(module, rb_str_new_cstr(name), value);
}

void rb_define_global_const(const char *name, VALUE value) {
  rb_define_const(rb_cObject, name, value);
}

// Raising exceptions

VALUE rb_exc_new3(VALUE exception_class, VALUE message) {
  return (VALUE) truffle_invoke((void *)exception_class, "new", message);
}

void rb_exc_raise(VALUE exception) {
  truffle_invoke(RUBY_CEXT, "rb_exc_raise", exception);
  abort();
}

void rb_raise(VALUE exception, const char *format, ...) {
  rb_jt_error("rb_raise not implemented");
  truffle_invoke(RUBY_CEXT, "rb_raise", format /*, where to get args? */);
  abort();
}

VALUE rb_protect(VALUE (*function)(VALUE), VALUE data, int *status) {
  // TODO CS 23-Jul-16
  return function(data);
}

void rb_jump_tag(int status) {
  if (status) {
    rb_jt_error("rb_jump_tag not implemented");
    abort();
  }
}

void rb_set_errinfo(VALUE error) {
  rb_jt_error("rb_set_errinfo not implemented");
  abort();
}

void rb_syserr_fail(int errno, const char *message) {
  rb_jt_error(message);
  abort();
}

void rb_sys_fail(const char *message) {
  rb_jt_error(message);
  abort();
}

// Defining classes, modules and methods

VALUE rb_define_class(const char *name, VALUE superclass) {
  return rb_define_class_under(rb_cObject, name, superclass);
}

VALUE rb_define_class_under(VALUE module, const char *name, VALUE superclass) {
  return rb_define_class_id_under(module, rb_str_new_cstr(name), superclass);
}

VALUE rb_define_class_id_under(VALUE module, ID name, VALUE superclass) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_define_class_under", module, name, superclass);
}

VALUE rb_define_module(const char *name) {
  return rb_define_module_under(rb_cObject, name);
}

VALUE rb_define_module_under(VALUE module, const char *name) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_define_module_under", module, rb_str_new_cstr(name));
}

void rb_include_module(VALUE module, VALUE to_include) {
  truffle_invoke((void *)module, "include", to_include);
}

void rb_define_method(VALUE module, const char *name, void *function, int argc) {
  truffle_invoke(RUBY_CEXT, "rb_define_method", module, rb_str_new_cstr(name), truffle_address_to_function(function), argc);
}

void rb_define_private_method(VALUE module, const char *name, void *function, int argc) {
  truffle_invoke(RUBY_CEXT, "rb_define_private_method", module, rb_str_new_cstr(name), truffle_address_to_function(function), argc);
}

void rb_define_protected_method(VALUE module, const char *name, void *function, int argc) {
  truffle_invoke(RUBY_CEXT, "rb_define_protected_method", module, rb_str_new_cstr(name), truffle_address_to_function(function), argc);
}

void rb_define_module_function(VALUE module, const char *name, void *function, int argc) {
  truffle_invoke(RUBY_CEXT, "rb_define_module_function", module, rb_str_new_cstr(name), truffle_address_to_function(function), argc);
}

void rb_define_global_function(const char *name, void *function, int argc) {
  rb_define_module_function(rb_mKernel, name, function, argc);
}

void rb_define_singleton_method(VALUE object, const char *name, void *function, int argc) {
  truffle_invoke(RUBY_CEXT, "rb_define_singleton_method", object, rb_str_new_cstr(name), truffle_address_to_function(function), argc);
}

void rb_define_alias(VALUE module, const char *new_name, const char *old_name) {
  rb_alias(module, rb_str_new_cstr(new_name), rb_str_new_cstr(old_name));
}

void rb_alias(VALUE module, ID new_name, ID old_name) {
  truffle_invoke(RUBY_CEXT, "rb_alias", module, new_name, old_name);
}

void rb_undef_method(VALUE module, const char *name) {
  rb_undef(module, rb_str_new_cstr(name));
}

void rb_undef(VALUE module, ID name) {
  truffle_invoke(RUBY_CEXT, "rb_undef", module, name);
}

void rb_attr(VALUE ruby_class, ID name, int read, int write, int ex) {
  truffle_invoke(RUBY_CEXT, "rb_attr", ruby_class, name, read, write, ex);
}

void rb_define_alloc_func(VALUE ruby_class, rb_alloc_func_t alloc_function) {
  truffle_invoke(RUBY_CEXT, "rb_define_alloc_func", ruby_class, truffle_address_to_function(alloc_function));
}

// Rational

VALUE rb_Rational(VALUE num, VALUE den) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_Rational", num, den);
}

VALUE rb_rational_raw(VALUE num, VALUE den) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_rational_raw", num, den);
}

VALUE rb_rational_new(VALUE num, VALUE den) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_rational_new", num, den);
}

VALUE rb_rational_num(VALUE rat) {
  return (VALUE) truffle_invoke((void *)rat, "numerator");
}

VALUE rb_rational_den(VALUE rat) {
  return (VALUE) truffle_invoke((void *)rat, "denominator");
}

VALUE rb_flt_rationalize_with_prec(VALUE value, VALUE precision) {
  return (VALUE) truffle_invoke((void *)value, "rationalize", precision);
}

VALUE rb_flt_rationalize(VALUE value) {
  return (VALUE) truffle_invoke((void *)value, "rationalize");
}

// Complex

VALUE rb_Complex(VALUE real, VALUE imag) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_Complex", real, imag);
}

VALUE rb_complex_new(VALUE real, VALUE imag) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_complex_new", real, imag);
}

VALUE rb_complex_raw(VALUE real, VALUE imag) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_complex_raw", real, imag);
}

VALUE rb_complex_polar(VALUE r, VALUE theta) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_complex_polar", r, theta);
}

VALUE rb_complex_set_real(VALUE complex, VALUE real) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_complex_set_real", complex, real);
}

VALUE rb_complex_set_imag(VALUE complex, VALUE imag) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_complex_set_imag", complex, imag);
}

// Mutexes

VALUE rb_mutex_new(void) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_mutex_new");
}

VALUE rb_mutex_locked_p(VALUE mutex) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_mutex_locked_p", mutex);
}

VALUE rb_mutex_trylock(VALUE mutex) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_mutex_trylock", mutex);
}

VALUE rb_mutex_lock(VALUE mutex) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_mutex_lock", mutex);
}

VALUE rb_mutex_unlock(VALUE mutex) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_mutex_unlock", mutex);
}

VALUE rb_mutex_sleep(VALUE mutex, VALUE timeout) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_mutex_sleep", mutex, timeout);
}

VALUE rb_mutex_synchronize(VALUE mutex, VALUE (*func)(VALUE arg), VALUE arg) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_mutex_synchronize", mutex, func, arg);
}

// GC

void rb_gc_register_address(VALUE *address) {
}

VALUE rb_gc_enable() {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_gc_enable");
}

VALUE rb_gc_disable() {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_gc_disable");
}

// Threads

void *rb_thread_call_with_gvl(gvl_call function, void *data1) {
  return function(data1);
}

void *rb_thread_call_without_gvl(gvl_call function, void *data1, rb_unblock_function_t *unblock_function, void *data2) {
  // TODO do we need to do anyhting with the unblock_function?
  return function(data1);
}

void *rb_thread_call_without_gvl2(gvl_call function, void *data1, rb_unblock_function_t *unblock_function, void *data2) {
  // TODO do we need to do anyhting with the unblock_function?
  return function(data1);
}

rb_nativethread_id_t rb_nativethread_self() {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_nativethread_self");
}

int rb_nativethread_lock_initialize(rb_nativethread_lock_t *lock) {
  *lock = truffle_invoke(RUBY_CEXT, "rb_nativethread_lock_initialize");
  return 0;
}

int rb_nativethread_lock_destroy(rb_nativethread_lock_t *lock) {
  *lock = NULL;
  return 0;
}

int rb_nativethread_lock_lock(rb_nativethread_lock_t *lock) {
  truffle_invoke((void *)lock, "lock");
  return 0;
}

int rb_nativethread_lock_unlock(rb_nativethread_lock_t *lock) {
  truffle_invoke((void *)lock, "unlock");
  return 0;
}

// IO

void rb_io_check_writable(rb_io_t *io) {
  // TODO
}

void rb_io_check_readable(rb_io_t *io) {
  // TODO
}

int rb_cloexec_dup(int oldfd) {
  rb_jt_error("rb_cloexec_dup not implemented");
  abort();
}

void rb_fd_fix_cloexec(int fd) {
  rb_jt_error("rb_fd_fix_cloexec not implemented");
  abort();
}

int rb_jt_io_handle(VALUE io) {
  return truffle_invoke_i(RUBY_CEXT, "rb_jt_io_handle", io);
}

int rb_io_wait_readable(int fd) {
  rb_jt_error("rb_io_wait_readable not implemented");
  abort();
}

int rb_io_wait_writable(int fd) {
  rb_jt_error("rb_io_wait_writable not implemented");
  abort();
}

void rb_thread_wait_fd(int fd) {
  rb_jt_error("rb_thread_wait_fd not implemented");
  abort();
}

NORETURN(void rb_eof_error(void)) {
  rb_jt_error("rb_eof_error not implemented");
  abort();
}

// Data

struct RData *rb_jt_adapt_rdata(VALUE value) {
  return (struct RData *)truffle_invoke(RUBY_CEXT, "rb_jt_adapt_rdata", value);
}

// Typed data

struct RTypedData *rb_jt_adapt_rtypeddata(VALUE value) {
  return (struct RTypedData *)truffle_invoke(RUBY_CEXT, "rb_jt_adapt_rtypeddata", value);
}

VALUE rb_data_typed_object_wrap(VALUE ruby_class, void *data, const rb_data_type_t *data_type) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_data_typed_object_wrap", ruby_class, data, data_type);
}

VALUE rb_data_typed_object_zalloc(VALUE ruby_class, size_t size, const rb_data_type_t *data_type) {
  VALUE obj = rb_data_typed_object_wrap(ruby_class, 0, data_type);
  DATA_PTR(obj) = calloc(1, size);
  return obj;
}

VALUE rb_data_typed_object_make(VALUE ruby_class, const rb_data_type_t *type, void **data_pointer, size_t size) {
  TypedData_Make_Struct0(result, ruby_class, void, size, type, *data_pointer);
  return result;
}

void *rb_check_typeddata(VALUE value, const rb_data_type_t *data_type) {
  // TODO CS 24-Sep-2016 we're supposed to do some error checking here
  return RTYPEDDATA_DATA(value);
}

// VM

VALUE rb_jt_ruby_verbose_ptr;

VALUE *rb_ruby_verbose_ptr(void) {
  rb_jt_ruby_verbose_ptr = truffle_invoke(RUBY_CEXT, "rb_ruby_verbose_ptr");
  return &rb_jt_ruby_verbose_ptr;
}

VALUE rb_jt_ruby_debug_ptr;

VALUE *rb_ruby_debug_ptr(void) {
  rb_jt_ruby_debug_ptr = truffle_invoke(RUBY_CEXT, "rb_ruby_debug_ptr");
  return &rb_jt_ruby_debug_ptr;
}

// Non-standard

void rb_jt_error(const char *message) {
  truffle_invoke(RUBY_CEXT, "rb_jt_error", rb_str_new_cstr(message));
  abort();
}

void *rb_jt_to_native_handle(VALUE managed) {
  return (void *)truffle_invoke_l(RUBY_CEXT, "rb_jt_to_native_handle", managed);
}

VALUE rb_jt_from_native_handle(void *native) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_jt_from_native_handle", (long) native);
}
