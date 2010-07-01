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
#include <string.h>
#include <pthread.h>
#include <sys/param.h>
#include <jni.h>

#include "JLocalEnv.h"
#include "jruby.h"
#include "JUtil.h"
#include "ruby.h"

using namespace jruby;


static VALUE
newString(const char* ptr, int len, int capacity = 0, bool tainted = false)
{
    JLocalEnv env;

    jbyteArray bytes = env->NewByteArray(capacity > len ? capacity : len);
    checkExceptions(env);

    if (len > 0 && ptr != NULL) {
        env->SetByteArrayRegion(bytes, 0, len, (jbyte *) ptr);
        checkExceptions(env);
    }

    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_newString, jruby::getRuntime(), bytes, (jboolean) tainted);
    checkExceptions(env);

    return (VALUE) result;
}

extern "C" VALUE
rb_str_new(const char *ptr, long len)
{
    return newString(ptr, len);
}

extern "C" VALUE
rb_str_new_cstr(const char *ptr)
{
    return newString(ptr, ptr ? strlen(ptr) : 0);
}

#define STR_BUF_MIN_SIZE 128
extern "C" VALUE
rb_str_buf_new(long capacity)
{
    capacity = MAX(capacity, STR_BUF_MIN_SIZE);

    return newString(NULL, 0, MAX(capacity, STR_BUF_MIN_SIZE), false);
}

extern "C" VALUE
rb_str_dup(VALUE str)
{
    return callMethod(str, "dup", 0, NULL);
}

extern "C" VALUE
rb_str_append(VALUE str, VALUE str2)
{
    return callMethod(str, "append", 1, str2);
}

extern "C" VALUE
rb_str_cat(VALUE str, const char *ptr, long len)
{
    return callMethod(str, "concat", 1, rb_str_new(ptr, len));
}

extern "C" VALUE
rb_str_plus(VALUE str1, VALUE str2)
{
    return callMethod(str1, "+", 1, str2);
}

extern "C" VALUE
rb_str_buf_cat(VALUE str, const char *ptr, long len)
{
    return callMethod(str, "concat", 1, rb_str_new(ptr, len));
}

extern "C" int
rb_str_cmp(VALUE str1, VALUE str2)
{
    return NUM2INT(callMethod(str1, "<=>", 1, str2));
}

extern "C" VALUE
rb_str_split(VALUE str, const char *sep)
{
    return callMethod(str, "split", 1, rb_str_new2(sep));
}

extern "C" VALUE
rb_str2inum(VALUE str, int base)
{
    return callMethod(str, "to_i", 1, INT2NUM(base));
}

extern "C" VALUE
rb_cstr2inum(const char* str, int base)
{
    return callMethod(rb_str_new2(str), "to_i", 1, INT2NUM(base));
}

extern "C" VALUE
rb_str_substr(VALUE str, long beg, long len)
{
    return callMethod(str, "substr", 2, LONG2NUM(beg), LONG2NUM(len));
}

extern "C" VALUE
rb_tainted_str_cstr(const char *ptr)
{
    int len = strlen(ptr);

    return newString(ptr, len, len, true);
}

extern "C" VALUE
rb_tainted_str_new(const char* ptr, long len)
{
    return newString(ptr, len, len, true);
}

extern "C" VALUE
rb_obj_as_string(VALUE obj)
{
    return callMethod(obj, "to_s", 0);
}

extern "C" VALUE
rb_str_to_str(VALUE obj)
{
    return callMethod(obj, "to_str", 0);
}

extern "C" VALUE
rb_string_value(VALUE* ptr)
{
    return *ptr = callMethod(*ptr, "to_str", 0);
}

extern "C" const char*
rb_str_ptr_readonly(VALUE obj)
{
    JLocalEnv env;
    char* ctext;
    printf("Get me a pointer from %p!\n", (void*)obj);
    //valueToObject(env, obj);
    //printf("valueToObject works\n");
    jobject rubyString = valueToObject(env, obj);
    printf("Got me rubyString object (%p)\n", (void*)rubyString);
    jmethodID mid = getMethodID(env, RubyString_class, "setTaint", "(Z)V");
    printf("Got me toString method (%p)\n", (void*)mid);
    jstring javaString = (jstring)(env->CallObjectMethod(rubyString, mid));
    printf("Got me java String (%p)\n", (void*)javaString);
    env->GetStringUTFChars(javaString, NULL);
    printf("Got me cstring\n");
    return ctext;
}


