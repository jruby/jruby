#include "ruby.h"
#include "rubyspec.h"

#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_ARRAY
static VALUE array_spec_rb_Array(VALUE self, VALUE object) {
  return rb_Array(object);
}
#endif

#ifdef HAVE_RARRAY
static VALUE array_spec_RARRAY_ptr_assign(VALUE self, VALUE ary, VALUE content, VALUE length) {
  int i;
  for (i = 0; i < NUM2INT(length); i++) {
    RARRAY(ary)->ptr[i] = content;
  }
  RARRAY(ary)->len = i;
  return ary;
}

static VALUE array_spec_RARRAY_ptr_assign_call(VALUE self, VALUE array) {
  VALUE* ptr;

  ptr = RARRAY(array)->ptr;
  ptr[1] = INT2FIX(5);
  ptr[2] = INT2FIX(7);
  rb_ary_push(array, INT2FIX(9));

  return array;
}

static VALUE array_spec_RARRAY_ptr_assign_funcall(VALUE self, VALUE array) {
  VALUE* ptr;

  ptr = RARRAY(array)->ptr;
  ptr[1] = INT2FIX(1);
  ptr[2] = INT2FIX(2);
  rb_funcall(array, rb_intern("<<"), 1, INT2FIX(3));

  return array;
}

static VALUE array_spec_RARRAY_len(VALUE self, VALUE array) {
  return INT2FIX(RARRAY(array)->len);
}

static VALUE array_spec_RARRAY_ptr_iterate(VALUE self, VALUE array) {
  int i;
  VALUE* ptr;

  ptr = RARRAY(array)->ptr;
  for(i = 0; i < RARRAY_LEN(array); i++) {
    rb_yield(ptr[i]);
  }
  return Qnil;
}
#endif

#if defined(HAVE_RARRAY_LEN) && defined(HAVE_RARRAY_PTR)
static VALUE array_spec_RARRAY_PTR_iterate(VALUE self, VALUE array) {
  int i;
  VALUE* ptr;

  ptr = RARRAY_PTR(array);
  for(i = 0; i < RARRAY_LEN(array); i++) {
    rb_yield(ptr[i]);
  }
  return Qnil;
}

static VALUE array_spec_RARRAY_PTR_assign(VALUE self, VALUE array, VALUE value) {
  int i;
  VALUE* ptr;

  ptr = RARRAY_PTR(array);
  for(i = 0; i < RARRAY_LEN(array); i++) {
    ptr[i] = value;
  }
  return Qnil;
}

static VALUE array_spec_RARRAY_LEN(VALUE self, VALUE array) {
  return INT2FIX(RARRAY_LEN(array));
}
#endif

#ifdef HAVE_RB_ARY_AREF
static VALUE array_spec_rb_ary_aref(int argc, VALUE *argv, VALUE self) {
  VALUE ary, args;
  rb_scan_args(argc, argv, "1*", &ary, &args);
  return rb_ary_aref((int)RARRAY_LEN(args), RARRAY_PTR(args), ary);
}
#endif

#ifdef HAVE_RB_ARY_CLEAR
static VALUE array_spec_rb_ary_clear(VALUE self, VALUE array) {
  return rb_ary_clear(array);
}
#endif

#ifdef HAVE_RB_ARY_DELETE
static VALUE array_spec_rb_ary_delete(VALUE self, VALUE array, VALUE item) {
  return rb_ary_delete(array, item);
}
#endif

#ifdef HAVE_RB_ARY_DELETE_AT
static VALUE array_spec_rb_ary_delete_at(VALUE self, VALUE array, VALUE index) {
  return rb_ary_delete_at(array, NUM2LONG(index));
}
#endif

#ifdef HAVE_RB_ARY_DUP
static VALUE array_spec_rb_ary_dup(VALUE self, VALUE array) {
  return rb_ary_dup(array);
}
#endif

#ifdef HAVE_RB_ARY_ENTRY
static VALUE array_spec_rb_ary_entry(VALUE self, VALUE array, VALUE offset) {
  return rb_ary_entry(array, FIX2INT(offset));
}
#endif

#ifdef HAVE_RB_ARY_INCLUDES
static VALUE array_spec_rb_ary_includes(VALUE self, VALUE ary, VALUE item) {
  return rb_ary_includes(ary, item);
}
#endif

