/*
 * Copyright (C) 2010 Wayne Meissner
 * Copyright (C) 1993-2007 Yukihiro Matsumoto
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

extern "C" VALUE
rb_exc_new(VALUE etype, const char *ptr, long len)
{
    return rb_funcall(etype, rb_intern("new"), 1, rb_str_new(ptr, len));
}

#undef rb_exc_new2

extern "C" VALUE
rb_exc_new2(VALUE etype, const char *s)
{
    return rb_exc_new(etype, s, strlen(s));
}

extern "C" VALUE
rb_exc_new3(VALUE etype, VALUE str)
{
    StringValue(str);
    return rb_funcall(etype, rb_intern("new"), 1, str);
}

extern "C" VALUE
rb_exc_raise(VALUE exc)
{
    using namespace jruby;
    JLocalEnv env;

    jmethodID ctor = getMethodID(env, RaiseException_class, "<init>", "(Lorg/jruby/RubyException;)V");
    jthrowable jException = (jthrowable) env->NewObject(RaiseException_class, ctor, valueToObject(env, exc));
    checkExceptions(env);

    throw JavaException(env, jException);
}


extern "C" VALUE
rb_ensure(VALUE (*b_proc)(ANYARGS), VALUE data1, VALUE (*e_proc)(ANYARGS), VALUE data2)
{
    bool b_returned = false;
    VALUE result;
    try {
        result = (*b_proc)(data1);
        b_returned = true;
        (*e_proc)(data2);

        return result;

    } catch (jruby::JavaException& ex) {
        if (!b_returned) {
            (*e_proc)(data2);
        }

        throw;
    }
}


extern "C" VALUE
rb_rescue(VALUE (*b_proc)(ANYARGS), VALUE data1, VALUE (*r_proc)(ANYARGS), VALUE data2)
{
    try {
        return (*b_proc)(data1);

    } catch (jruby::JavaException& ex) {
        JLocalEnv env;

        jthrowable t = ex.getCause(env);
        if (!env->IsInstanceOf(t, RaiseException_class)) {
            // Not a ruby exception, just propagate
            throw;
        }

        jobject rubyException = env->GetObjectField(t, RaiseException_exception_field);
        checkExceptions(env);

        VALUE exc = objectToValue(env, rubyException);
        if (rb_obj_is_kind_of(exc, rb_eStandardError)) {

            VALUE result = (*r_proc)(data2);
            env->CallStaticVoidMethod(JRuby_class, JRuby_clearErrorInfo, jruby::getRuntime());

            return result;
        }

        rb_raise(rb_eTypeError, "wrong exception type raised (expected StandardError subclass)");

        return Qnil;
    }
}

extern "C" VALUE
rb_rescue2(VALUE (*b_proc)(ANYARGS), VALUE data1, VALUE (*r_proc)(ANYARGS), VALUE data2, ...)
{
    try {
        return (*b_proc)(data1);

    } catch (jruby::JavaException& ex) {
        JLocalEnv env;

        jthrowable t = ex.getCause(env);
        if (!env->IsInstanceOf(t, RaiseException_class)) {
            // Not a ruby exception, just propagate
            throw;
        }

        jobject rubyException = env->GetObjectField(t, RaiseException_exception_field);
        checkExceptions(env);

        VALUE exc = objectToValue(env, rubyException);

        va_list ap;
        va_start(ap, data2);
        VALUE eclass = 0;

        bool handle = false;
        while ((eclass = va_arg(ap, VALUE)) != 0) {
            if (rb_obj_is_kind_of(exc, eclass)) {
                handle = true;
                break;
            }
        }
        va_end(ap);

        if (handle) {

            VALUE result = (*r_proc)(data2);
            env->CallStaticVoidMethod(JRuby_class, JRuby_clearErrorInfo, jruby::getRuntime());

            return result;
        }

        rb_raise(rb_eTypeError, "wrong exception type raised");

        return Qnil;
    }
}

extern "C" VALUE
rb_protect(VALUE (*func)(VALUE), VALUE data, int* status)
{
    bool returned = false;
    VALUE result;
    try {
        result = (*func)(data);
        returned = true;
        *status = 0;
        return result;
    } catch (jruby::JavaException& ex) {
        *status = 1;
        return Qnil;
    }
}

extern "C" void
rb_jump_tag(int status) {
    if (status) {
        // TODO: Check if there is an exception here?
        rb_exc_raise(rb_gv_get("$!"));
    }
}
