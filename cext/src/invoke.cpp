 /***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2008-2010 Wayne Meissner
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

#include <stdio.h>
#include <stdlib.h>
#ifdef __sun
  #include <alloca.h>
#endif
#include <jni.h>

#include "ruby.h"
#include "jruby.h"
#include "Handle.h"
#include "JUtil.h"
#include "JavaException.h"
#include "org_jruby_cext_Native.h"

using namespace jruby;

static VALUE dispatch(void* func, int arity, int argCount, VALUE recv, VALUE* v);
static int invokeLevel = 0;

static void clearSyncQueue(DataSyncQueue* q);

class InvocationSession {
private:
    JNIEnv* env;
public:
    InvocationSession(JNIEnv* env_) {
        ++invokeLevel;
        this->env = env_;

        nsync(env);
    }

    ~InvocationSession() {
        --invokeLevel;

        if (unlikely(!TAILQ_EMPTY(&jsyncq))) {
            runSyncQueue(env, &jsyncq);
            if (invokeLevel < 1) {
                clearSyncQueue(&jsyncq);
            }
        }

        if (likely(invokeLevel < 1)) {
            if (unlikely(!TAILQ_EMPTY(&cleanq))) {
                runSyncQueue(env, &cleanq);
                clearSyncQueue(&cleanq);
            }

            if (unlikely(!TAILQ_EMPTY(&nsyncq))) {
                clearSyncQueue(&nsyncq);
            }
        }
    }
};


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
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C extension initialized against invalid ruby runtime");
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
            values[i] = makeStrongRef(env, (VALUE) largs[i]);
        }

        InvocationSession session(env);
        makeStrongRef(env, (VALUE) recv);
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
extern "C" JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod0(JNIEnv* env, jobject self, jlong fn, jlong recv)
{
    try {

        InvocationSession session(env);
        makeStrongRef(env, (VALUE) recv);
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
extern "C" JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod1(JNIEnv* env, jobject self, jlong fn, jlong recv, jlong arg1)
{
    try {
        InvocationSession session(env);
        makeStrongRef(env, (VALUE) recv);
        makeStrongRef(env, (VALUE) arg1);
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
extern "C" JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod2(JNIEnv* env, jobject self, jlong fn, jlong recv,
        jlong arg1, jlong arg2)
{
    try {
        InvocationSession session(env);
        makeStrongRef(env, (VALUE) recv);
        makeStrongRef(env, (VALUE) arg1);
        makeStrongRef(env, (VALUE) arg2);
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
extern "C" JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callMethod3(JNIEnv* env, jobject self, jlong fn, jlong recv,
        jlong arg1, jlong arg2, jlong arg3)
{
    try {
        InvocationSession session(env);
        makeStrongRef(env, (VALUE) recv);
        makeStrongRef(env, (VALUE) arg1);
        makeStrongRef(env, (VALUE) arg2);
        makeStrongRef(env, (VALUE) arg3);

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

static void
clearSyncQueue(DataSyncQueue* q)
{
    DataSync* d;
    while ((d = TAILQ_FIRST(q))) {
        TAILQ_REMOVE(q, d, syncq);
    }
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    callFunction
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_callFunction(JNIEnv* env, jobject, jlong function, jlong data)
{
    try {
        if (function == (jlong)0) return (jlong)0; // Just in case somebody passes no unblock function
        return ((VALUE (*)(void*)) function)((void*) data);

    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());
        return 0x0;

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());
        return 0x0;
    }
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    callProcMethod
 * Signature: (JJ)Lorg/jruby/runtime/builtin/IRubyObject;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_callProcMethod(JNIEnv* env, jobject, jlong fn, jlong args_ary)
{
    try {

        InvocationSession session(env);
        makeStrongRef(env, (VALUE) args_ary);
        return valueToObject(env, ((VALUE (*)(VALUE)) fn)((VALUE) args_ary));

    } catch (jruby::JavaException& ex) {
        env->Throw(ex.getCause());
        return NULL;

    } catch (std::exception& ex) {
        jruby::throwExceptionByName(env, jruby::RuntimeException, "C runtime exception occurred: ", ex.what());
        return NULL;
    }
}
