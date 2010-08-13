/*
 * Copyright (C) 2010 Wayne Meissner, Tim Felgentreff
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

#include <sys/select.h>

#include "ruby.h"
#include "jruby.h"
#include "JLocalEnv.h"

using namespace jruby;

static void GIL_releaseNoCleanup();

VALUE rb_thread_critical = 0;

extern "C" VALUE
rb_thread_local_aref(VALUE thread, ID id)
{
    return callMethod(thread, "[]", 1, ID2SYM(id));
}

extern "C" VALUE
rb_thread_local_aset(VALUE thread, ID id, VALUE value)
{
    return callMethod(thread, "[]=", 2, ID2SYM(id), value);
}

extern "C" int
rb_thread_alone()
{
    return rb_ary_size(callMethodA(rb_cThread, "list", 0, NULL)) < 2;
}

extern "C" void
rb_thread_schedule()
{
    callMethod(rb_cThread, "pass", 0);
}

extern "C" int
rb_thread_select(int max, fd_set * read, fd_set * write, fd_set * except, struct timeval *timeout)
{
    JLocalEnv env;

    if (!read && !write && !except) {
        // Just sleep for the specified amount of time
        long interval = timeout ? (timeout->tv_sec * 1000 + timeout->tv_usec / 1000) : 0;
        env->CallStaticVoidMethod(JRuby_class, JRuby_threadSleep, getRuntime(), (jint)interval);
        checkExceptions(env);
        return 0;
    } else {
        int ret = select(max, read, write, except, timeout);
        // TODO: Check for async events and possibly exceptions
        return ret;
    }
}

extern "C" VALUE
rb_thread_current(void)
{
    return callMethod(rb_cThread, "current", 0);
}

extern "C" VALUE
rb_thread_blocking_region(rb_blocking_function_t func, void* data, rb_unblock_function_t, void*)
{
    // unblock function is ignored, Rubinius does it, too, so it can't be too bad to get exts working
    VALUE ret = Qnil;
    GIL_releaseNoCleanup();
    ret = (*func)(data);
    return ret;
}

static void
GIL_releaseNoCleanup() {
    JLocalEnv env;
    jclass gil = env->FindClass("org/jruby/cext/GIL");
    checkExceptions(env);
    jmethodID mid = getStaticMethodID(env, gil, "releaseNoCleanup", "()V");
    env->CallStaticVoidMethod(gil, mid);
    checkExceptions(env);
}
