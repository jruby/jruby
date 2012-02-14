/*
 * Copyright (C) 2010, Tim Felgentreff
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
 *
 */

#include <stdarg.h>

#include "ruby.h"
#include "jruby.h"
#include "Handle.h"
#include "JLocalEnv.h"
#include "JUtil.h"

using namespace jruby;

/** Copied from Rubinius */
#define RB_EXC_BUFSIZE 256

extern "C" void
rb_warn(const char *fmt, ...)
{
    va_list args;
    char msg[RB_EXC_BUFSIZE];

    va_start(args, fmt);
    vsnprintf(msg, RB_EXC_BUFSIZE, fmt, args);
    va_end(args);

    callMethod(rb_mKernel, "warn", 1, rb_str_new_cstr(msg));
}

extern "C" void
rb_warning(const char *fmt, ...)
{
    va_list args;
    char msg[RB_EXC_BUFSIZE];

    va_start(args, fmt);
    vsnprintf(msg, RB_EXC_BUFSIZE, fmt, args);
    va_end(args);

    callMethod(rb_mKernel, "warning", 1, rb_str_new_cstr(msg));
}

extern "C" VALUE
rb_yield(VALUE argument)
{
    return rb_yield_splat(rb_ary_new3(1, argument));
}

extern "C" VALUE
rb_yield_splat(VALUE array)
{
    JLocalEnv env;
    jobject retval = env->CallStaticObjectMethod(JRuby_class, JRuby_yield, getRuntime(), valueToObject(env, array));
    checkExceptions(env);

    return objectToValue(env, retval);
}

extern "C" VALUE
rb_yield_values(int n, ...)
{
    va_list varargs;
    VALUE ary = rb_ary_new2(n);

    va_start(varargs, n);
    while (n--) {
        rb_ary_push(ary, va_arg(varargs, VALUE));
    }
    va_end(varargs);

    return rb_yield_splat(ary);
}

extern "C" int
rb_block_given_p()
{
    JLocalEnv env;
    return (int)(env->CallStaticIntMethod(JRuby_class, JRuby_blockGiven, getRuntime()));
}

extern "C" void
rb_need_block() {
  if (!rb_block_given_p()) {
    rb_raise(rb_eLocalJumpError, "no block given", 0);
  }
}

extern "C" VALUE
rb_block_proc()
{
    JLocalEnv env;
    jobject proc = env->CallStaticObjectMethod(JRuby_class, JRuby_getBlockProc, getRuntime());
    checkExceptions(env);

    return objectToValue(env, proc);
}

extern "C" VALUE
rb_require(const char* name)
{
    return callMethod(rb_mKernel, "require", 1, rb_str_new_cstr(name));
}

extern "C" void
rb_define_alias(VALUE klass, const char* new_name, const char* old_name)
{
    JLocalEnv env;
    jmethodID mid = getCachedMethodID(env, RubyModule_class, "defineAlias",
            "(Ljava/lang/String;Ljava/lang/String;)V");
    env->CallVoidMethod(valueToObject(env, klass), mid, env->NewStringUTF(new_name),
            env->NewStringUTF(old_name));
    checkExceptions(env);
}

extern "C" VALUE
rb_f_sprintf(int argc, const VALUE* argv)
{
    VALUE ary = rb_ary_new4(argc-1, argv+1);
    return callMethod(argv[0], "%", 1, ary);
}
