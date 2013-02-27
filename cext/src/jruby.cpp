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
#include <string.h>
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
    // Do not do something like 'assert(recv != 0x0);' -> false is 0x0
    assert(methodName != 0x0);
    assert(argCount > 0 ? args != NULL : 1);

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

VALUE
jruby::callRubyMethodB(JNIEnv* env, VALUE recv, jobject methodName, int argCount, VALUE* args, VALUE block)
{
    // Do not do something like 'assert(recv != 0x0);' -> false is 0x0
    assert(methodName != 0x0);
    assert(argCount > 0 ? args != NULL : 1);

    jsync(env);

    jobjectArray argArray;

    jvalue jparams[4];
    jparams[0].l = valueToObject(env, recv);
    jparams[1].l = methodName;
    jparams[2].l = argArray = env->NewObjectArray(argCount, IRubyObject_class, NULL);
    checkExceptions(env);
    for (int i = 0; i < argCount; ++i) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, args[i]));
        checkExceptions(env);
    }
    jparams[3].l = valueToObject(env, block);

    jlong ret = env->CallStaticLongMethodA(JRuby_class, JRuby_callMethodB, jparams);
    checkExceptions(env);

    nsync(env);
    checkExceptions(env);

    return makeStrongRef(env, (VALUE) ret);
}

#undef callMethod
VALUE
jruby::callMethod(VALUE recv, jobject methodName, int argCount, ...)
{
    VALUE args[argCount];

    va_list ap;
    va_start(ap, argCount);

    for (int i = 0; i < argCount; ++i) {
        args[i] = va_arg(ap, VALUE);
    }

    va_end(ap);

    JLocalEnv env;
    return callRubyMethodA(env, recv, methodName, argCount, args);
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

jobject
jruby::getConstMethodNameInstance(JNIEnv* env, const char* methodName)
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

jobject
jruby::getConstMethodNameInstance(const char* methodName)
{
    std::map<const char*, jobject>::iterator it = constMethodNameMap.find(methodName);
    if (likely(it != constMethodNameMap.end())) {
        return it->second;
    }

    JLocalEnv env;
    return getConstMethodNameInstance(env, methodName);
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

#undef callMethodA
VALUE
jruby::callMethodA(VALUE recv, jobject method, int argc, VALUE* argv)
{
    JLocalEnv env;

    return callRubyMethodA(env, recv, method, argc, argv);
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
