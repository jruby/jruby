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

#include <jni.h>
#include <map>
#include "JUtil.h"
#include "jruby.h"
#include "Handle.h"
#include "JavaException.h"

namespace jruby {
    jclass IRubyObject_class;
    jclass ObjectAllocator_class;
    jclass RaiseException_class;
    jclass Ruby_class;
    jclass RubyObject_class;
    jclass RubyBasicObject_class;
    jclass RubyClass_class;
    jclass RubyData_class;    
    jclass RubyModule_class;
    jclass RubyNumeric_class;
    jclass RubyFloat_class;
    jclass RubyString_class;
    jclass Handle_class;
    jclass GC_class;
    jclass NativeMethod_class;
    jclass NativeObjectAllocator_class;
    jclass ThreadContext_class;
    jclass Symbol_class;
    jclass JRuby_class;
    jclass ByteList_class;
    jclass GlobalVariable_class;
    jmethodID JRuby_callMethod;
    jmethodID JRuby_callMethod0;
    jmethodID JRuby_callMethod1;
    jmethodID JRuby_callMethod2;
    jmethodID JRuby_callMethod3;
    jmethodID JRuby_newString;
    jmethodID JRuby_ll2inum;
    jmethodID JRuby_ull2inum;
    jmethodID JRuby_int2big;
    jmethodID JRuby_uint2big;
    jmethodID JRuby_getRString;
    jmethodID JRuby_newFloat;

    jmethodID ThreadContext_getRuntime_method;
    jmethodID Ruby_defineModule_method;
    jmethodID Ruby_getNil_method;
    jmethodID Ruby_getTrue_method;
    jmethodID Ruby_getFalse_method;
    jmethodID Ruby_getClass_method;
    jmethodID Ruby_getModule_method;
    jmethodID Ruby_newSymbol_method;
    jmethodID Ruby_newFixnum_method;
    jmethodID RaiseException_constructor;
    jmethodID RubyData_newRubyData_method;
    jmethodID RubyObject_getNativeTypeIndex_method;
    jmethodID RubyNumeric_num2long_method;
    jmethodID RubyNumeric_num2chr_method;
    jmethodID RubyNumeric_num2dbl_method;    
    jmethodID RubyNumeric_int2fix_method;    
    jmethodID RubyString_newStringNoCopy;
    jmethodID RubyString_view;
    jmethodID RubySymbol_getSymbolLong;
    jmethodID IRubyObject_callMethod;
    jmethodID IRubyObject_asJavaString_method;
    jmethodID IRubyObject_respondsTo_method;
    jmethodID Handle_valueOf;
    jmethodID Ruby_getCurrentContext_method;
    jmethodID GC_trigger;
    jfieldID Handle_address_field;
    jfieldID RubyString_value_field;
    jfieldID RubyFloat_value_field;
    jfieldID ByteList_bytes_field, ByteList_begin_field, ByteList_length_field;
    jfieldID RubySymbol_id_field;
    jfieldID RubySymbol_symbol_field;

    jobject runtime;
    jobject nilRef;
    jobject trueRef;
    jobject falseRef;
};

using namespace jruby;

void
jruby::checkExceptions(JNIEnv* env)
{
    jthrowable ex = env->ExceptionOccurred();
    if (ex) {
        throw JavaException(env, ex);
    }
}

static jclass
loadClass(JNIEnv* env, const char *name)
{
    jclass tmp = env->FindClass(name);
    checkExceptions(env);

    jclass retVal = (jclass)env->NewGlobalRef(tmp);

    checkExceptions(env);
    
    return retVal;
}

jmethodID
jruby::getMethodID(JNIEnv* env, jclass klass, const char* fieldName, const char* signature)
{
    jmethodID mid = env->GetMethodID(klass, fieldName, signature);
    
    checkExceptions(env);

    return mid;
}

jmethodID
jruby::getStaticMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature)
{
    jmethodID mid = env->GetStaticMethodID(klass, methodName, signature);

    checkExceptions(env);

    return mid;
}

jfieldID
jruby::getFieldID(JNIEnv* env, jclass klass, const char* fieldName, const char* signature)
{
    jfieldID fid = env->GetFieldID(klass, fieldName, signature);

    checkExceptions(env);

    return fid;
}

