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
rb_warn(const char *fmt, ...) {
    va_list args;
    char msg[RB_EXC_BUFSIZE];

    va_start(args, fmt);
    vsnprintf(msg, RB_EXC_BUFSIZE, fmt, args);
    va_end(args);

    rb_funcall(rb_mKernel, rb_intern("warn"), 1, rb_str_new2(msg));
}

extern "C" void
rb_warning(const char *fmt, ...) {
    va_list args;
    char msg[RB_EXC_BUFSIZE];

    va_start(args, fmt);
    vsnprintf(msg, RB_EXC_BUFSIZE, fmt, args);
    va_end(args);

    rb_funcall(rb_mKernel, rb_intern("warning"), 1, rb_str_new2(msg));
}

extern "C" VALUE
rb_yield(VALUE argument) {
    JLocalEnv env;
    jobject retval = env->CallStaticObjectMethod(JRuby_class, JRuby_yield, getRuntime(), valueToObject(env, argument));
    checkExceptions(env);
    return objectToValue(env, retval);
}

extern "C" int
rb_block_given_p() {
    JLocalEnv env;
    return (int)(env->CallStaticIntMethod(JRuby_class, JRuby_blockGiven, getRuntime()));
}