#ifdef HAVE_RB_ARY_JOIN
static VALUE array_spec_rb_ary_join(VALUE self, VALUE array1, VALUE array2) {
  return rb_ary_join(array1, array2);
}
#endif

#ifdef HAVE_RB_ARY_TO_S
static VALUE array_spec_rb_ary_to_s(VALUE self, VALUE array) {
  return rb_ary_to_s(array);
}
#endif

#ifdef HAVE_RB_ARY_NEW
static VALUE array_spec_rb_ary_new(VALUE self) {
  VALUE ret;
  ret = rb_ary_new();
  return ret;
}
#endif

#ifdef HAVE_RB_ARY_NEW2
static VALUE array_spec_rb_ary_new2(VALUE self, VALUE length) {
  return rb_ary_new2(NUM2LONG(length));
}
#endif

#ifdef HAVE_RB_ARY_NEW3
static VALUE array_spec_rb_ary_new3(VALUE self, VALUE first, VALUE second, VALUE third) {
  return rb_ary_new3(3, first, second, third);
}
#endif

#ifdef HAVE_RB_ARY_NEW_FROM_ARGS
static VALUE array_spec_rb_ary_new_from_args(VALUE self, VALUE first, VALUE second, VALUE third) {
  return rb_ary_new_from_args(3, first, second, third);
}
#endif

#ifdef HAVE_RB_ARY_NEW4
static VALUE array_spec_rb_ary_new4(VALUE self, VALUE first, VALUE second, VALUE third) {
  VALUE values[3];
  values[0] = first;
  values[1] = second;
  values[2] = third;
  return rb_ary_new4(3, values);
}
#endif

#ifdef HAVE_RB_ARY_POP
static VALUE array_spec_rb_ary_pop(VALUE self, VALUE array) {
  return rb_ary_pop(array);
}
#endif

#ifdef HAVE_RB_ARY_PUSH
static VALUE array_spec_rb_ary_push(VALUE self, VALUE array, VALUE item) {
  rb_ary_push(array, item);
  return array;
}
#endif

#ifdef HAVE_RB_ARY_REVERSE
static VALUE array_spec_rb_ary_reverse(VALUE self, VALUE array) {
  return rb_ary_reverse(array);
}
#endif

#ifdef HAVE_RB_ARY_SHIFT
static VALUE array_spec_rb_ary_shift(VALUE self, VALUE array) {
  return rb_ary_shift(array);
}
#endif

#ifdef HAVE_RB_ARY_STORE
static VALUE array_spec_rb_ary_store(VALUE self, VALUE array, VALUE offset, VALUE value) {
  rb_ary_store(array, FIX2INT(offset), value);

  return Qnil;
}
#endif

#ifdef HAVE_RB_ARY_CONCAT
static VALUE array_spec_rb_ary_concat(VALUE self, VALUE array1, VALUE array2) {
  return rb_ary_concat(array1, array2);
}
#endif

#ifdef HAVE_RB_ARY_PLUS
static VALUE array_spec_rb_ary_plus(VALUE self, VALUE array1, VALUE array2) {
  return rb_ary_plus(array1, array2);
}
#endif

#ifdef HAVE_RB_ARY_UNSHIFT
static VALUE array_spec_rb_ary_unshift(VALUE self, VALUE array, VALUE val) {
  return rb_ary_unshift(array, val);
}
#endif

#ifdef HAVE_RB_ASSOC_NEW
static VALUE array_spec_rb_assoc_new(VALUE self, VALUE first, VALUE second) {
  return rb_assoc_new(first, second);
}
#endif

#if defined(HAVE_RB_ITERATE) && defined(HAVE_RB_EACH)
static VALUE copy_ary(VALUE el, VALUE new_ary) {
  return rb_ary_push(new_ary, el);
}

static VALUE array_spec_rb_iterate(VALUE self, VALUE ary) {
  VALUE new_ary = rb_ary_new();

  rb_iterate(rb_each, ary, copy_ary, new_ary);

  return new_ary;
}

static VALUE sub_pair(VALUE el, VALUE holder) {
  return rb_ary_push(holder, rb_ary_entry(el, 1));
}

static VALUE each_pair(VALUE obj) {
  return rb_funcall(obj, rb_intern("each_pair"), 0);
}

