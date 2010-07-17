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
rb_exc_raise(VALUE exc) {
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

#ifdef notyet /* does not quite work */

extern "C" VALUE
rb_rescue(VALUE (*b_proc)(ANYARGS), VALUE data1, VALUE (*r_proc)(ANYARGS), VALUE data2)
{
    try {
        return (*b_proc)(data1);

    } catch (jruby::JavaException& ex) {

        return (*r_proc)(data2);
    }
}

#endif