/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

#include <truffle.h>

#include <ruby.h>

#define RUBY_CEXT truffle_import_cached("ruby_cext")

VALUE get_Qfalse() {
  return (VALUE) truffle_read(RUBY_CEXT, "Qfalse");
}

VALUE get_Qtrue() {
  return (VALUE) truffle_read(RUBY_CEXT, "Qtrue");
}

VALUE get_Qnil() {
  return (VALUE) truffle_read(RUBY_CEXT, "Qnil");
}

VALUE get_rb_cObject() {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cObject");
}

VALUE get_rb_cArray() {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cArray");
}

VALUE get_rb_cHash() {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_cHash");
}

VALUE get_rb_eRuntimeError() {
  return (VALUE) truffle_read(RUBY_CEXT, "rb_eRuntimeError");
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

int FIX2INT(VALUE value) {
  return truffle_invoke_i(RUBY_CEXT, "FIX2INT", value);
}

unsigned int FIX2UINT(VALUE value) {
  return (unsigned int) truffle_invoke_i(RUBY_CEXT, "FIX2UINT", value);
}

long FIX2LONG(VALUE value) {
  return truffle_invoke_l(RUBY_CEXT, "FIX2LONG", value);
}

VALUE INT2NUM(int value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "INT2NUM", value);
}

VALUE INT2FIX(int value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "INT2FIX", value);
}

VALUE UINT2NUM(unsigned int value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "UINT2NUM", value);
}

VALUE LONG2NUM(long value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "LONG2NUM", value);
}

VALUE LONG2FIX(long value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "LONG2FIX", value);
}

int FIXNUM_P(VALUE value) {
  return truffle_invoke_i(RUBY_CEXT, "FIXNUM_P", value);
}

VALUE rb_float_new(double value) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_float_new", value);
}

char *RSTRING_PTR(VALUE string) {
  // Needs to return a fake char* which actually calls back into Ruby when read or written
  return (char*) truffle_invoke(RUBY_CEXT, "RSTRING_PTR", string);
}

int RSTRING_LEN(VALUE string) {
  return truffle_get_size(string);
}

VALUE rb_ary_dup(VALUE array) {
  return (VALUE) truffle_invoke(array, "dup");
}

ID rb_intern(const char *string) {
  return (ID) truffle_invoke(RUBY_CEXT, "rb_intern", string);
}

VALUE rb_str_new2(const char *string) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_str_new2", truffle_read_string(string));
}

VALUE rb_intern_str(VALUE string) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_intern_str", string);
}

VALUE ID2SYM(ID id) {
  return truffle_invoke(RUBY_CEXT, "ID2SYM", id);
}

void rb_str_cat(VALUE string, const char *to_concat, long length) {
  truffle_invoke(RUBY_CEXT, "rb_str_cat", string, truffle_read_string(to_concat), length);
}

int RARRAY_LEN(VALUE array) {
  return truffle_get_size(array);
}

VALUE *RARRAY_PTR(VALUE array) {
  // Needs to return a fake VALUE* which actually calls back into Ruby when read or written
  return (VALUE*) truffle_invoke(RUBY_CEXT, "RARRAY_PTR", array);
}

VALUE rb_ary_new_capa(long capacity) {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_ary_new_capa", capacity);
}

VALUE rb_ary_new() {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_ary_new");
}

void rb_ary_push(VALUE array, VALUE value) {
  truffle_invoke(array, "push", value);
}

VALUE rb_ary_pop(VALUE array) {
  return truffle_invoke(array, "pop");
}

void rb_ary_store(VALUE array, long index, VALUE value) {
  truffle_write_idx(array, (int) index, value);
}

VALUE rb_ary_entry(VALUE array, long index) {
  return truffle_read_idx(array, (int) index);
}

int RARRAY_LENINT(VALUE array) {
  return truffle_get_size(array);
}

VALUE rb_hash_new() {
  return (VALUE) truffle_invoke(RUBY_CEXT, "rb_hash_new");
}

VALUE rb_hash_aref(VALUE hash, VALUE key) {
  return truffle_read(hash, key);
}

void rb_hash_aset(VALUE hash, VALUE key, VALUE value) {
  truffle_write(hash, key, value);
}

void rb_scan_args(int argc, VALUE *argv, const char *format, ...) {
  truffle_invoke(RUBY_CEXT, "rb_scan_args", argc, argv, format /*, where to get args? */);
}

VALUE rb_funcall(VALUE object, ID name, int argc, ...) {
  return truffle_invoke(object, name /*, where to get args? */);
}

VALUE rb_iv_get(VALUE object, const char *name) {
  return truffle_read(object, truffle_read_string(name));
}

VALUE rb_iv_set(VALUE object, const char *name, VALUE value) {
  truffle_write(object, truffle_read_string(name), value);
  return value;
}

VALUE rb_const_get(VALUE object, ID name) {
  return truffle_invoke(object, "const_get", name);
}

void rb_raise(VALUE exception, const char *format, ...) {
  truffle_invoke(RUBY_CEXT, "rb_raise", format /*, where to get args? */);
}

VALUE rb_define_class(const char *name, VALUE superclass) {
  return truffle_invoke(RUBY_CEXT, "rb_define_class", truffle_read_string(name), superclass);
}

VALUE rb_define_module(const char *name) {
  return truffle_invoke(RUBY_CEXT, "rb_define_module", truffle_read_string(name));
}

VALUE rb_define_module_under(VALUE module, const char *name) {
  return truffle_invoke(RUBY_CEXT, "rb_define_module_under", module, name);
}

void rb_define_method(VALUE module, const char *name, void *function, int args) {
  truffle_invoke(RUBY_CEXT, "rb_define_method", module, truffle_read_string(name), truffle_address_to_function(function), args);
}

void rb_define_private_method(VALUE module, const char *name, void *function, int args) {
  truffle_invoke(RUBY_CEXT, "rb_define_private_method", module, truffle_read_string(name), truffle_address_to_function(function), args);
}

void rb_define_module_function(VALUE module, const char *name, void *function, int args) {
  truffle_invoke(RUBY_CEXT, "rb_define_module_function", module, truffle_read_string(name), truffle_address_to_function(function), args);
}
