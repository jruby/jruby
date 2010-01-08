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
#include <ctype.h>
#include <vector>
#include <jni.h>
#include "JLocalEnv.h"
#include "jruby.h"
#include "ruby.h"
#include "JUtil.h"
#include "JavaException.h"


VALUE
jruby::callMethodA(VALUE recv, const char* method, int argCount, VALUE* args)
{
    JLocalEnv env;
    jobjectArray argArray;
 

    argArray = env->NewObjectArray(argCount, IRubyObject_class, NULL);
    checkExceptions(env);
    for (int i = 0; i < argCount; ++i) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, args[i]));
        checkExceptions(env);
    }
    
    jvalue jparams[3];
    jparams[0].l = env->CallObjectMethod(getRuntime(), Ruby_getCurrentContext_method);
    jparams[1].l = env->NewStringUTF(method);
    jparams[2].l = argArray;

    jobject ret = env->CallObjectMethodA(valueToObject(env, recv), IRubyObject_callMethod, jparams);
    checkExceptions(env);

    return objectToValue(env, ret);
}

VALUE
jruby::callMethod(VALUE recv, const char* method, int argCount, ...)
{
    VALUE args[argCount];

    va_list ap;
    va_start(ap, argCount);

    for (int i = 0; i < argCount; ++i) {
        args[i] = va_arg(ap, VALUE);
    }

    va_end(ap);

    return callMethodA(recv, method, argCount, args);
}

static jobject callObjectMethod(JNIEnv* env, jobject recv, jmethodID mid)
{
    jobject result = env->CallObjectMethod(recv, mid);
    jruby::checkExceptions(env);
    return result;
}

jobject
jruby::getNilRef(JNIEnv* env)
{
    return jruby::nilRef;
}

jobject
jruby::getFalseRef(JNIEnv* env)
{
    return jruby::falseRef;
}

jobject
jruby::getTrueRef(JNIEnv* env)
{
    return jruby::trueRef;
}

VALUE
jruby::getSymbol(const char* name)
{
    JLocalEnv env;
    jobject result = env->CallObjectMethod(getRuntime(), Ruby_newSymbol_method, env->NewStringUTF(name));
    checkExceptions(env);
    
    return objectToValue(env, result);
}


VALUE
jruby::getModule(JNIEnv* env, const char* className)
{
    jobject klass = env->CallObjectMethod(getRuntime(), Ruby_getModule_method, env->NewStringUTF(className));
    checkExceptions(env);
    if (klass == NULL) {
        throw JavaException(env, jruby::RuntimeException, "failed to find lookup module %s", className);
    }

    return objectToValue(env, klass);
}

VALUE
jruby::getClass(JNIEnv* env, const char* className)
{
    jobject klass = env->CallObjectMethod(getRuntime(), Ruby_getClass_method, env->NewStringUTF(className));
    checkExceptions(env);
    if (klass == NULL) {
        throw JavaException(env, RuntimeException, "failed to find lookup class %s", className);
    }
    return objectToValue(env, klass);
}

VALUE
jruby::getClass(const char* className)
{
    return getClass(JLocalEnv(), className);
}