static void
loadIds(JNIEnv* env)
{
    Ruby_class = loadClass(env, "org/jruby/Ruby");
    RaiseException_class = loadClass(env, "org/jruby/exceptions/RaiseException");
    RubyObject_class = loadClass(env, "org/jruby/RubyObject");
    RubyBasicObject_class = loadClass(env, "org/jruby/RubyBasicObject");
    RubyClass_class = loadClass(env, "org/jruby/RubyClass");
    RubyData_class = loadClass(env, "org/jruby/cext/RubyData");
    RubyFloat_class = loadClass(env, "org/jruby/RubyFloat");
    RubyModule_class = loadClass(env, "org/jruby/RubyModule");
    RubyNumeric_class = loadClass(env, "org/jruby/RubyNumeric");
    RubyFloat_class = loadClass(env, "org/jruby/RubyFloat");
    RubyString_class = loadClass(env, "org/jruby/RubyString");
    IRubyObject_class = loadClass(env, "org/jruby/runtime/builtin/IRubyObject");
    Handle_class = loadClass(env, "org/jruby/cext/Handle");
    GC_class = loadClass(env, "org/jruby/cext/GC");
    NativeMethod_class = loadClass(env, "org/jruby/cext/NativeMethod");
    NativeObjectAllocator_class = loadClass(env, "org/jruby/cext/NativeObjectAllocator");
    ObjectAllocator_class = loadClass(env, "org/jruby/runtime/ObjectAllocator");
    ThreadContext_class = loadClass(env, "org/jruby/runtime/ThreadContext");
    Symbol_class = loadClass(env, "org/jruby/RubySymbol");
    JRuby_class = loadClass(env, "org/jruby/cext/JRuby");
    ByteList_class = loadClass(env, "org/jruby/util/ByteList");
    GlobalVariable_class = loadClass(env, "org/jruby/runtime/GlobalVariable");

    Handle_address_field = getFieldID(env, Handle_class, "address", "J");
    Ruby_defineModule_method = getMethodID(env, Ruby_class, "defineModule", "(Ljava/lang/String;)Lorg/jruby/RubyModule;");
    Ruby_getCurrentContext_method = getMethodID(env, Ruby_class, "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
    Ruby_getFalse_method = getMethodID(env, Ruby_class, "getFalse", "()Lorg/jruby/RubyBoolean;");
    Ruby_getTrue_method = getMethodID(env, Ruby_class, "getTrue", "()Lorg/jruby/RubyBoolean;");
    Ruby_getNil_method = getMethodID(env, Ruby_class, "getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
    Ruby_getModule_method = getMethodID(env, Ruby_class, "getModule", "(Ljava/lang/String;)Lorg/jruby/RubyModule;");
    Ruby_getClass_method = getMethodID(env, Ruby_class, "getClass", "(Ljava/lang/String;)Lorg/jruby/RubyClass;");
    Ruby_newSymbol_method = getMethodID(env, Ruby_class, "newSymbol", "(Ljava/lang/String;)Lorg/jruby/RubySymbol;");
    Ruby_newFixnum_method = getMethodID(env, Ruby_class, "newFixnum", "(J)Lorg/jruby/RubyFixnum;");
    RaiseException_constructor = getMethodID(env, RaiseException_class, "<init>",
            "(Lorg/jruby/Ruby;Lorg/jruby/RubyClass;Ljava/lang/String;Z)V");
    RubyObject_getNativeTypeIndex_method = getMethodID(env, RubyObject_class, "getNativeTypeIndex", "()I");
    
    RubyNumeric_num2long_method = getStaticMethodID(env, RubyNumeric_class, "num2long",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)J");
    RubyNumeric_num2chr_method = getStaticMethodID(env, RubyNumeric_class, "num2chr",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)B");
    RubyNumeric_num2dbl_method = getStaticMethodID(env, RubyNumeric_class, "num2dbl",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)D");
    RubyNumeric_int2fix_method = getStaticMethodID(env, RubyNumeric_class, "int2fix",
            "(Lorg/jruby/Ruby;J)Lorg/jruby/RubyNumeric;");

    GC_trigger = getStaticMethodID(env, GC_class, "trigger", "()V");
    Handle_valueOf = getStaticMethodID(env, Handle_class, "valueOf", "(Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/cext/Handle;");

    IRubyObject_callMethod = getMethodID(env, IRubyObject_class, "callMethod",
            "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
    IRubyObject_asJavaString_method = getMethodID(env, IRubyObject_class, "asJavaString", "()Ljava/lang/String;");
    IRubyObject_respondsTo_method = getMethodID(env, IRubyObject_class, "respondsTo", "(Ljava/lang/String;)Z");

    ThreadContext_getRuntime_method = getMethodID(env, ThreadContext_class, "getRuntime", "()Lorg/jruby/Ruby;");
    RubyData_newRubyData_method = getStaticMethodID(env, RubyData_class, "newRubyData", "(Lorg/jruby/Ruby;Lorg/jruby/RubyClass;J)Lorg/jruby/cext/RubyData;");
    RubyString_newStringNoCopy = getStaticMethodID(env, RubyString_class,
            "newStringNoCopy", "(Lorg/jruby/Ruby;[B)Lorg/jruby/RubyString;");
    RubyString_view = getMethodID(env, RubyString_class,
            "view", "([B)V");
    RubySymbol_getSymbolLong = getStaticMethodID(env, Symbol_class, "getSymbolLong",
            "(Lorg/jruby/Ruby;J)Lorg/jruby/RubySymbol;");
    JRuby_callMethod = getStaticMethodID(env, JRuby_class, "callRubyMethod",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;[Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callMethod0 = getStaticMethodID(env, JRuby_class, "callRubyMethod0",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;)J");
    JRuby_callMethod1 = getStaticMethodID(env, JRuby_class, "callRubyMethod1",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callMethod2 = getStaticMethodID(env, JRuby_class, "callRubyMethod2",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callMethod3 = getStaticMethodID(env, JRuby_class, "callRubyMethod3",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_newString = getStaticMethodID(env, JRuby_class, "newString",
            "(Lorg/jruby/Ruby;[BZ)J");
    JRuby_ll2inum = getStaticMethodID(env, JRuby_class, "ll2inum",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_ull2inum = getStaticMethodID(env, JRuby_class, "ull2inum",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_int2big = getStaticMethodID(env, JRuby_class, "int2big",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_uint2big = getStaticMethodID(env, JRuby_class, "uint2big",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_getRString = getStaticMethodID(env, JRuby_class, "getRString", "(Lorg/jruby/RubyString;)J");
    JRuby_newFloat = getStaticMethodID(env, JRuby_class, "newFloat", "(Lorg/jruby/Ruby;JD)Lorg/jruby/RubyFloat;");
    RubyString_value_field = getFieldID(env, RubyString_class, "value", "Lorg/jruby/util/ByteList;");
    RubyFloat_value_field = getFieldID(env, RubyFloat_class, "value", "D");
    ByteList_bytes_field = getFieldID(env, ByteList_class, "bytes", "[B");
    ByteList_begin_field = getFieldID(env, ByteList_class, "begin", "I");
    ByteList_length_field = getFieldID(env, ByteList_class, "realSize", "I");
    RubySymbol_id_field = getFieldID(env, Symbol_class, "id", "I");
    RubySymbol_symbol_field = getFieldID(env, Symbol_class, "symbol", "Ljava/lang/String;");
}

static jobject
callObjectMethod(JNIEnv* env, jobject recv, jmethodID mid)
{
    jobject result = env->CallObjectMethod(recv, mid);
    jruby::checkExceptions(env);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_initNative(JNIEnv* env, jobject self, jobject runtime)
{
    try {
        loadIds(env);
        jruby::runtime = env->NewGlobalRef(runtime);
        
        constHandles[0] = new Handle(env, callObjectMethod(env, runtime, Ruby_getFalse_method), T_FALSE);
        constHandles[0]->flags |= FL_CONST;

        constHandles[1] = new Handle(env, callObjectMethod(env, runtime, Ruby_getTrue_method), T_TRUE);
        constHandles[1]->flags |= FL_CONST;

        constHandles[2] = new Handle(env, callObjectMethod(env, runtime, Ruby_getNil_method), T_NIL);
        constHandles[2]->flags |= FL_CONST;
        
        initRubyClasses(env, runtime);
    } catch (JavaException& ex) {
        return;
    }
}


extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_getNil(JNIEnv* env, jobject self)
{
    return Qnil;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_getTrue(JNIEnv* env, jobject self)
{
    return Qtrue;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_getFalse(JNIEnv* env, jobject self)
{
    return Qfalse;
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    if (jruby::jvm != NULL && jruby::jvm != vm) {
        return JNI_FALSE;
    }

    jruby::jvm = vm;
    return JNI_VERSION_1_4;
}
