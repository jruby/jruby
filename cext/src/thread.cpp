/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Wayne Meissner, Tim Felgentreff
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

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
