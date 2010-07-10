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

VALUE
jruby::callRubyMethodA(JNIEnv* env, VALUE recv, jobject methodName, int argCount, VALUE* args)
{

    jsync(env);

    jobjectArray argArray;

    jvalue jparams[5];
    jparams[0].l = valueToObject(env, recv);
    jparams[1].l = methodName;
    jmethodID mid = JRuby_callMethod;
    switch (argCount) {
        case 0:
            mid = JRuby_callMethod0;
            break;

        case 1:
            mid = JRuby_callMethod1;
            jparams[2].l = valueToObject(env, args[0]);
            break;

        case 2:
            mid = JRuby_callMethod2;
            jparams[2].l = valueToObject(env, args[0]);
            jparams[3].l = valueToObject(env, args[1]);
            break;

        case 3:
            mid = JRuby_callMethod3;
            jparams[2].l = valueToObject(env, args[0]);
            jparams[3].l = valueToObject(env, args[1]);
            jparams[4].l = valueToObject(env, args[2]);
            break;

        default:
            mid = JRuby_callMethod;
            jparams[2].l = argArray = env->NewObjectArray(argCount, IRubyObject_class, NULL);
            checkExceptions(env);
            for (int i = 0; i < argCount; ++i) {
                env->SetObjectArrayElement(argArray, i, valueToObject(env, args[i]));
                checkExceptions(env);
            }

            break;
    }

    jlong ret = env->CallStaticLongMethodA(JRuby_class, mid, jparams);
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
jruby::callMethodANonConst(VALUE recv, const char* method, int argCount, VALUE* args)
{
    JLocalEnv env;

    return callRubyMethodA(env, recv, getNonConstMethodNameInstance(env, method), argCount, args);
}

VALUE
jruby::callMethodAConst(VALUE recv, const char* method, int argCount, VALUE* args)
{
    JLocalEnv env;
    
    return callRubyMethodA(env, recv, getConstMethodNameInstance(env, method), argCount, args);
}


VALUE
jruby::callMethodNonConst(VALUE recv, const char* method, int argCount, ...)
{
    VALUE args[argCount];

    va_list ap;
    va_start(ap, argCount);

    for (int i = 0; i < argCount; ++i) {
        args[i] = va_arg(ap, VALUE);
    }

    va_end(ap);

    JLocalEnv env;
    return callRubyMethodA(env, recv, getNonConstMethodNameInstance(env, method), argCount, args);
}

VALUE
jruby::callMethodConst(VALUE recv, const char* method, int argCount, ...)
{
    VALUE args[argCount];

    va_list ap;
    va_start(ap, argCount);

    for (int i = 0; i < argCount; ++i) {
        args[i] = va_arg(ap, VALUE);
    }

    va_end(ap);

    
    JLocalEnv env;
    return callRubyMethodA(env, recv, getConstMethodNameInstance(env, method), argCount, args);
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