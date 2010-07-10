/*
 * Copyright (C) 2008-2010 Wayne Meissner
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
#include <ctype.h>
#include <vector>
#include <jni.h>
#include "JLocalEnv.h"
#include "jruby.h"
#include "ruby.h"
#include "Handle.h"
#include "JUtil.h"
#include "JavaException.h"

using namespace jruby;


struct StringCompare: public std::binary_function<const char*, const char*, bool> {
    inline bool operator()(const char* k1, const char* k2) const {
        return strcmp(k1, k2) < 0;
    }
};

static std::map<const char*, jobject> constMethodNameMap;
static std::map<const char*, jobject, StringCompare> nonConstMethodNameMap;

static VALUE
callRubyMethod(JNIEnv* env, VALUE recv, jobject methodName, int argCount, VALUE* args)
{

    jsync(env);

    jobjectArray argArray;

    argArray = env->NewObjectArray(argCount, IRubyObject_class, NULL);
    checkExceptions(env);
    for (int i = 0; i < argCount; ++i) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, args[i]));
        checkExceptions(env);
    }
    
    jvalue jparams[3];
    jparams[0].l = valueToObject(env, recv);
    jparams[1].l = methodName;
    jparams[2].l = argArray;

    jlong ret = env->CallStaticLongMethodA(JRuby_class, JRuby_callMethod, jparams);
    checkExceptions(env);

    nsync(env);
    checkExceptions(env);

    return makeStrongRef(env, (VALUE) ret);
}

static inline jobject
getNonConstMethodNameInstance(JNIEnv* env, const char* methodName)
{
    std::map<const char*, jobject>::iterator it = nonConstMethodNameMap.find(methodName);
    if (likely(it != nonConstMethodNameMap.end())) {
        return it->second;
    }

    jobject obj = env->NewGlobalRef(env->NewStringUTF(methodName));

    nonConstMethodNameMap.insert(std::map<const char*, jobject>::value_type(strdup(methodName), obj));

    return obj;
}


static inline jobject
getConstMethodNameInstance(JNIEnv* env, const char* methodName)
{
    std::map<const char*, jobject>::iterator it = constMethodNameMap.find(methodName);
    if (likely(it != constMethodNameMap.end())) {
        return it->second;
    }

    jobject obj = getNonConstMethodNameInstance(env, methodName);

    constMethodNameMap.insert(std::map<const char*, jobject>::value_type(methodName, obj));

    return obj;

    return constMethodNameMap[methodName] = getNonConstMethodNameInstance(env, methodName);
}

VALUE
jruby::callMethodA(VALUE recv, const char* method, int argCount, VALUE* args)
{
    JLocalEnv env;

    return callRubyMethod(env, recv, getNonConstMethodNameInstance(env, method), argCount, args);
}

VALUE
jruby::callMethodAConst(VALUE recv, const char* method, int argCount, VALUE* args)
{
    JLocalEnv env;
    
    return callRubyMethod(env, recv, getConstMethodNameInstance(env, method), argCount, args);
}


VALUE
jruby::callMethodV(VALUE recv, const char* method, int argCount, ...)
{
    VALUE args[argCount];

    va_list ap;
    va_start(ap, argCount);

    for (int i = 0; i < argCount; ++i) {
        args[i] = va_arg(ap, VALUE);
    }

    va_end(ap);

    JLocalEnv env;
    return callRubyMethod(env, recv, getNonConstMethodNameInstance(env, method), argCount, args);
}

VALUE
jruby::callMethodVConst(VALUE recv, const char* method, int argCount, ...)
{
    VALUE args[argCount];

    va_list ap;
    va_start(ap, argCount);

    for (int i = 0; i < argCount; ++i) {
        args[i] = va_arg(ap, VALUE);
    }

    va_end(ap);

    
    JLocalEnv env;
    return callRubyMethod(env, recv, getConstMethodNameInstance(env, method), argCount, args);
}


static jobject
callObjectMethod(JNIEnv* env, jobject recv, jmethodID mid)
{
    jobject result = env->CallObjectMethod(recv, mid);
    jruby::checkExceptions(env);
    return result;
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
    JLocalEnv env;
    return getClass(env, className);
}

jobject
jruby::getFalse()
{
    return constHandles[0]->obj;
}

jobject
jruby::getTrue()
{
    return constHandles[1]->obj;
}

jobject
jruby::getNil()
{
    return constHandles[2]->obj;
}