static VALUE array_spec_rb_iterate_each_pair(VALUE self, VALUE obj) {
  VALUE new_ary = rb_ary_new();

  rb_iterate(each_pair, obj, sub_pair, new_ary);

  return new_ary;
}

static VALUE iter_yield(VALUE el, VALUE ary) {
  rb_yield(el);
  return Qnil;
}

static VALUE array_spec_rb_iterate_then_yield(VALUE self, VALUE obj) {
  rb_iterate(rb_each, obj, iter_yield, obj);
  return Qnil;
}
#endif

#if defined(HAVE_RB_MEM_CLEAR)
static VALUE array_spec_rb_mem_clear(VALUE self, VALUE obj) {
  VALUE ary = obj;

  rb_mem_clear(&ary, 1);

  return ary;
}
#endif

#if defined(HAVE_RB_PROTECT_INSPECT) && defined(HAVE_RB_INSPECTING_P)

static VALUE rec_pi(VALUE obj, VALUE arg) {
  if(RTEST(rb_inspecting_p(obj))) return arg;
  return Qfalse;
}

static VALUE array_spec_rb_protect_inspect(VALUE self, VALUE obj) {
  return rb_protect_inspect(rec_pi, obj, Qtrue);
}

#endif

#ifdef HAVE_RB_ARY_FREEZE
static VALUE array_spec_rb_ary_freeze(VALUE self, VALUE ary) {
  return rb_ary_freeze(ary);
}
#endif

#ifdef HAVE_RB_ARY_TO_ARY
static VALUE array_spec_rb_ary_to_ary(VALUE self, VALUE ary) {
  return rb_ary_to_ary(ary);
}
#endif

#ifdef HAVE_RB_ARY_SUBSEQ
static VALUE array_spec_rb_ary_subseq(VALUE self, VALUE ary, VALUE begin, VALUE len) {
  return rb_ary_subseq(ary, FIX2LONG(begin), FIX2LONG(len));
}
#endif

