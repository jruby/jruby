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
#include <unistd.h>
#include <string.h>
#include <math.h>

#include <truffle.h>

// Support

#define RUBY_CEXT (void *)truffle_import_cached("ruby_cext")
#define MUST_INLINE __attribute__((always_inline))

// Configuration

#define JRUBY_TRUFFLE 1

#define SIZEOF_INT 32
#define SIZEOF_LONG 64

#define HAVE_SYS_TIME_H

#define HAVE_RB_IO_T

// Overrides

#ifdef memcpy
#undef memcpy
#endif

#define memcpy truffle_managed_memcpy

// Macros

#define NORETURN(X) __attribute__((__noreturn__)) X
#define UNREACHABLE ((void)0)
#define _(x) x

// Basic types

typedef void *VALUE;

typedef VALUE ID;

// Helpers

NORETURN(VALUE rb_f_notimplement(int args_count, const VALUE *args, VALUE object));

// Non-standard

NORETURN(void rb_jt_error(const char *message));

void *rb_jt_to_native_handle(VALUE managed);
VALUE rb_jt_from_native_handle(void *native);

// Memory

#define xmalloc       malloc
#define xfree         free
#define ruby_xfree    free
#define ruby_xcalloc  calloc

#define ALLOC_N(type, n)            ((type *)malloc(sizeof(type) * (n)))
#define ALLOCA_N(type, n)           ((type *)alloca(sizeof(type) * (n)))

#define RB_ZALLOC_N(type, n)        ((type *)ruby_xcalloc((n), sizeof(type)))
#define RB_ZALLOC(type)             (RB_ZALLOC_N(type, 1))
#define ZALLOC_N(type, n)           RB_ZALLOC_N(type, n)
#define ZALLOC(type)                RB_ZALLOC(type)

void *rb_alloc_tmp_buffer(VALUE *buffer_pointer, long length);
void *rb_alloc_tmp_buffer2(VALUE *buffer_pointer, long count, size_t size);
void rb_free_tmp_buffer(VALUE *buffer_pointer);

#define RB_ALLOCV(v, n)             rb_alloc_tmp_buffer(&(v), (n))
#define RB_ALLOCV_N(type, v, n)     rb_alloc_tmp_buffer2(&(v), (n), sizeof(type))
#define RB_ALLOCV_END(v)            rb_free_tmp_buffer(&(v))

#define ALLOCV(v, n)                RB_ALLOCV(v, n)
#define ALLOCV_N(type, v, n)        RB_ALLOCV_N(type, v, n)
#define ALLOCV_END(v)               RB_ALLOCV_END(v)

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

#define T_NONE    RUBY_T_NONE
#define T_NIL     RUBY_T_NIL
#define T_OBJECT  RUBY_T_OBJECT
#define T_CLASS   RUBY_T_CLASS
#define T_ICLASS  RUBY_T_ICLASS
#define T_MODULE  RUBY_T_MODULE
#define T_FLOAT   RUBY_T_FLOAT
#define T_STRING  RUBY_T_STRING
#define T_REGEXP  RUBY_T_REGEXP
#define T_ARRAY   RUBY_T_ARRAY
#define T_HASH    RUBY_T_HASH
#define T_STRUCT  RUBY_T_STRUCT
#define T_BIGNUM  RUBY_T_BIGNUM
#define T_FILE    RUBY_T_FILE
#define T_FIXNUM  RUBY_T_FIXNUM
#define T_TRUE    RUBY_T_TRUE
#define T_FALSE   RUBY_T_FALSE
#define T_DATA    RUBY_T_DATA
#define T_MATCH   RUBY_T_MATCH
#define T_SYMBOL  RUBY_T_SYMBOL
#define T_RATIONAL  RUBY_T_RATIONAL
#define T_COMPLEX   RUBY_T_COMPLEX
#define T_IMEMO   RUBY_T_IMEMO
#define T_UNDEF   RUBY_T_UNDEF
#define T_NODE    RUBY_T_NODE
#define T_ZOMBIE  RUBY_T_ZOMBIE
#define T_MASK    RUBY_T_MASK

int rb_type(VALUE value);
#define TYPE(value) rb_type((VALUE) (value))
bool RB_TYPE_P(VALUE value, int type);

void rb_check_type(VALUE value, int type);
#define Check_Type(v,t) rb_check_type((VALUE)(v), (t))

