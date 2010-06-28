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

#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"
#include "JavaException.h"

using namespace jruby;



static jthrowable
newRaiseException(JNIEnv* env, VALUE exc, const char* fmt, va_list ap)
{
    char message[1024];
    vsnprintf(message, sizeof(message) - 1,fmt, ap);

    return (jthrowable) env->NewObject(RaiseException_class, RaiseException_constructor,
            getRuntime(), valueToObject(env, exc), env->NewStringUTF(message), true);
}


extern "C" void
rb_raise(VALUE exc, const char *fmt, ...)
{
    JLocalEnv env;
    va_list ap;
    va_start(ap, fmt);
    jthrowable jException = newRaiseException(env, exc, fmt, ap);
    va_end(ap);
    checkExceptions(env);
    throw JavaException(env, jException);
}

extern "C" void
rb_fatal(const char *fmt, ...)
{
    JLocalEnv env;
    va_list ap;
    va_start(ap, fmt);
    jthrowable jException = newRaiseException(env, rb_eFatal, fmt, ap);
    va_end(ap);
    checkExceptions(env);
    throw JavaException(env, jException);
}

extern "C" void
rb_bug(const char *fmt, ...)
{
    JLocalEnv env;
    va_list ap;
    va_start(ap, fmt);
    jthrowable jException = newRaiseException(env, rb_eFatal, fmt, ap);
    va_end(ap);
    checkExceptions(env);
    throw JavaException(env, jException);
}
