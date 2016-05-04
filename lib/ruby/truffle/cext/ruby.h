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

#define xmalloc malloc
#define xfree free
#define ALLOC_N(type, n) malloc(sizeof(type) * n)

typedef void *ID;
typedef void *VALUE;

VALUE get_Qfalse();
VALUE get_Qtrue();
VALUE get_Qnil();

#define Qfalse get_Qfalse()
#define Qtrue get_Qtrue()
#define Qnil get_Qnil()

VALUE get_rb_cObject();
VALUE get_rb_cArray();
VALUE get_rb_cHash();

#define rb_cObject get_rb_cObject()
#define rb_cArray get_rb_cArray()
#define rb_cHash get_rb_cHash()

VALUE get_rb_eRuntimeError();

#define rb_eRuntimeError get_rb_eRuntimeError()


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

int FIXNUM_P(VALUE value);

VALUE rb_float_new(double value);

char *RSTRING_PTR(VALUE string);
int RSTRING_LEN(VALUE string);
ID rb_intern(const char *string);
VALUE rb_str_new2(const char *string);
VALUE ID2SYM(ID id);
VALUE rb_intern_str(VALUE string);
void rb_str_cat(VALUE string, const char *to_concat, long length);

int RARRAY_LEN(VALUE array);
VALUE *RARRAY_PTR(VALUE array);
VALUE rb_ary_new_capa(long capacity);
VALUE rb_ary_new2();
VALUE rb_ary_new();
void rb_ary_push(VALUE array, VALUE value);
void rb_ary_store(VALUE array, long index, VALUE value);
VALUE rb_ary_entry(VALUE array, long index);
int RARRAY_LENINT(VALUE array);
VALUE rb_ary_dup(VALUE array);

VALUE rb_hash_new();
VALUE rb_hash_aref(VALUE hash, VALUE key);
void rb_hash_aset(VALUE hash, VALUE key, VALUE value);

void rb_scan_args(int argc, VALUE *argv, const char *format, ...);

VALUE rb_funcall(VALUE object, ID name, int argc, ...);

VALUE rb_iv_get(VALUE object, const char *name);
VALUE rb_iv_set(VALUE object, const char *name, VALUE value);

VALUE rb_const_get(VALUE object, ID name);

void rb_raise(VALUE exception, const char *format, ...);

VALUE rb_define_module(const char *name);
VALUE rb_define_module_under(VALUE module, const char *name);

void rb_define_method(VALUE module, const char *name, void *function, int args);
void rb_define_private_method(VALUE module, const char *name, void *function, int args);
void rb_define_module_function(VALUE module, const char *name, void *function, int args);

#if defined(__cplusplus)
}
#endif

#endif
