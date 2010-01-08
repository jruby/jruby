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
    extern jclass Handle_class;
    extern jclass ThreadContext_class;
    extern jclass Symbol_class;
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
    extern jmethodID Handle_valueOf;
    extern jmethodID RubyObject_getNativeTypeIndex_method;
    extern jfieldID Handle_address_field;
    extern jobject runtime, nilRef, trueRef, falseRef;

    void initRubyClasses(JNIEnv* env, jobject runtime);

    Handle* newHandle(JNIEnv* env);
    VALUE callMethod(VALUE recv, const char* methodName, int argCount, ...);
    VALUE callMethodA(VALUE recv, const char* methodName, int argCount, VALUE* args);
    VALUE getClass(const char* className);
    VALUE getModule(const char* className);
    VALUE getSymbol(const char* name);

    int typeOf(JNIEnv* env, jobject obj);

    jobject valueToObject(JNIEnv* env, VALUE v);
    VALUE objectToValue(JNIEnv* env, jobject obj);

    inline jobject getRuntime() { return jruby::runtime; }
    void checkExceptions(JNIEnv* env);

    VALUE getModule(JNIEnv* env, const char* className);
    VALUE getClass(JNIEnv* env, const char* className);
    
    jobject getNilRef(JNIEnv* env);
    jobject getFalseRef(JNIEnv* env);
    jobject getTrueRef(JNIEnv* env);

    jfieldID getFieldID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getStaticMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
}

#define JRUBY_callRubyMethodA(recv, meth, argc, argv) \
            jruby::callMethod(recv, meth, argc, argv)



#endif	/* JRUBY_H */