VALUE rb_obj_is_instance_of(VALUE object, VALUE ruby_class);
VALUE rb_obj_is_kind_of(VALUE object, VALUE ruby_class);

void rb_check_frozen(VALUE object);
void rb_check_safe_obj(VALUE object);

bool SYMBOL_P(VALUE value);

// Constants

VALUE rb_jt_get_Qundef(void);
VALUE rb_jt_get_Qfalse(void);
VALUE rb_jt_get_Qtrue(void);
VALUE rb_jt_get_Qnil(void);

#define Qundef  rb_jt_get_Qundef()
#define Qfalse  rb_jt_get_Qfalse()
#define Qtrue   rb_jt_get_Qtrue()
#define Qnil    rb_jt_get_Qnil()

VALUE rb_jt_get_cObject(void);
VALUE rb_jt_get_cArray(void);
VALUE rb_jt_get_cHash(void);
VALUE rb_jt_get_mKernel(void);
VALUE rb_jt_get_cProc(void);
VALUE rb_jt_get_cTime(void);
VALUE rb_jt_get_mEnumerable(void);
VALUE rb_jt_get_mWaitReadable(void);
VALUE rb_jt_get_mWaitWritable(void);
VALUE rb_jt_get_mComparable(void);

#define rb_cObject          rb_jt_get_cObject()
#define rb_cArray           rb_jt_get_cArray()
#define rb_cHash            rb_jt_get_cHash()
#define rb_mKernel          rb_jt_get_mKernel()
#define rb_cProc            rb_jt_get_cProc()
#define rb_cTime            rb_jt_get_cTime()
#define rb_mEnumerable      rb_jt_get_mEnumerable()
#define rb_mWaitReadable    rb_jt_get_mWaitReadable()
#define rb_mWaitWritable    rb_jt_get_mWaitWritable()
#define rb_mComparable      rb_jt_get_mComparable()

VALUE rb_jt_get_eException(void);
VALUE rb_jt_get_eRuntimeError(void);
VALUE rb_jt_get_eStandardError(void);
VALUE rb_jt_get_eNoMemError(void);
VALUE rb_jt_get_eTypeError(void);
VALUE rb_jt_get_eArgError(void);
VALUE rb_jt_get_eRangeError(void);
VALUE rb_jt_get_eNotImpError(void);

#define rb_eException       rb_jt_get_eException()
#define rb_eRuntimeError    rb_jt_get_eRuntimeError()
#define rb_eStandardError   rb_jt_get_eStandardError()
#define rb_eNoMemError      rb_jt_get_eNoMemError()
#define rb_eTypeError       rb_jt_get_eTypeError()
#define rb_eArgError        rb_jt_get_eArgError()
#define rb_eRangeError      rb_jt_get_eRangeError()
#define rb_eNotImpError     rb_jt_get_eNotImpError()

// Conversions

VALUE CHR2FIX(char ch);

int NUM2INT(VALUE value);
unsigned int NUM2UINT(VALUE value);
long NUM2LONG(VALUE value);
unsigned long NUM2ULONG(VALUE value);
double NUM2DBL(VALUE value);

int FIX2INT(VALUE value);
unsigned int FIX2UINT(VALUE value);
long FIX2LONG(VALUE value);

VALUE INT2NUM(long value);
VALUE INT2FIX(long value);
VALUE UINT2NUM(unsigned int value);

VALUE LONG2NUM(long value);
VALUE ULONG2NUM(long value);
VALUE LONG2FIX(long value);

int rb_fix2int(VALUE value);
unsigned long rb_fix2uint(VALUE value);
int rb_long2int(long value);

ID SYM2ID(VALUE value);
VALUE ID2SYM(ID value);

#define NUM2TIMET(value) NUM2LONG(value)
#define TIMET2NUM(value) LONG2NUM(value)

// Type checks

int RB_NIL_P(VALUE value);
int RB_FIXNUM_P(VALUE value);

#define NIL_P RB_NIL_P
#define FIXNUM_P RB_FIXNUM_P

int RTEST(VALUE value);

// Kernel

VALUE rb_require(const char *feature);

// Object

VALUE rb_obj_dup(VALUE object);

