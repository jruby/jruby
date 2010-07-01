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
#include <pthread.h>
#include <jni.h>

#include "ruby.h"
#include "jruby.h"
#include "JUtil.h"
#include "JavaException.h"
#include "org_jruby_cext_Native.h"

using namespace jruby;

static VALUE dispatch(void* func, int arity, int argCount, VALUE recv, VALUE* v);

/*
 * Class:     org_jruby_cext_Native
 * Method:    callInit
 * Signature: (Lorg/jruby/runtime/ThreadContext;J)J
 */
extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_callInit(JNIEnv* env, jobject self, jobject jThreadContext, jlong address)
{
    jobject runtime = env->CallObjectMethod(jThreadContext, jruby::ThreadContext_getRuntime_method);
    if (!env->IsSameObject(runtime, jruby::runtime)) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "invalid ruby runtime");
        return 0;
    }

    try {

        ((void (*)(void)) address)();

    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());

    }

    return 0;
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    callMethod
 * Signature: (Lorg/jruby/runtime/ThreadContext;JLorg/jruby/runtime/builtin/IRubyObject;I[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod(JNIEnv* env, jobject nativeClass, jobject jThreadContext,
        jlong address, jlong recv, jint arity, jlongArray argArray)
{
    
    try {
        int argCount = env->GetArrayLength(argArray);
        jlong* largs = (jlong *) alloca(argCount * sizeof(jlong));
        env->GetLongArrayRegion(argArray, 0, argCount, largs);

        VALUE* values = (VALUE *) alloca(argCount * sizeof(VALUE));
        for (int i = 0; i < argCount; ++i) {
            values[i] = (VALUE) largs[i];
        }

        VALUE v = dispatch((void *) address, arity, argCount, (VALUE) recv, values);
        return valueToObject(env, v);    
        
    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());
        return NULL;

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());
        return NULL;
    }
    
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    callMethod0
 * Signature: (JJ)Lorg/jruby/runtime/builtin/IRubyObject;
 */
JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod0(JNIEnv* env, jobject self, jlong fn, jlong recv)
{
    try {

        return valueToObject(env, ((VALUE (*)(VALUE)) fn)((VALUE) recv));

    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());
        return NULL;

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());
        return NULL;
    }
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    callMethod1
 * Signature: (JJJ)Lorg/jruby/runtime/builtin/IRubyObject;
 */
JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod1(JNIEnv* env, jobject self, jlong fn, jlong recv, jlong arg1)
{
    try {
        return valueToObject(env, ((VALUE (*)(VALUE, VALUE)) fn)((VALUE) recv, (VALUE) arg1));

    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());
        return NULL;

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());
        return NULL;
    }
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    callMethod2
 * Signature: (JJJJ)Lorg/jruby/runtime/builtin/IRubyObject;
 */
JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod2(JNIEnv* env, jobject self, jlong fn, jlong recv,
        jlong arg1, jlong arg2)
{
    try {
        return valueToObject(env, ((VALUE (*)(VALUE, VALUE, VALUE)) fn)((VALUE) recv, (VALUE) arg1, (VALUE) arg2));

    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());
        return NULL;

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());
        return NULL;
    }
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    callMethod3
 * Signature: (JJJJJ)Lorg/jruby/runtime/builtin/IRubyObject;
 */
JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod3(JNIEnv* env, jobject self, jlong fn, jlong recv,
        jlong arg1, jlong arg2, jlong arg3)
{
    try {
        return valueToObject(env, ((VALUE (*)(VALUE, VALUE, VALUE, VALUE)) fn)((VALUE) recv, (VALUE) arg1, (VALUE) arg2, (VALUE) arg3));

    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());
        return NULL;

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());
        return NULL;
    }
}


static VALUE
dispatch(void* func, int arity, int argCount, VALUE recv, VALUE* v)
{
//    printf("calling %p with arity=%d\n", func, arity);
    if (arity < 0) {
        return ((VALUE (*)(int, VALUE*, VALUE)) func)(argCount, v, recv);
    }
    switch (argCount) {
    case 0:
        return ((VALUE (*)(VALUE))func)(recv);
    case 1:
        return ((VALUE (*)(VALUE, VALUE))func)(recv, v[0]);
    case 2:
        return ((VALUE (*)(VALUE, VALUE, VALUE))func)(recv, v[0], v[1]);
    case 3:
        return ((VALUE (*)(VALUE, VALUE, VALUE, VALUE))func)(recv, v[0], v[1], v[2]);
    case 4:
        return ((VALUE (*)(VALUE, VALUE, VALUE, VALUE, VALUE))func)(recv, v[0], v[1], v[2], v[3]);
    case 5:
        return ((VALUE (*)(VALUE, VALUE, VALUE, VALUE, VALUE, VALUE))func)(recv, v[0], v[1], v[2], v[3], v[4]);
    case 6:
        return ((VALUE (*)(VALUE, VALUE, VALUE, VALUE, VALUE, VALUE, VALUE))func)(recv, v[0], v[1], v[2], v[3], v[4], v[5]);
    case 7:
        return ((VALUE (*)(VALUE, VALUE, VALUE, VALUE, VALUE, VALUE, VALUE, VALUE))func)(recv, v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
    case 8:
        return ((VALUE (*)(VALUE, VALUE, VALUE, VALUE, VALUE, VALUE, VALUE, VALUE, VALUE))func)(recv, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]);
    default:
        rb_raise(rb_eArgError, "Too many arguments (%d for max %d)", argCount, 8);
    }
}