void Init_array_spec() {
  VALUE cls;
  cls = rb_define_class("CApiArraySpecs", rb_cObject);

#ifdef HAVE_RB_ARRAY
  rb_define_method(cls, "rb_Array", array_spec_rb_Array, 1);
#endif

#ifdef HAVE_RARRAY
  rb_define_method(cls, "RARRAY_ptr_iterate", array_spec_RARRAY_ptr_iterate, 1);
  rb_define_method(cls, "RARRAY_ptr_assign", array_spec_RARRAY_ptr_assign, 3);
  rb_define_method(cls, "RARRAY_ptr_assign_call",
      array_spec_RARRAY_ptr_assign_call, 1);
  rb_define_method(cls, "RARRAY_ptr_assign_funcall",
      array_spec_RARRAY_ptr_assign_funcall, 1);
  rb_define_method(cls, "RARRAY_len", array_spec_RARRAY_len, 1);
#endif

#if defined(HAVE_RARRAY_LEN) && defined(HAVE_RARRAY_PTR)
  rb_define_method(cls, "RARRAY_LEN", array_spec_RARRAY_LEN, 1);
  rb_define_method(cls, "RARRAY_PTR_iterate", array_spec_RARRAY_PTR_iterate, 1);
  rb_define_method(cls, "RARRAY_PTR_assign", array_spec_RARRAY_PTR_assign, 2);
#endif

#ifdef HAVE_RB_ARY_AREF
  rb_define_method(cls, "rb_ary_aref", array_spec_rb_ary_aref, -1);
#endif

#ifdef HAVE_RB_ARY_CLEAR
  rb_define_method(cls, "rb_ary_clear", array_spec_rb_ary_clear, 1);
#endif

#ifdef HAVE_RB_ARY_DELETE
  rb_define_method(cls, "rb_ary_delete", array_spec_rb_ary_delete, 2);
#endif

#ifdef HAVE_RB_ARY_DELETE_AT
  rb_define_method(cls, "rb_ary_delete_at", array_spec_rb_ary_delete_at, 2);
#endif

#ifdef HAVE_RB_ARY_DUP
  rb_define_method(cls, "rb_ary_dup", array_spec_rb_ary_dup, 1);
#endif

#ifdef HAVE_RB_ARY_ENTRY
  rb_define_method(cls, "rb_ary_entry", array_spec_rb_ary_entry, 2);
#endif

#ifdef HAVE_RB_ARY_INCLUDES
  rb_define_method(cls, "rb_ary_includes", array_spec_rb_ary_includes, 2);
#endif

#ifdef HAVE_RB_ARY_JOIN
  rb_define_method(cls, "rb_ary_join", array_spec_rb_ary_join, 2);
#endif

#ifdef HAVE_RB_ARY_TO_S
  rb_define_method(cls, "rb_ary_to_s", array_spec_rb_ary_to_s, 1);
#endif

#ifdef HAVE_RB_ARY_NEW
  rb_define_method(cls, "rb_ary_new", array_spec_rb_ary_new, 0);
#endif

#ifdef HAVE_RB_ARY_NEW2
  rb_define_method(cls, "rb_ary_new2", array_spec_rb_ary_new2, 1);
#endif

#ifdef HAVE_RB_ARY_NEW3
  rb_define_method(cls, "rb_ary_new3", array_spec_rb_ary_new3, 3);
#endif

#ifdef HAVE_RB_ARY_NEW_FROM_ARGS
  rb_define_method(cls, "rb_ary_new_from_args", array_spec_rb_ary_new_from_args, 3);
#endif

#ifdef HAVE_RB_ARY_NEW4
  rb_define_method(cls, "rb_ary_new4", array_spec_rb_ary_new4, 3);
#endif

#ifdef HAVE_RB_ARY_POP
  rb_define_method(cls, "rb_ary_pop", array_spec_rb_ary_pop, 1);
#endif

#ifdef HAVE_RB_ARY_PUSH
  rb_define_method(cls, "rb_ary_push", array_spec_rb_ary_push, 2);
#endif

#ifdef HAVE_RB_ARY_REVERSE
  rb_define_method(cls, "rb_ary_reverse", array_spec_rb_ary_reverse, 1);
#endif

#ifdef HAVE_RB_ARY_SHIFT
  rb_define_method(cls, "rb_ary_shift", array_spec_rb_ary_shift, 1);
#endif

#ifdef HAVE_RB_ARY_STORE
  rb_define_method(cls, "rb_ary_store", array_spec_rb_ary_store, 3);
#endif

#ifdef HAVE_RB_ARY_CONCAT
  rb_define_method(cls, "rb_ary_concat", array_spec_rb_ary_concat, 2);
#endif

#ifdef HAVE_RB_ARY_PLUS
  rb_define_method(cls, "rb_ary_plus", array_spec_rb_ary_plus, 2);
#endif

#ifdef HAVE_RB_ARY_UNSHIFT
  rb_define_method(cls, "rb_ary_unshift", array_spec_rb_ary_unshift, 2);
#endif

#ifdef HAVE_RB_ASSOC_NEW
  rb_define_method(cls, "rb_assoc_new", array_spec_rb_assoc_new, 2);
#endif

#if defined(HAVE_RB_ITERATE) && defined(HAVE_RB_EACH)
  rb_define_method(cls, "rb_iterate", array_spec_rb_iterate, 1);
  rb_define_method(cls, "rb_iterate_each_pair", array_spec_rb_iterate_each_pair, 1);
  rb_define_method(cls, "rb_iterate_then_yield", array_spec_rb_iterate_then_yield, 1);
#endif

#if defined(HAVE_RB_MEM_CLEAR)
  rb_define_method(cls, "rb_mem_clear", array_spec_rb_mem_clear, 1);
#endif

#if defined(HAVE_RB_PROTECT_INSPECT) && defined(HAVE_RB_INSPECTING_P)
  rb_define_method(cls, "rb_protect_inspect",  array_spec_rb_protect_inspect, 1);
#endif

#ifdef HAVE_RB_ARY_FREEZE
  rb_define_method(cls, "rb_ary_freeze", array_spec_rb_ary_freeze, 1);
#endif

#ifdef HAVE_RB_ARY_TO_ARY
  rb_define_method(cls, "rb_ary_to_ary", array_spec_rb_ary_to_ary, 1);
#endif

#ifdef HAVE_RB_ARY_SUBSEQ
  rb_define_method(cls, "rb_ary_subseq", array_spec_rb_ary_subseq, 3);
#endif
}

#ifdef __cplusplus
}
#endif