VALUE rb_jt_obj_taint(VALUE object);
bool rb_jt_obj_taintable_p(VALUE object);
bool rb_jt_obj_tainted_p(VALUE object);
#define RB_OBJ_TAINTABLE(object)        rb_jt_obj_taintable_p(object)
#define RB_OBJ_TAINTED_RAW(object)      rb_jt_obj_tainted_p(object)
#define RB_OBJ_TAINTED(object)          rb_jt_obj_tainted_p(object)
#define RB_OBJ_TAINT_RAW(object)        rb_jt_obj_taint(object)
#define RB_OBJ_TAINT(object)            rb_jt_obj_taint(object)
#define RB_OBJ_UNTRUSTED(object)        rb_jt_obj_tainted_p(object)
#define RB_OBJ_UNTRUST(object)          rb_jt_obj_taint(object)
#define OBJ_TAINTABLE(object)           rb_jt_obj_taintable_p(object)
#define OBJ_TAINTED_RAW(object)         rb_jt_obj_tainted_p(object)
#define OBJ_TAINTED(object)             rb_jt_obj_tainted_p(object)
#define OBJ_TAINT_RAW(object)           rb_jt_obj_taint(object)
#define OBJ_TAINT(object)               rb_jt_obj_taint(object)
#define OBJ_UNTRUSTED(object)           rb_jt_obj_tainted_p(object)
#define OBJ_UNTRUST(object)             rb_jt_obj_tainted_p(object)

VALUE rb_obj_freeze(VALUE object);
bool rb_jt_obj_frozen_p(VALUE object);
#define rb_obj_freeze_inline(object)    rb_obj_freeze(object)
#define RB_OBJ_FROZEN_RAW(x)            rb_jt_obj_frozen_p(object)
#define RB_OBJ_FROZEN(x)                rb_jt_obj_frozen_p(object)
#define RB_OBJ_FREEZE_RAW(object)       rb_obj_freeze(object)
#define RB_OBJ_FREEZE(x)                rb_obj_freeze((VALUE)x)
#define OBJ_FROZEN_RAW(object)          rb_jt_obj_frozen_p(object)
#define OBJ_FROZEN(object)              rb_jt_obj_frozen_p(object)
#define OBJ_FREEZE_RAW(object)          rb_obj_freeze(object)
#define OBJ_FREEZE(object)              rb_obj_freeze(object)

// Integer

VALUE rb_Integer(VALUE value);

#define INTEGER_PACK_MSWORD_FIRST                   0x01
#define INTEGER_PACK_LSWORD_FIRST                   0x02
#define INTEGER_PACK_MSBYTE_FIRST                   0x10
#define INTEGER_PACK_LSBYTE_FIRST                   0x20
#define INTEGER_PACK_NATIVE_BYTE_ORDER              0x40
#define INTEGER_PACK_2COMP                          0x80
#define INTEGER_PACK_FORCE_GENERIC_IMPLEMENTATION   0x400
#define INTEGER_PACK_FORCE_BIGNUM                   0x100
#define INTEGER_PACK_NEGATIVE                       0x200
#define INTEGER_PACK_LITTLE_ENDIAN                  (INTEGER_PACK_LSWORD_FIRST | INTEGER_PACK_LSBYTE_FIRST)
#define INTEGER_PACK_BIG_ENDIAN                     (INTEGER_PACK_MSWORD_FIRST | INTEGER_PACK_MSBYTE_FIRST)

int rb_integer_pack(VALUE value, void *words, size_t numwords, size_t wordsize, size_t nails, int flags);
VALUE rb_integer_unpack(const void *words, size_t numwords, size_t wordsize, size_t nails, int flags);

size_t rb_absint_size(VALUE value, int *nlz_bits_ret);

VALUE rb_cstr_to_inum(const char* string, int base, int raise);

// Float

VALUE rb_float_new(double value);
VALUE rb_Float(VALUE value);
double RFLOAT_VALUE(VALUE value);

// String

#define PRI_VALUE_PREFIX        "l"
#define PRI_LONG_PREFIX         "l"
#define PRI_64_PREFIX           PRI_LONG_PREFIX
#define RUBY_PRI_VALUE_MARK     "\v"
#define PRIdVALUE               PRI_VALUE_PREFIX"d"
#define PRIoVALUE               PRI_VALUE_PREFIX"o"
#define PRIuVALUE               PRI_VALUE_PREFIX"u"
#define PRIxVALUE               PRI_VALUE_PREFIX"x"
#define PRIXVALUE               PRI_VALUE_PREFIX"X"
#define PRIsVALUE               PRI_VALUE_PREFIX"i" RUBY_PRI_VALUE_MARK

