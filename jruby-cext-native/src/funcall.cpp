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

#include <jni.h>
#include "JLocalEnv.h"
#include "jruby.h"
#include "ruby.h"

using namespace jruby;

static VALUE jruby_funcall(JNIEnv* env, VALUE recv, ID meth, jobjectArray argArray);

extern "C" VALUE
rb_funcall(VALUE recv, ID meth, int argCount, ...)
{
    JLocalEnv env;
    jobjectArray argArray;

    argArray = env->NewObjectArray(argCount, IRubyObject_class, NULL);
    checkExceptions(env);

    va_list ap;
    va_start(ap, argCount);
    for (int i = 0; i < argCount; i++) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, va_arg(ap, VALUE)));
        checkExceptions(env);
    }

    va_end(ap);

    return jruby_funcall(env, recv, meth, argArray);
}

extern "C" VALUE
rb_funcall2(VALUE recv, ID meth, int argCount, VALUE* args)
{
    JLocalEnv env;
    jobjectArray argArray;
    int i;

    argArray = env->NewObjectArray(argCount, IRubyObject_class, NULL);
    checkExceptions(env);
    for (i = 0; i < argCount; i++) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, args[i]));
        checkExceptions(env);
    }

    return jruby_funcall(env, recv, meth, argArray);
}


static VALUE
jruby_funcall(JNIEnv* env, VALUE recv, ID meth, jobjectArray argArray)
{
    jvalue jparams[4];
    jobject obj = valueToObject(env, recv);

    jparams[0].l = env->CallObjectMethod(getRuntime(), Ruby_getCurrentContext_method);
    checkExceptions(env);
    jparams[1].l = env->CallObjectMethod(obj, IRubyObject_asJavaString_method);
    checkExceptions(env);
    jparams[2].l = argArray;


    jobject ret = env->CallObjectMethodA(obj, IRubyObject_callMethod, jparams);
    checkExceptions(env);

    return objectToValue(env, ret);
}