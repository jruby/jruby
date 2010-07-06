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
#include "queue.h"
#include "ruby.h"

namespace jruby {
    class Handle;

    struct DataSync {
        TAILQ_ENTRY(DataSync) syncq;
        bool (*sync)(JNIEnv* env, DataSync* data);
        void* data;
    };

    extern jclass NativeMethod_class;
    extern jclass NativeObjectAllocator_class;
    extern jclass ObjectAllocator_class;
    extern jclass Ruby_class;
    extern jclass RubyData_class;
    extern jclass RubyFloat_class;
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
    extern jclass ByteList_class;

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
    extern jmethodID RubyNumeric_int2fix_method;
    extern jmethodID RubyString_newStringNoCopy;
    extern jmethodID RubyString_view;
    extern jmethodID GC_trigger;
    extern jmethodID Handle_valueOf;
    extern jmethodID RubyObject_getNativeTypeIndex_method;
    extern jmethodID JRuby_callMethod;
    extern jmethodID JRuby_newString;
    extern jmethodID JRuby_newFloat;
    extern jmethodID JRuby_ll2inum;
    extern jmethodID JRuby_ull2inum;
    extern jmethodID JRuby_int2big;
    extern jmethodID JRuby_uint2big;
    extern jmethodID JRuby_getRString;
    extern jfieldID Handle_address_field;
    extern jfieldID RubyString_value_field;
    extern jfieldID ByteList_bytes_field, ByteList_begin_field, ByteList_length_field;
    extern jfieldID RubyFloat_value_field;
    extern jfieldID RubySymbol_id_field;

    extern jobject runtime;
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
    void checkExceptions(JNIEnv* env);

    VALUE getModule(JNIEnv* env, const char* className);
    VALUE getClass(JNIEnv* env, const char* className);
    
    jfieldID getFieldID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getStaticMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);

    TAILQ_HEAD(DataSyncQueue, DataSync);
    extern DataSyncQueue jsyncq, nsyncq;
}

#define JRUBY_callRubyMethodA(recv, meth, argc, argv) \
            jruby::callMethod(recv, meth, argc, argv)

// FIXME - no need to match ruby here, unless we fold type into flags
#define FL_MARK      (1<<5)
#define FL_LIVE      (1<<9)
#define FL_WEAK      (1<<10)
#define FL_CONST     (1<<11)

#define FL_USHIFT    12

#define FL_USER0     (((VALUE)1)<<(FL_USHIFT+0))
#define FL_USER1     (((VALUE)1)<<(FL_USHIFT+1))
#define FL_USER2     (((VALUE)1)<<(FL_USHIFT+2))
#define FL_USER3     (((VALUE)1)<<(FL_USHIFT+3))
#define FL_USER4     (((VALUE)1)<<(FL_USHIFT+4))
#define FL_USER5     (((VALUE)1)<<(FL_USHIFT+5))
#define FL_USER6     (((VALUE)1)<<(FL_USHIFT+6))
#define FL_USER7     (((VALUE)1)<<(FL_USHIFT+7))
#define FL_USER8     (((VALUE)1)<<(FL_USHIFT+8))
#define FL_USER9     (((VALUE)1)<<(FL_USHIFT+9))
#define FL_USER10    (((VALUE)1)<<(FL_USHIFT+10))
#define FL_USER11    (((VALUE)1)<<(FL_USHIFT+11))
#define FL_USER12    (((VALUE)1)<<(FL_USHIFT+12))
#define FL_USER13    (((VALUE)1)<<(FL_USHIFT+13))
#define FL_USER14    (((VALUE)1)<<(FL_USHIFT+14))
#define FL_USER15    (((VALUE)1)<<(FL_USHIFT+15))
#define FL_USER16    (((VALUE)1)<<(FL_USHIFT+16))
#define FL_USER17    (((VALUE)1)<<(FL_USHIFT+17))
#define FL_USER18    (((VALUE)1)<<(FL_USHIFT+18))
#define FL_USER19    (((VALUE)1)<<(FL_USHIFT+19))

#endif	/* JRUBY_H */