char *RSTRING_PTR(VALUE string);
int rb_str_len(VALUE string);
#define RSTRING_LEN(str) rb_str_len(str)
#define RSTRING_LENINT(str) rb_str_len(str)
VALUE rb_intern_str(VALUE string);
VALUE rb_str_new(const char *string, long length);
VALUE rb_str_new_cstr(const char *string);
#define rb_str_new2 rb_str_new_cstr
VALUE rb_str_cat(VALUE string, const char *to_concat, long length);
VALUE rb_str_cat2(VALUE string, const char *to_concat);
VALUE rb_str_to_str(VALUE string);

MUST_INLINE VALUE rb_string_value(VALUE *value_pointer) {
  VALUE value = *value_pointer;

  if (!RB_TYPE_P(value, T_STRING)) {
    value = rb_str_to_str(value);
    *value_pointer = value;
  }

  return value;
}

MUST_INLINE char *rb_string_value_ptr(VALUE *value_pointer) {
  VALUE string = rb_string_value(value_pointer);
  return RSTRING_PTR(string);
}

MUST_INLINE char *rb_string_value_cstr(VALUE *value_pointer) {
  VALUE string = rb_string_value(value_pointer);

  if (!truffle_invoke_b(RUBY_CEXT, "rb_string_value_cstr_check", string)) {
    rb_jt_error("rb_string_value_cstr failure case not implemented");
    abort();
  }

  return RSTRING_PTR(string);
}

#define StringValue(value) rb_string_value(&(value))
#define SafeStringValue StringValue
#define StringValuePtr(string) rb_string_value_ptr(&(string))
#define StringValueCStr(string) rb_string_value_cstr(&(string))
VALUE rb_str_buf_new(long capacity);
VALUE rb_sprintf(const char *format, ...);
VALUE rb_vsprintf(const char *format, va_list args);
VALUE rb_str_append(VALUE string, VALUE to_append);
void rb_str_set_len(VALUE string, long length);
VALUE rb_str_new_frozen(VALUE value);
#define rb_str_new4(value) rb_str_new_frozen(value)
VALUE rb_String(VALUE value);
VALUE rb_str_resize(VALUE string, long length);
#define RSTRING_GETMEM(string, data_pointer, length_pointer) ((data_pointer) = RSTRING_PTR(string), (length_pointer) = rb_str_len(string))
VALUE rb_str_split(VALUE string, const char *split);
void rb_str_modify(VALUE string);

// Symbol

ID rb_intern(const char *string);
ID rb_intern2(const char *string, long length);
#define rb_intern_const(str) rb_intern2((str), strlen(str))
VALUE rb_sym2str(VALUE string);

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
VALUE rb_ary_each(VALUE array);
VALUE rb_ary_unshift(VALUE array, VALUE value);
#define rb_assoc_new(a, b) rb_ary_new3(2, a, b)
VALUE rb_check_array_type(VALUE array);

// Hash

VALUE rb_hash_new(void);
VALUE rb_hash_aref(VALUE hash, VALUE key);
VALUE rb_hash_aset(VALUE hash, VALUE key, VALUE value);
VALUE rb_hash_lookup(VALUE hash, VALUE key);
VALUE rb_hash_lookup2(VALUE hash, VALUE key, VALUE default_value);
VALUE rb_hash_set_ifnone(VALUE hash, VALUE if_none);
#define RHASH_SET_IFNONE(hash, if_none) rb_hash_set_ifnone((VALUE) hash, if_none)

typedef unsigned long st_data_t;
typedef st_data_t st_index_t;

st_index_t rb_memhash(const void *data, long length);

// Class

const char* rb_class2name(VALUE module);
VALUE rb_class_real(VALUE ruby_class);
VALUE rb_class_superclass(VALUE ruby_class);
VALUE rb_class_of(VALUE object);
VALUE rb_obj_class(VALUE object);
VALUE CLASS_OF(VALUE object);
VALUE rb_obj_alloc(VALUE ruby_class);
VALUE rb_class_path(VALUE ruby_class);
VALUE rb_path2class(const char *string);

// Proc

VALUE rb_proc_new(void *function, VALUE value);

// Utilities

void rb_warn(const char *fmt, ...);
void rb_warning(const char *fmt, ...);

MUST_INLINE int rb_jt_scan_args_0_HASH(int argc, VALUE *argv, const char *format, VALUE *v1) {
  if (argc >= 1) *v1 = argv[0];
  return argc;
}

