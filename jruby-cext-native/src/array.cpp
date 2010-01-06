/*
 * Copyright (C) 2008, 2009 Wayne Meissner
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

/* Array */
/* The same value as 1.8.x */
#define ARRAY_DEFAULT_SIZE 16

using namespace jruby;

extern "C" VALUE 
rb_Array(VALUE val) 
{
    return callMethod(getClass("Array"), "new", 1, val);
}

extern "C" VALUE 
rb_ary_new2(long length) 
{
    VALUE num = INT2NUM(length);
    return callMethod(getClass("Array"), "new", 1, num);
}

extern "C" VALUE
rb_ary_new(void) 
{
    return callMethod(getClass("Array"), "new", 0);
}

extern "C" VALUE 
rb_ary_new4(long n, const VALUE* argv) 
{
    VALUE ary = rb_ary_new();

    for (int i = 0; i < n; ++i) {
        rb_ary_push(ary, argv[i]);
    }

    return ary;
}

extern "C" VALUE 
rb_ary_push(VALUE array, VALUE val) 
{
    return callMethod(array, "push", 1, val);
}

extern "C" VALUE 
rb_ary_pop(VALUE array) {
    return callMethod(array, "pop", 0);
}

extern "C" VALUE 
rb_ary_entry(VALUE array, int offset) 
{
    return callMethod(array, "[]", 1, INT2NUM(offset));
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
    return callMethod(array, "unshift", 1);
}

extern "C" VALUE 
rb_ary_shift(VALUE array) 
{
    return callMethod(array, "shift", 0);
}
