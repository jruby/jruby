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

#ifndef JRUBY_H
#define	JRUBY_H

#include <jni.h>
#include <map>
#include "Handle.h"
#include "ruby.h"

namespace jruby {
    extern jclass NativeMethod_class;
    extern jclass NativeObjectAllocator_class;
    extern jclass ObjectAllocator_class;
    extern jclass Ruby_class;
    extern jclass RubyData_class;
    extern jclass RubyObject_class;
    extern jclass RubyBasicObject_class;
    extern jclass RubyClass_class;
    extern jclass RaiseException_class;
    extern jclass RubyModule_class;
    extern jclass RubyNumeric_class;
    extern jclass RubyString_class;
    extern jclass IRubyObject_class;
    extern jclass GC_class;
    extern jclass Handle_class;
    extern jclass ThreadContext_class;
    extern jclass Symbol_class;
    extern jclass JRuby_class;
    extern jclass GlobalVariable_class;

    extern jmethodID IRubyObject_callMethod;
    extern jmethodID IRubyObject_asJavaString_method;
    extern jmethodID ThreadContext_getRuntime_method;
    extern jmethodID Ruby_defineModule_method;
    extern jmethodID Ruby_getNil_method;
    extern jmethodID Ruby_getTrue_method;
    extern jmethodID Ruby_getFalse_method;
    extern jmethodID Ruby_getCurrentContext_method;
    extern jmethodID Ruby_getClass_method;
    extern jmethodID Ruby_getModule_method;
    extern jmethodID Ruby_newSymbol_method;
    extern jmethodID Ruby_newFixnum_method;
    extern jmethodID RubyData_newRubyData_method;
    extern jmethodID RaiseException_constructor;
    extern jmethodID RubyNumeric_num2long_method;
    extern jmethodID RubyString_newStringNoCopy;
    extern jmethodID GC_trigger;
    extern jmethodID Handle_valueOf;
    extern jmethodID RubyObject_getNativeTypeIndex_method;
    extern jmethodID JRuby_callMethod;
    extern jmethodID JRuby_newString;
    extern jmethodID JRuby_ll2inum;
    extern jmethodID JRuby_ull2inum;
    extern jmethodID JRuby_int2big;
    extern jmethodID JRuby_uint2big;
    extern jfieldID Handle_address_field;
    extern jobject runtime;
    extern jobject nilRef;
    extern jobject trueRef;
    extern jobject falseRef;
    extern std::map<const char*, jobject> methodNameMap;

    void initRubyClasses(JNIEnv* env, jobject runtime);

    Handle* newHandle(JNIEnv* env);

#define callMethod(recv, method, argCount, a...) \
    (__builtin_constant_p(method) \
        ? jruby::callMethodVConst(recv, method, argCount, ##a) \
        : jruby::callMethodV(recv, method, argCount, ##a))

    VALUE callMethodV(VALUE recv, const char* methodName, int argCount, ...);
    VALUE callMethodA(VALUE recv, const char* methodName, int argCount, VALUE* args);
    VALUE callMethodVConst(VALUE recv, const char* methodName, int argCount, ...);
    VALUE callMethodAConst(VALUE recv, const char* methodName, int argCount, VALUE* args);
    VALUE getClass(const char* className);
    VALUE getModule(const char* className);
    VALUE getSymbol(const char* name);

    int typeOf(JNIEnv* env, jobject obj);

    jobject valueToObject(JNIEnv* env, VALUE v);
    VALUE objectToValue(JNIEnv* env, jobject obj);

    inline jobject getRuntime() { return jruby::runtime; }
    inline jobject getTrue() { return jruby::constHandles[0]->obj; }
    inline jobject getFalse() { return jruby::constHandles[1]->obj; }
    inline jobject getNil() { return jruby::constHandles[2]->obj; }
    void checkExceptions(JNIEnv* env);

    VALUE getModule(JNIEnv* env, const char* className);
    VALUE getClass(JNIEnv* env, const char* className);
    
    jfieldID getFieldID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getStaticMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
}

#define JRUBY_callRubyMethodA(recv, meth, argc, argv) \
            jruby::callMethod(recv, meth, argc, argv)



#endif	/* JRUBY_H */