MUST_INLINE int rb_jt_scan_args_02(int argc, VALUE *argv, const char *format, VALUE *v1, VALUE *v2) {
  if (argc >= 1) *v1 = argv[0];
  if (argc >= 2) *v2 = argv[1];
  return argc;
}

MUST_INLINE int rb_jt_scan_args_11(int argc, VALUE *argv, const char *format, VALUE *v1, VALUE *v2) {
  if (argc < 1) {
    rb_jt_error("rb_jt_scan_args_11 error case not implemented");
    abort();
  }
  *v1 = argv[0];
  if (argc >= 2) *v2 = argv[1];
  return argc - 1;
}

MUST_INLINE int rb_jt_scan_args_12(int argc, VALUE *argv, const char *format, VALUE *v1, VALUE *v2, VALUE *v3) {
  if (argc < 1) {
    rb_jt_error("rb_jt_scan_args_12 error case not implemented");
    abort();
  }
  *v1 = argv[0];
  if (argc >= 2) *v2 = argv[1];
  if (argc >= 3) *v3 = argv[2];
  return argc - 1;
}

int rb_scan_args(int argc, VALUE *argv, const char *format, ...);

// Calls

int rb_respond_to(VALUE object, ID name);

#define rb_funcall(object, name, ...) truffle_invoke(RUBY_CEXT, "rb_funcall", (void *)object, name, __VA_ARGS__)
VALUE rb_funcallv(VALUE object, ID name, int args_count, const VALUE *args);
VALUE rb_funcallv_public(VALUE object, ID name, int args_count, const VALUE *args);
#define rb_funcall2 rb_funcallv
#define rb_funcall3 rb_funcallv_public
VALUE rb_apply(VALUE object, ID name, VALUE args);

#define RUBY_BLOCK_CALL_FUNC_TAKES_BLOCKARG 1
#define RB_BLOCK_CALL_FUNC_ARGLIST(yielded_arg, callback_arg) VALUE yielded_arg, VALUE callback_arg, int __args_count, const VALUE *__args, VALUE __block_arg
typedef VALUE rb_block_call_func(RB_BLOCK_CALL_FUNC_ARGLIST(yielded_arg, callback_arg));
typedef rb_block_call_func *rb_block_call_func_t;
VALUE rb_block_call(VALUE object, ID name, int args_count, const VALUE *args, rb_block_call_func_t block_call_func, VALUE data);

VALUE rb_call_super(int args_count, const VALUE *args);

int rb_block_given_p();
VALUE rb_block_proc(void);
VALUE rb_yield(VALUE value);

// Instance variables

VALUE rb_iv_get(VALUE object, const char *name);
VALUE rb_iv_set(VALUE object, const char *name, VALUE value);

VALUE rb_ivar_get(VALUE object, ID name);
VALUE rb_ivar_set(VALUE object, ID name, VALUE value);

VALUE rb_ivar_lookup(VALUE object, const char *name, VALUE default_value);
VALUE rb_attr_get(VALUE object, const char *name);

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

VALUE rb_exc_new3(VALUE exception_class, VALUE message);

NORETURN(void rb_exc_raise(VALUE exception));
NORETURN(void rb_raise(VALUE exception, const char *format, ...));

VALUE rb_protect(VALUE (*function)(VALUE), VALUE data, int *status);
void rb_jump_tag(int status);

void rb_set_errinfo(VALUE error);

void rb_syserr_fail(int errno, const char *message);
void rb_sys_fail(const char *message);

// Defining classes, modules and methods

VALUE rb_define_class(const char *name, VALUE superclass);
VALUE rb_define_class_under(VALUE module, const char *name, VALUE superclass);
VALUE rb_define_class_id_under(VALUE module, ID name, VALUE superclass);
VALUE rb_define_module(const char *name);
VALUE rb_define_module_under(VALUE module, const char *name);
void rb_include_module(VALUE module, VALUE to_include);

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

void rb_attr(VALUE ruby_class, ID name, int read, int write, int ex);

typedef VALUE (*rb_alloc_func_t)(VALUE ruby_class);
void rb_define_alloc_func(VALUE ruby_class, rb_alloc_func_t alloc_function);

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

#define RB_GC_GUARD(v) \
    (*__extension__ ({volatile VALUE *rb_gc_guarded_ptr = &(v); rb_gc_guarded_ptr;}))

