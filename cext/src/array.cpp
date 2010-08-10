/*
 * Copyright (C) 2008-2010 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <jni.h>
#include "jruby.h"
#include "ruby.h"
#include "Handle.h"
#include "JLocalEnv.h"

/* Array */
/* The same value as 1.8.x */
#define ARRAY_DEFAULT_SIZE 16

using namespace jruby;

struct RArray*
RubyArray::toRArray()
{
    JLocalEnv env;
    int length;
    jobjectArray elements;

    length = (int)(env->GetIntField(obj, RubyArray_length_field));
    elements = (jobjectArray)(env->CallObjectMethod(obj, RubyArray_toJavaArray_method));

    if (length > 0) {
        int i;
        VALUE* ptr = (VALUE*)xmalloc(sizeof(VALUE) * length);
        for (i = 0; i < length; i++) {
            ptr[i] = objectToValue(env, env->GetObjectArrayElement(elements, i));
        }
        rarray.ptr = ptr;
    } else {
        rarray.ptr = NULL;
    }
    rarray.len = length;
    return &rarray;
}

extern "C" struct RArray*
jruby_rarray(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_ARRAY) {
        return ((RubyArray *) h)->toRArray();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Array)");
}

extern "C" VALUE
rb_Array(VALUE val)
{
    return callMethod(rb_cArray, "new", 1, val);
}

extern "C" VALUE
rb_ary_new2(long length)
{
    // FIXME: Potential segfault
    // MRI sets the internal buffer to the specified size here to
    // allow writing to the array from C. Extensions might not
    // check the size after calling this method
    return callMethod(rb_cArray, "new", 0);
}

extern "C" VALUE
rb_ary_new(void)
{
    return callMethod(rb_cArray, "new", 0);
}

extern "C" VALUE
rb_ary_new3(long size, ...)
{
    va_list args;
    VALUE ary = rb_ary_new2(size);

    va_start(args, size);
    for (long i = 0; i < size; i++) {
        rb_ary_push(ary, va_arg(args, VALUE));
    }
    va_end(args);
    return ary;
}

extern "C" VALUE
rb_ary_new4(long n, const VALUE* argv)
{
    VALUE ary = rb_ary_new();

    for (long i = 0; i < n; ++i) {
        rb_ary_push(ary, argv[i]);
    }

    return ary;
}

extern "C" VALUE
rb_assoc_new(VALUE key, VALUE value) {
    VALUE ary = rb_ary_new2(2);
    rb_ary_push(ary, key);
    rb_ary_push(ary, value);
    return ary;
}

extern "C" VALUE
rb_ary_push(VALUE array, VALUE val)
{
    return callMethod(array, "push", 1, val);
}

extern "C" VALUE
rb_ary_pop(VALUE array)
{
    return callMethod(array, "pop", 0);
}

extern "C" VALUE
rb_ary_entry(VALUE array, long offset)
{
    return callMethod(array, "[]", 1, LONG2NUM(offset));
}

extern "C" VALUE
rb_ary_clear(VALUE array)
{
    return callMethod(array, "clear", 0);
}

VALUE
rb_ary_dup(VALUE array)
{
    return callMethod(array, "dup", 0);
}

extern "C" VALUE
rb_ary_join(VALUE array1, VALUE array2)
{
    return callMethod(array1, "join", 1, array2);
}

extern "C" VALUE
rb_ary_reverse(VALUE array)
{
    return callMethod(array, "reverse", 0);
}

extern "C" VALUE
rb_ary_unshift(VALUE array, VALUE val)
{
    return callMethod(array, "unshift", 1, val);
}

extern "C" VALUE
rb_ary_shift(VALUE array)
{
    return callMethod(array, "shift", 0);
}

extern "C" void
rb_ary_store(VALUE array, long offset, VALUE val)
{
    callMethod(array, "[]=", 2, LONG2NUM(offset), val);
}

extern "C" VALUE
rb_ary_includes(VALUE ary, VALUE item)
{
    return callMethodA(ary, "include?", 1, &item);
}

extern "C" VALUE
rb_ary_delete(VALUE ary, VALUE item)
{
    return callMethodA(ary, "delete", 1, &item);
}

extern "C" VALUE
rb_ary_delete_at(VALUE ary, long pos)
{
    return callMethod(ary, "delete_at", 1, LONG2NUM(pos));
}

extern "C" VALUE
rb_ary_aref(int argc, VALUE* argv, VALUE ary)
{
    return callMethodA(ary, "slice", argc, argv);
}

extern "C" VALUE
rb_check_array_type(VALUE val)
{
    return rb_check_convert_type(val, 0, "Array", "to_ary");
}

extern "C" int
rb_ary_size(VALUE ary)
{
    return jruby_rarray(ary)->len;
}

// Copyied from Rbx
// Really just used as a placeholder/sentinal value to half implement
// rb_iterate
extern "C" VALUE
rb_each(VALUE ary)
{
    rb_raise(rb_eArgError, "rb_each not fully supported", 0);
    return Qnil;
}

extern "C" VALUE
rb_iterate(VALUE(*ifunc)(VALUE), VALUE ary, VALUE(*cb)(ANYARGS), VALUE cb_data)
{
    if (ifunc != rb_each || !rb_obj_is_kind_of(ary, rb_cArray)) {
        rb_raise(rb_eArgError, "rb_iterate only supported with rb_each and an Array");
        return Qnil;
    }

    for (int i = 0; i < rb_ary_size(ary); i++) {
        (*cb)(rb_ary_entry(ary, i), cb_data, Qnil);
    }

    return ary;
}
