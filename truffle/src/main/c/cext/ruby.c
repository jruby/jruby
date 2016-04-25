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

static void *ruby_cext;

__attribute__((constructor))
void truffle_ruby_load() {
  ruby_cext = truffle_import("ruby_cext");
}

VALUE get_Qfalse() {
  return (VALUE) truffle_read(ruby_cext, "qfalse");
}

VALUE get_Qtrue() {
  return (VALUE) truffle_read(ruby_cext, "qtrue");
}

VALUE get_Qnil() {
  return (VALUE) truffle_read(ruby_cext, "qnil");
}

VALUE get_rb_cObject() {
  return (VALUE) truffle_read(ruby_cext, "object");
}

VALUE get_rb_cArray() {
  return (VALUE) truffle_read(ruby_cext, "array");
}

VALUE get_rb_cHash() {
  return (VALUE) truffle_read(ruby_cext, "hash");
}

VALUE get_rb_eRuntimeError() {
  return (VALUE) truffle_read(ruby_cext, "runtime_error");
}

int NUM2INT(VALUE value) {
  return truffle_invoke_i(ruby_cext, "num2int", value);
}

unsigned int NUM2UINT(VALUE value) {
  return (unsigned int) truffle_invoke_i(ruby_cext, "num2uint", value);
}

long NUM2LONG(VALUE value) {
  return truffle_invoke_l(ruby_cext, "num2long", value);
}

int FIX2INT(VALUE value) {
  return truffle_invoke_i(ruby_cext, "fix2int", value);
}

unsigned int FIX2UINT(VALUE value) {
  return (unsigned int) truffle_invoke_i(ruby_cext, "fix2uint", value);
}

long FIX2LONG(VALUE value) {
  return truffle_invoke_l(ruby_cext, "fix2long", value);
}

VALUE INT2NUM(int value) {
  return (VALUE) truffle_invoke(ruby_cext, "int2num", value);
}

VALUE INT2FIX(int value) {
  return (VALUE) truffle_invoke(ruby_cext, "int2fix", value);
}

VALUE UINT2NUM(unsigned int value) {
  return (VALUE) truffle_invoke(ruby_cext, "uint2num", value);
}

VALUE LONG2NUM(long value) {
  return (VALUE) truffle_invoke(ruby_cext, "long2num", value);
}

VALUE LONG2FIX(long value) {
  return (VALUE) truffle_invoke(ruby_cext, "long2fix", value);
}

int FIXNUM_P(VALUE value) {
  return truffle_invoke_i(ruby_cext, "fixnum?", value);
}

VALUE rb_float_new(double value) {
  return (VALUE) truffle_invoke(ruby_cext, "float_new", value);
}

char *RSTRING_PTR(VALUE string) {
  // Needs to return a fake char* which actually calls back into Ruby when read or written
  return (char*) truffle_invoke(ruby_cext, "string_ptr", string);
}

int RSTRING_LEN(VALUE string) {
  return truffle_get_size(string);
}

VALUE rb_ary_dup(VALUE array) {
  return (VALUE) truffle_invoke(array, "dup");
}

ID rb_intern(const char *string) {
  return (ID) truffle_invoke(ruby_cext, "intern", string);
}

VALUE rb_str_new2(const char *string) {
  return (VALUE) truffle_invoke(ruby_cext, "str_new2", string);
}

VALUE rb_intern_str(VALUE string) {
  return (VALUE) truffle_invoke(ruby_cext, "intern", string);
}

VALUE ID2SYM(ID id) {
  return truffle_invoke(ruby_cext, "id2sym", id);
}

void rb_str_cat(VALUE string, char *to_concat, long length) {
  truffle_invoke(ruby_cext, "string_cat", string, to_concat, length);
}

int RARRAY_LEN(VALUE array) {
  return truffle_get_size(array);
}

VALUE *RARRAY_PTR(VALUE array) {
  // Needs to return a fake VALUE* which actually calls back into Ruby when read or written
  return (VALUE*) truffle_invoke(ruby_cext, "array_ptr", array);
}

VALUE rb_ary_new_capa(long capacity) {
  return (VALUE) truffle_invoke(ruby_cext, "array_new_capa", capacity);
}

VALUE rb_ary_new2() {
  return (VALUE) truffle_invoke(ruby_cext, "array_new2");
}

VALUE rb_ary_new() {
  return (VALUE) truffle_invoke(ruby_cext, "array_new");
}

void rb_ary_push(VALUE array, VALUE value) {
  truffle_invoke(array, "push", value);
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
  return (VALUE) truffle_invoke(ruby_cext, "hash_new");
}

VALUE rb_hash_aref(VALUE hash, VALUE key) {
  return truffle_read(hash, key);
}

void rb_hash_aset(VALUE hash, VALUE key, VALUE value) {
  truffle_write(hash, key, value);
}

void rb_scan_args(int argc, VALUE *argv, char *format, ...) {
  truffle_invoke(ruby_cext, "scan_args", argc, argv, format /*, where to get args? */);
}

VALUE rb_funcall(VALUE object, ID name, int argc, ...) {
  return truffle_invoke(object, name /*, where to get args? */);
}

VALUE rb_iv_get(VALUE object, const char *name) {
  return truffle_read(object, name);
}

VALUE rb_iv_set(VALUE object, const char *name, VALUE value) {
  truffle_write(object, name, value);
  return value;
}

VALUE rb_const_get(VALUE object, ID name) {
  return truffle_invoke(ruby_cext, "const_get", object, name);
}

void rb_raise(VALUE exception, const char *format, ...) {
  truffle_invoke(ruby_cext, "raise", format /*, where to get args? */);
}

VALUE rb_define_module(char *name) {
  return truffle_invoke(ruby_cext, "define_module", name);
}

VALUE rb_define_module_under(VALUE module, char *name) {
  return truffle_invoke(ruby_cext, "define_module_under", module, name);
}

void rb_define_method(VALUE module, char *name, void *function, int args) {
  truffle_invoke(ruby_cext, "define_method", module, name, function, args);
}

void rb_define_private_method(VALUE module, char *name, void *function, int args) {
  truffle_invoke(ruby_cext, "define_private_method", module, name, function, args);
}

int rb_define_module_function(VALUE module, char *name, void *function, int args) {
  return truffle_invoke_i(ruby_cext, "define_module_function", module, name, function, args);
}