void rb_gc_register_address(VALUE *address);
#define rb_global_variable(address) ;
VALUE rb_gc_enable();
VALUE rb_gc_disable();

// Threads

typedef void *(*gvl_call)(void *);
typedef void rb_unblock_function_t(void *);

void *rb_thread_call_with_gvl(gvl_call function, void *data1);
void *rb_thread_call_without_gvl(gvl_call function, void *data1, rb_unblock_function_t *unblock_function, void *data2);
void *rb_thread_call_without_gvl2(gvl_call function, void *data1, rb_unblock_function_t *unblock_function, void *data2);

typedef void *rb_nativethread_id_t;
typedef void *rb_nativethread_lock_t;

rb_nativethread_id_t rb_nativethread_self();
int rb_nativethread_lock_initialize(rb_nativethread_lock_t *lock);
int rb_nativethread_lock_destroy(rb_nativethread_lock_t *lock);
int rb_nativethread_lock_lock(rb_nativethread_lock_t *lock);
int rb_nativethread_lock_unlock(rb_nativethread_lock_t *lock);

// IO

typedef struct rb_io_t {
  int fd;
} rb_io_t;

#define rb_update_max_fd(fd) {}

void rb_io_check_writable(rb_io_t *io);
void rb_io_check_readable(rb_io_t *io);

int rb_cloexec_dup(int oldfd);
void rb_fd_fix_cloexec(int fd);

int rb_jt_io_handle(VALUE file);

#define GetOpenFile(file, pointer) ((pointer) = truffle_managed_malloc(sizeof(rb_io_t)), (pointer)->fd = rb_jt_io_handle(file))

int rb_io_wait_readable(int fd);
int rb_io_wait_writable(int fd);
void rb_thread_wait_fd(int fd);

NORETURN(void rb_eof_error(void));

// Objects

struct RBasic {
  // Empty
};

// Data

struct RData {
  struct RBasic basic;
  void (*dmark)(void *data);
  void (*dfree)(void *data);
  void *data;
};

struct RData *rb_jt_adapt_rdata(VALUE value);

#define RDATA(value) rb_jt_adapt_rdata(value)

#define DATA_PTR(value) (RDATA(value)->data)

// Typed data

typedef struct rb_data_type_struct rb_data_type_t;

struct rb_data_type_struct {
  const char *wrap_struct_name;
  struct {
    void (*dmark)(void *data);
    void (*dfree)(void *data);
    size_t (*dsize)(const void *data);
    void *reserved[2];
  } function;
  const rb_data_type_t *parent;
  void *data;
  VALUE flags;
};

struct RTypedData {
  struct RBasic basic;
  const rb_data_type_t *type;
  VALUE typed_flag;
  void *data;
};

#define RUBY_TYPED_FREE_IMMEDIATELY 1

#define RTYPEDDATA(value) ((struct RTypedData *)RDATA(value))

#define RTYPEDDATA_DATA(value) (RTYPEDDATA(value)->data)

VALUE rb_data_typed_object_wrap(VALUE ruby_class, void *data, const rb_data_type_t *data_type);

#define TypedData_Wrap_Struct(ruby_class, data_type, data) rb_data_typed_object_wrap((ruby_class), (data), (data_type))

VALUE rb_data_typed_object_zalloc(VALUE ruby_class, size_t size, const rb_data_type_t *data_type);

#define TypedData_Make_Struct0(result, ruby_class, type, size, data_type, sval) \
    VALUE result = rb_data_typed_object_zalloc(ruby_class, size, data_type); \
    (void)((sval) = (type *)DATA_PTR(result));

VALUE rb_data_typed_object_make(VALUE ruby_class, const rb_data_type_t *type, void **data_pointer, size_t size);

#define TypedData_Make_Struct(ruby_class, type, data_type, sval) rb_data_typed_object_make((ruby_class), (data_type), (void **)&(sval), sizeof(type))

void *rb_check_typeddata(VALUE value, const rb_data_type_t *data_type);

#define TypedData_Get_Struct(value, type, data_type, variable) ((variable) = (type *)rb_check_typeddata((value), (data_type)))

// VM

VALUE *rb_ruby_verbose_ptr(void);
#define ruby_verbose (*rb_ruby_verbose_ptr())

VALUE *rb_ruby_debug_ptr(void);
#define ruby_debug (*rb_ruby_debug_ptr())

#if defined(__cplusplus)
}
#endif

#endif
