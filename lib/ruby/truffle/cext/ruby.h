/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

#ifndef TRUFFLE_RUBY_H
#define TRUFFLE_RUBY_H

#if defined(__cplusplus)
extern "C" {
#endif

#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <math.h>

#define JRUBY_TRUFFLE 1

#define xmalloc malloc
#define xfree free
#define ALLOC_N(type, n) malloc(sizeof(type) * n)

typedef void* ID;
typedef void* VALUE;

// Constants

VALUE get_Qfalse(void);
VALUE get_Qtrue(void);
VALUE get_Qnil(void);
VALUE get_rb_cProc(void);
VALUE get_rb_eException(void);

#define Qfalse get_Qfalse()
#define Qtrue get_Qtrue()
#define Qnil get_Qnil()
#define rb_cProc get_rb_cProc();
#define rb_eException get_rb_eException();

VALUE get_rb_cObject(void);
VALUE get_rb_cArray(void);
VALUE get_rb_cHash(void);
VALUE get_rb_mKernel(void);

#define rb_cObject get_rb_cObject()
#define rb_cArray get_rb_cArray()
#define rb_cHash get_rb_cHash()
#define rb_mKernel get_rb_mKernel()

VALUE get_rb_eRuntimeError(void);

#define rb_eRuntimeError get_rb_eRuntimeError()

// Conversions

int NUM2INT(VALUE value);
unsigned int NUM2UINT(VALUE value);
long NUM2LONG(VALUE value);

int FIX2INT(VALUE value);
unsigned int FIX2UINT(VALUE value);
long FIX2LONG(VALUE value);

VALUE INT2NUM(int value);
VALUE INT2FIX(int value);
VALUE UINT2NUM(unsigned int value);

VALUE LONG2NUM(long value);
VALUE LONG2FIX(long value);

ID SYM2ID(VALUE value);
VALUE ID2SYM(ID value);

// Type checks

int NIL_P(VALUE value);
int FIXNUM_P(VALUE value);

// Float

VALUE rb_float_new(double value);

// String

char *RSTRING_PTR(VALUE string);
int RSTRING_LEN(VALUE string);
VALUE rb_intern_str(VALUE string);
VALUE rb_str_new_cstr(const char *string);
#define rb_str_new2 rb_str_new_cstr
void rb_str_cat(VALUE string, const char *to_concat, long length);

// Symbol

ID rb_intern(const char *string);
ID rb_intern2(const char *string, long length);
#define rb_intern_const(str) rb_intern2((str), strlen(str))

// Array

int RARRAY_LEN(VALUE array);
int RARRAY_LENINT(VALUE array);
VALUE *RARRAY_PTR(VALUE array);
VALUE RARRAY_AREF(VALUE array, long index);
VALUE rb_Array(VALUE value);
VALUE rb_ary_new(void);
VALUE rb_ary_new_capa(long capacity);
#define rb_ary_new2 rb_ary_new_capa
VALUE rb_ary_new_from_args(long n, ...);
#define rb_ary_new3 rb_ary_new_from_args
VALUE rb_ary_push(VALUE array, VALUE value);
VALUE rb_ary_pop(VALUE array);
void rb_ary_store(VALUE array, long index, VALUE value);
VALUE rb_ary_entry(VALUE array, long index);
VALUE rb_ary_dup(VALUE array);

// Hash

VALUE rb_hash_new(void);
VALUE rb_hash_aref(VALUE hash, VALUE key);
VALUE rb_hash_aset(VALUE hash, VALUE key, VALUE value);

// Utilities

int rb_scan_args(int argc, VALUE *argv, const char *format, ...);

// Calls

VALUE rb_funcall(VALUE object, ID name, int argc, ...);

// Instance variables

VALUE rb_iv_get(VALUE object, const char *name);
VALUE rb_iv_set(VALUE object, const char *name, VALUE value);

// Accessing constants

VALUE rb_const_get(VALUE object, ID name);

// Raising exceptions

void rb_raise(VALUE exception, const char *format, ...);

// Defining classes, modules and methods

VALUE rb_define_class(const char *name, VALUE superclass);
VALUE rb_define_class_under(VALUE module, const char *name, VALUE superclass);
VALUE rb_define_class_id_under(VALUE module, ID name, VALUE superclass);
VALUE rb_define_module(const char *name);
VALUE rb_define_module_under(VALUE module, const char *name);

void rb_define_method(VALUE module, const char *name, void *function, int argc);
void rb_define_private_method(VALUE module, const char *name, void *function, int argc);
void rb_define_protected_method(VALUE module, const char *name, void *function, int argc);
void rb_define_module_function(VALUE module, const char *name, void *function, int argc);
void rb_define_global_function(const char *name, void *function, int argc);
void rb_define_singleton_method(VALUE object, const char *name, void *function, int argc);

#if defined(__cplusplus)
}
#endif

#endif
