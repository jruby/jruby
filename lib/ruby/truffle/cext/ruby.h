/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * This file contains code that is based on the Ruby API headers,
 * copyright (C) Yukihiro Matsumoto, licensed under the 2-clause BSD licence
 * as described in the file BSDL included with JRuby+Truffle.
 */

#ifndef TRUFFLE_RUBY_H
#define TRUFFLE_RUBY_H

#if defined(__cplusplus)
extern "C" {
#endif

#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#define JRUBY_TRUFFLE 1

#define SIZEOF_INT 32
#define SIZEOF_LONG 64

#include <truffle.h>

#define RUBY_CEXT truffle_import_cached("ruby_cext")

#define xmalloc malloc
#define xfree free
#define ALLOC_N(type, n) malloc(sizeof(type) * n)

typedef void *ID;
typedef void *VALUE;

#define NORETURN(X) __attribute__((__noreturn__)) X

// Types

enum ruby_value_type {
    RUBY_T_NONE     = 0x00,

    RUBY_T_OBJECT   = 0x01,
    RUBY_T_CLASS    = 0x02,
    RUBY_T_MODULE   = 0x03,
    RUBY_T_FLOAT    = 0x04,
    RUBY_T_STRING   = 0x05,
    RUBY_T_REGEXP   = 0x06,
    RUBY_T_ARRAY    = 0x07,
    RUBY_T_HASH     = 0x08,
    RUBY_T_STRUCT   = 0x09,
    RUBY_T_BIGNUM   = 0x0a,
    RUBY_T_FILE     = 0x0b,
    RUBY_T_DATA     = 0x0c,
    RUBY_T_MATCH    = 0x0d,
    RUBY_T_COMPLEX  = 0x0e,
    RUBY_T_RATIONAL = 0x0f,

    RUBY_T_NIL      = 0x11,
    RUBY_T_TRUE     = 0x12,
    RUBY_T_FALSE    = 0x13,
    RUBY_T_SYMBOL   = 0x14,
    RUBY_T_FIXNUM   = 0x15,
    RUBY_T_UNDEF    = 0x16,

    RUBY_T_IMEMO    = 0x1a,
    RUBY_T_NODE     = 0x1b,
    RUBY_T_ICLASS   = 0x1c,
    RUBY_T_ZOMBIE   = 0x1d,

    RUBY_T_MASK     = 0x1f
};

#define T_NONE      RUBY_T_NONE
#define T_NIL       RUBY_T_NIL
#define T_OBJECT    RUBY_T_OBJECT
#define T_CLASS     RUBY_T_CLASS
#define T_ICLASS    RUBY_T_ICLASS
#define T_MODULE    RUBY_T_MODULE
#define T_FLOAT     RUBY_T_FLOAT
#define T_STRING    RUBY_T_STRING
#define T_REGEXP    RUBY_T_REGEXP
#define T_ARRAY     RUBY_T_ARRAY
#define T_HASH      RUBY_T_HASH
#define T_STRUCT    RUBY_T_STRUCT
#define T_BIGNUM    RUBY_T_BIGNUM
#define T_FILE      RUBY_T_FILE
#define T_FIXNUM    RUBY_T_FIXNUM
#define T_TRUE      RUBY_T_TRUE
#define T_FALSE     RUBY_T_FALSE
#define T_DATA      RUBY_T_DATA
#define T_MATCH     RUBY_T_MATCH
#define T_SYMBOL    RUBY_T_SYMBOL
#define T_RATIONAL  RUBY_T_RATIONAL
#define T_COMPLEX   RUBY_T_COMPLEX
#define T_IMEMO     RUBY_T_IMEMO
#define T_UNDEF     RUBY_T_UNDEF
#define T_NODE      RUBY_T_NODE
#define T_ZOMBIE    RUBY_T_ZOMBIE
#define T_MASK      RUBY_T_MASK

typedef struct rb_data_type_struct rb_data_type_t;

struct rb_data_type_struct {
  const char *wrap_struct_name;
  
  struct {
    void (*dmark)(void *);
    void (*dfree)(void *);
    size_t (*dsize)(const void *);
    void *reserved[2];
  } function;
  
  const rb_data_type_t *parent;
  void *data;
  VALUE flags;
};

int rb_type(VALUE value);
#define TYPE(value) rb_type((VALUE) (value))
bool RB_TYPE_P(VALUE value, int type);

void rb_check_type(VALUE value, int type);
#define Check_Type(v,t) rb_check_type((VALUE)(v), (t))

VALUE rb_obj_is_kind_of(VALUE object, VALUE ruby_class);

// Constants

VALUE get_Qfalse(void);
VALUE get_Qtrue(void);
VALUE get_Qnil(void);
VALUE get_rb_cProc(void);
VALUE get_rb_eException(void);

#define Qfalse get_Qfalse()
#define Qtrue get_Qtrue()
#define Qnil get_Qnil()
#define rb_cProc get_rb_cProc()
#define rb_eException get_rb_eException()

VALUE get_rb_cObject(void);
VALUE get_rb_cArray(void);
VALUE get_rb_cHash(void);
VALUE get_rb_mKernel(void);

#define rb_cObject get_rb_cObject()
#define rb_cArray get_rb_cArray()
#define rb_cHash get_rb_cHash()
#define rb_mKernel get_rb_mKernel()

VALUE get_rb_eRuntimeError(void);
VALUE get_rb_eStandardError(void);
VALUE get_rb_eNoMemError(void);

#define rb_eRuntimeError get_rb_eRuntimeError()
#define rb_eStandardError get_rb_eStandardError()
#define rb_eNoMemError get_rb_eNoMemError()

// Conversions

VALUE CHR2FIX(char ch);

int NUM2INT(VALUE value);
unsigned int NUM2UINT(VALUE value);
long NUM2LONG(VALUE value);

int FIX2INT(VALUE value);
unsigned int FIX2UINT(VALUE value);
long FIX2LONG(VALUE value);

VALUE INT2NUM(long value);
VALUE INT2FIX(long value);
VALUE UINT2NUM(unsigned int value);

VALUE LONG2NUM(long value);
VALUE LONG2FIX(long value);

int rb_fix2int(VALUE value);
unsigned long rb_fix2uint(VALUE value);

ID SYM2ID(VALUE value);
VALUE ID2SYM(ID value);

// Type checks

int NIL_P(VALUE value);
int FIXNUM_P(VALUE value);
int RTEST(VALUE value);

// Float

VALUE rb_float_new(double value);
VALUE rb_Float(VALUE value);
double RFLOAT_VALUE(VALUE value);

// String

char *RSTRING_PTR(VALUE string);
int rb_str_len(VALUE string);
#define RSTRING_LEN(str) rb_str_len(str)
#define RSTRING_LENINT(str) rb_str_len(str)
VALUE rb_intern_str(VALUE string);
VALUE rb_str_new(const char *string, long length);
VALUE rb_str_new_cstr(const char *string);
#define rb_str_new2 rb_str_new_cstr
void rb_str_cat(VALUE string, const char *to_concat, long length);
VALUE rb_str_to_str(VALUE string);
#define StringValue(value) rb_string_value(&(value))
#define SafeStringValue StringValue
VALUE rb_string_value(VALUE *value_pointer);
VALUE rb_str_buf_new(long capacity);

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
VALUE rb_ary_new4(long n, const VALUE *values);
VALUE rb_ary_push(VALUE array, VALUE value);
VALUE rb_ary_pop(VALUE array);
void rb_ary_store(VALUE array, long index, VALUE value);
VALUE rb_ary_entry(VALUE array, long index);
VALUE rb_ary_dup(VALUE array);

// Hash

VALUE rb_hash_new(void);
VALUE rb_hash_aref(VALUE hash, VALUE key);
VALUE rb_hash_aset(VALUE hash, VALUE key, VALUE value);

// Class

const char* rb_class2name(VALUE module);

// Proc

VALUE rb_proc_new(void *function, VALUE value);

// Utilities

void rb_warn(const char *fmt, ...);
void rb_warning(const char *fmt, ...);

int rb_scan_args(int argc, VALUE *argv, const char *format, ...);

// Calls

int rb_respond_to(VALUE object, ID name);

#define rb_funcall(object, name, ...) truffle_invoke(RUBY_CEXT, "rb_funcall", object, name, __VA_ARGS__)

int rb_block_given_p();
VALUE rb_yield(VALUE value);

// Instance variables

VALUE rb_iv_get(VALUE object, const char *name);
VALUE rb_iv_set(VALUE object, const char *name, VALUE value);

// Accessing constants

int rb_const_defined(VALUE module, ID name);
int rb_const_defined_at(VALUE module, ID name);

VALUE rb_const_get(VALUE module, ID name);
VALUE rb_const_get_at(VALUE module, ID name);
VALUE rb_const_get_from(VALUE module, ID name);

VALUE rb_const_set(VALUE module, ID name, VALUE value);
VALUE rb_define_const(VALUE module, const char *name, VALUE value);
void rb_define_global_const(const char *name, VALUE value);

// Exceptions

NORETURN(void rb_raise(VALUE exception, const char *format, ...));

VALUE rb_protect(VALUE (*function)(VALUE), VALUE data, int *status);
void rb_jump_tag(int status);

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

void rb_define_alias(VALUE module, const char *new_name, const char *old_name);
void rb_alias(VALUE module, ID new_name, ID old_name);

void rb_undef_method(VALUE module, const char *name);
void rb_undef(VALUE module, ID name);

// Mutexes

VALUE rb_mutex_new(void);
VALUE rb_mutex_locked_p(VALUE mutex);
VALUE rb_mutex_trylock(VALUE mutex);
VALUE rb_mutex_lock(VALUE mutex);
VALUE rb_mutex_unlock(VALUE mutex);
VALUE rb_mutex_sleep(VALUE mutex, VALUE timeout);
VALUE rb_mutex_synchronize(VALUE mutex, VALUE (*func)(VALUE arg), VALUE arg);

// Rational

VALUE rb_Rational(VALUE num, VALUE den);
#define rb_Rational1(x) rb_Rational((x), INT2FIX(1))
#define rb_Rational2(x,y) rb_Rational((x), (y))
VALUE rb_rational_raw(VALUE num, VALUE den);
#define rb_rational_raw1(x) rb_rational_raw((x), INT2FIX(1))
#define rb_rational_raw2(x,y) rb_rational_raw((x), (y))
VALUE rb_rational_new(VALUE num, VALUE den);
#define rb_rational_new1(x) rb_rational_new((x), INT2FIX(1))
#define rb_rational_new2(x,y) rb_rational_new((x), (y))
VALUE rb_rational_num(VALUE rat);
VALUE rb_rational_den(VALUE rat);
VALUE rb_flt_rationalize_with_prec(VALUE value, VALUE precision);
VALUE rb_flt_rationalize(VALUE value);

// Complex

VALUE rb_Complex(VALUE real, VALUE imag);
#define rb_Complex1(x) rb_Complex((x), INT2FIX(0))
#define rb_Complex2(x,y) rb_Complex((x), (y))
VALUE rb_complex_new(VALUE real, VALUE imag);
#define rb_complex_new1(x) rb_complex_new((x), INT2FIX(0))
#define rb_complex_new2(x,y) rb_complex_new((x), (y))
VALUE rb_complex_raw(VALUE real, VALUE imag);
#define rb_complex_raw1(x) rb_complex_raw((x), INT2FIX(0))
#define rb_complex_raw2(x,y) rb_complex_raw((x), (y))
VALUE rb_complex_polar(VALUE r, VALUE theta);
VALUE rb_complex_set_real(VALUE complex, VALUE real);
VALUE rb_complex_set_imag(VALUE complex, VALUE imag);

// GC

void rb_gc_register_address(VALUE *address);
#define rb_global_variable(address) ;
VALUE rb_gc_enable();
VALUE rb_gc_disable();

// Threads

typedef void *rb_nativethread_id_t;
typedef void *rb_nativethread_lock_t;

rb_nativethread_id_t rb_nativethread_self();
int rb_nativethread_lock_initialize(rb_nativethread_lock_t *lock);
int rb_nativethread_lock_destroy(rb_nativethread_lock_t *lock);
int rb_nativethread_lock_lock(rb_nativethread_lock_t *lock);
int rb_nativethread_lock_unlock(rb_nativethread_lock_t *lock);

#if defined(__cplusplus)
}
#endif

#endif
