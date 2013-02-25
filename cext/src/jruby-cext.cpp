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
    jclass RubyBignum_class;
    jclass RubyClass_class;
    jclass RubyData_class;
    jclass RubyModule_class;
    jclass RubyNumeric_class;
    jclass RubyFloat_class;
    jclass RubyArray_class;
    jclass RubyString_class;
    jclass RubyStruct_class;
    jclass Handle_class;
    jclass GC_class;
    jclass NativeMethod_class;
    jclass NativeObjectAllocator_class;
    jclass ThreadContext_class;
    jclass Symbol_class;
    jclass JRuby_class;
    jclass ByteList_class;
    jclass Block_class;
    jclass FileDescriptor_class;
    jmethodID JRuby_callMethod;
    jmethodID JRuby_callMethodB;
    jmethodID JRuby_callMethod0;
    jmethodID JRuby_callMethod1;
    jmethodID JRuby_callMethod2;
    jmethodID JRuby_callMethod3;
    jmethodID JRuby_callSuperMethod;
    jmethodID JRuby_instanceEval;
    jmethodID JRuby_clearErrorInfo;
    jmethodID JRuby_newString;
    jmethodID JRuby_ll2inum;
    jmethodID JRuby_ull2inum;
    jmethodID JRuby_int2big;
    jmethodID JRuby_uint2big;
    jmethodID JRuby_newFloat;
    jmethodID JRuby_yield;
    jmethodID JRuby_blockGiven;
    jmethodID JRuby_getBlockProc;
    jmethodID JRuby_gv_get_method;
    jmethodID JRuby_gv_set_method;
    jmethodID JRuby_nativeBlockingRegion;
    jmethodID JRuby_newThread;
    jmethodID JRuby_newProc;
    jmethodID JRuby_sysFail;
    jmethodID JRuby_threadSleep;
    jmethodID JRuby_getMetaClass;

    jmethodID ThreadContext_getRuntime_method;
    
    jmethodID Ruby_defineModule_method;
    jmethodID Ruby_getNil_method;
    jmethodID Ruby_getTrue_method;
    jmethodID Ruby_getFalse_method;
    jmethodID Ruby_getClass_method;
    jmethodID Ruby_getModule_method;
    jmethodID Ruby_newSymbol_method;
    jmethodID Ruby_newFixnum_method;
    jmethodID Ruby_defineClass_method;
    jmethodID Ruby_defineClassUnder_method;
    jmethodID Ruby_getClassFromPath_method;
    jmethodID Ruby_defineReadonlyVariable_method;
    
    jmethodID RaiseException_constructor;
    
    jmethodID RubyData_newRubyData_method;
    
    jmethodID RubyObject_getNativeTypeIndex_method;
    
    jmethodID RubyBignum_big2dbl_method;
    jmethodID RubyBignum_big2long_method;
    jmethodID RubyBignum_big2ulong_method;
    
    jmethodID RubyNumeric_num2long_method;
    jmethodID RubyNumeric_num2chr_method;
    jmethodID RubyNumeric_num2dbl_method;
    jmethodID RubyNumeric_int2fix_method;
    
    jmethodID RubyString_newStringNoCopy;
    jmethodID RubyString_view;
    jmethodID RubyString_resize_method;
    
    jmethodID RubySymbol_getSymbolLong;
    
    jmethodID RubyStruct_newInstance;
    
    jmethodID IRubyObject_callMethod;
    jmethodID IRubyObject_asJavaString_method;
    jmethodID IRubyObject_respondsTo_method;
    
    jmethodID Handle_nativeHandle;
    
    jmethodID Ruby_getCurrentContext_method;
    
    jmethodID GC_trigger;
    
    jmethodID RubyArray_toJavaArray_method;
    
    jmethodID RubyClass_newClass_method;
    jmethodID RubyClass_setAllocator_method;
    jmethodID RubyClass_getAllocator_method;
    
    jmethodID RubyModule_undef_method;

    jmethodID ObjectAllocator_allocate_method;
    
    jmethodID RubyBasicObject_getInstanceVariable_method;
    jmethodID RubyBasicObject_setInstanceVariable_method;
    jmethodID RubyBasicObject_hasInstanceVariable_method;
    
    jmethodID RubyArray_newArray;
    jmethodID RubyArray_clear_method;
    jmethodID RubyArray_append_method;

    jfieldID Handle_address_field;
    jfieldID RubyString_value_field;
    jfieldID RubyFloat_value_field;
    jfieldID ByteList_bytes_field, ByteList_begin_field, ByteList_length_field;
    jfieldID RubySymbol_id_field;
    jfieldID RubySymbol_symbol_field;
    jfieldID Block_null_block_field;
    jfieldID RaiseException_exception_field;
    jfieldID RubyArray_length_field;
    jfieldID ObjectAllocator_NotAllocatableAllocator_field;
    jfieldID FileDescriptor_fd_field;
    jfieldID RubyArray_values_field;
    jfieldID RubyArray_begin_field;

    bool is19;
    jobject runtime;
    jobject nullBlock;
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
        env->ExceptionClear();
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
    RubyBignum_class = loadClass(env, "org/jruby/RubyBignum");
    RubyClass_class = loadClass(env, "org/jruby/RubyClass");
    RubyData_class = loadClass(env, "org/jruby/cext/RubyData");
    RubyFloat_class = loadClass(env, "org/jruby/RubyFloat");
    RubyModule_class = loadClass(env, "org/jruby/RubyModule");
    RubyNumeric_class = loadClass(env, "org/jruby/RubyNumeric");
    RubyFloat_class = loadClass(env, "org/jruby/RubyFloat");
    RubyArray_class = loadClass(env, "org/jruby/RubyArray");
    RubyString_class = loadClass(env, "org/jruby/RubyString");
    RubyStruct_class = loadClass(env, "org/jruby/RubyStruct");
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
    Block_class = loadClass(env, "org/jruby/runtime/Block");
    FileDescriptor_class = loadClass(env, "java/io/FileDescriptor");

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

    RubyBignum_big2dbl_method = getStaticMethodID(env, RubyBignum_class, "big2dbl",
            "(Lorg/jruby/RubyBignum;)D");

    RubyBignum_big2long_method = getStaticMethodID(env, RubyBignum_class, "big2long",
            "(Lorg/jruby/RubyBignum;)J");

    RubyBignum_big2ulong_method = getStaticMethodID(env, RubyBignum_class, "big2ulong",
            "(Lorg/jruby/RubyBignum;)J");

    RubyNumeric_num2long_method = getStaticMethodID(env, RubyNumeric_class, "num2long",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)J");
    RubyNumeric_num2chr_method = getStaticMethodID(env, RubyNumeric_class, "num2chr",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)B");
    RubyNumeric_num2dbl_method = getStaticMethodID(env, RubyNumeric_class, "num2dbl",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)D");
    RubyNumeric_int2fix_method = getStaticMethodID(env, RubyNumeric_class, "int2fix",
            "(Lorg/jruby/Ruby;J)Lorg/jruby/RubyNumeric;");

    GC_trigger = getStaticMethodID(env, GC_class, "trigger", "()V");
    Handle_nativeHandle = getStaticMethodID(env, Handle_class, "nativeHandle",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)J");

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
    RubyStruct_newInstance = getStaticMethodID(env, RubyStruct_class, "newInstance",
            "(Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/Block;)Lorg/jruby/RubyClass;");
    JRuby_callMethod = getStaticMethodID(env, JRuby_class, "callRubyMethod",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;[Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callMethodB = getStaticMethodID(env, JRuby_class, "callRubyMethodB",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callMethod0 = getStaticMethodID(env, JRuby_class, "callRubyMethod0",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;)J");
    JRuby_callMethod1 = getStaticMethodID(env, JRuby_class, "callRubyMethod1",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callMethod2 = getStaticMethodID(env, JRuby_class, "callRubyMethod2",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callMethod3 = getStaticMethodID(env, JRuby_class, "callRubyMethod3",
            "(Lorg/jruby/runtime/builtin/IRubyObject;Ljava/lang/Object;Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_callSuperMethod = getStaticMethodID(env, JRuby_class, "callSuperMethod",
            "(Lorg/jruby/Ruby;[Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_instanceEval = getStaticMethodID(env, JRuby_class, "instanceEval",
            "(Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;)J");
    JRuby_clearErrorInfo = getStaticMethodID(env, JRuby_class, "clearErrorInfo",
            "(Lorg/jruby/Ruby;)V");
    JRuby_newString = getStaticMethodID(env, JRuby_class, "newString",
            "(Lorg/jruby/Ruby;[BIZ)J");
    JRuby_ll2inum = getStaticMethodID(env, JRuby_class, "ll2inum",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_ull2inum = getStaticMethodID(env, JRuby_class, "ull2inum",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_int2big = getStaticMethodID(env, JRuby_class, "int2big",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_uint2big = getStaticMethodID(env, JRuby_class, "uint2big",
            "(Lorg/jruby/Ruby;J)J");
    JRuby_newFloat = getStaticMethodID(env, JRuby_class, "newFloat", "(Lorg/jruby/Ruby;JD)Lorg/jruby/RubyFloat;");
    JRuby_yield = getStaticMethodID(env, JRuby_class, "yield",
            "(Lorg/jruby/Ruby;Lorg/jruby/RubyArray;)Lorg/jruby/runtime/builtin/IRubyObject;");
    JRuby_blockGiven = getStaticMethodID(env, JRuby_class, "blockGiven", "(Lorg/jruby/Ruby;)I");
    JRuby_getBlockProc = getStaticMethodID(env, JRuby_class, "getBlockProc", "(Lorg/jruby/Ruby;)Lorg/jruby/RubyProc;");
    JRuby_getMetaClass = getStaticMethodID(env, JRuby_class, "getMetaClass", "(Lorg/jruby/runtime/builtin/IRubyObject;)J");
    RubyArray_toJavaArray_method = getMethodID(env, RubyArray_class, "toJavaArray",
            "()[Lorg/jruby/runtime/builtin/IRubyObject;");
    RubyClass_newClass_method = getStaticMethodID(env, RubyClass_class, "newClass",
            "(Lorg/jruby/Ruby;Lorg/jruby/RubyClass;)Lorg/jruby/RubyClass;");
    Ruby_defineClass_method = getMethodID(env, Ruby_class, "defineClass",
            "(Ljava/lang/String;Lorg/jruby/RubyClass;Lorg/jruby/runtime/ObjectAllocator;)Lorg/jruby/RubyClass;");
    Ruby_defineClassUnder_method = getMethodID(env, Ruby_class, "defineClassUnder",
            "(Ljava/lang/String;Lorg/jruby/RubyClass;Lorg/jruby/runtime/ObjectAllocator;Lorg/jruby/RubyModule;)Lorg/jruby/RubyClass;");
    RubyClass_setAllocator_method = getMethodID(env, RubyClass_class, "setAllocator",
            "(Lorg/jruby/runtime/ObjectAllocator;)V");
    RubyModule_undef_method = getMethodID(env, RubyModule_class, "undef",
            "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;)V");
    Ruby_getClassFromPath_method = getMethodID(env, Ruby_class, "getClassFromPath",
            "(Ljava/lang/String;)Lorg/jruby/RubyModule;");
    ObjectAllocator_allocate_method = getMethodID(env, ObjectAllocator_class, "allocate",
            "(Lorg/jruby/Ruby;Lorg/jruby/RubyClass;)Lorg/jruby/runtime/builtin/IRubyObject;");
    RubyClass_getAllocator_method = getMethodID(env, RubyClass_class, "getAllocator",
            "()Lorg/jruby/runtime/ObjectAllocator;");
    RubyBasicObject_getInstanceVariable_method = getMethodID(env, RubyBasicObject_class, "getInstanceVariable",
            "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
    RubyBasicObject_setInstanceVariable_method = getMethodID(env, RubyBasicObject_class, "setInstanceVariable",
            "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
    RubyBasicObject_hasInstanceVariable_method = getMethodID(env, RubyBasicObject_class, "hasInstanceVariable",
            "(Ljava/lang/String;)Z");
    JRuby_gv_get_method = getStaticMethodID(env, JRuby_class, "gv_get", "(Lorg/jruby/Ruby;Ljava/lang/String;)J");
    JRuby_gv_set_method = getStaticMethodID(env, JRuby_class, "gv_set",
            "(Lorg/jruby/Ruby;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    Ruby_defineReadonlyVariable_method = getMethodID(env, Ruby_class, "defineReadonlyVariable",
            "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)V");
    JRuby_sysFail = getStaticMethodID(env, JRuby_class, "sysFail", "(Lorg/jruby/Ruby;Ljava/lang/String;I)V");
    RubyString_resize_method = getMethodID(env, RubyString_class, "resize", "(I)V");
    RubyArray_newArray = getStaticMethodID(env, RubyArray_class, "newArray", "(Lorg/jruby/Ruby;J)Lorg/jruby/RubyArray;");
    RubyArray_clear_method = getMethodID(env, RubyArray_class, "clear", "()V");
    RubyArray_append_method = getMethodID(env, RubyArray_class, "append",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/RubyArray;");
    JRuby_threadSleep = getStaticMethodID(env, JRuby_class, "threadSleep", "(Lorg/jruby/Ruby;I)V");
    JRuby_nativeBlockingRegion = getStaticMethodID(env, JRuby_class, "nativeBlockingRegion", "(Lorg/jruby/Ruby;JJJJ)J");
    JRuby_newThread = getStaticMethodID(env, JRuby_class, "newThread",
            "(Lorg/jruby/Ruby;JLorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
    JRuby_newProc = getStaticMethodID(env, JRuby_class, "newProc",
            "(Lorg/jruby/Ruby;J)Lorg/jruby/runtime/builtin/IRubyObject;");

    RubyString_value_field = getFieldID(env, RubyString_class, "value", "Lorg/jruby/util/ByteList;");
    RubyFloat_value_field = getFieldID(env, RubyFloat_class, "value", "D");
    ByteList_bytes_field = getFieldID(env, ByteList_class, "bytes", "[B");
    ByteList_begin_field = getFieldID(env, ByteList_class, "begin", "I");
    ByteList_length_field = getFieldID(env, ByteList_class, "realSize", "I");
    RubySymbol_id_field = getFieldID(env, Symbol_class, "id", "I");
    RubySymbol_symbol_field = getFieldID(env, Symbol_class, "symbol", "Ljava/lang/String;");
    Block_null_block_field = env->GetStaticFieldID(Block_class, "NULL_BLOCK", "Lorg/jruby/runtime/Block;");
    RaiseException_exception_field = getFieldID(env, RaiseException_class, "exception", "Lorg/jruby/RubyException;");
    RubyArray_length_field = getFieldID(env, RubyArray_class, "realLength", "I");
    ObjectAllocator_NotAllocatableAllocator_field = env->GetStaticFieldID(ObjectAllocator_class, "NOT_ALLOCATABLE_ALLOCATOR",
            "Lorg/jruby/runtime/ObjectAllocator;");
    FileDescriptor_fd_field = getFieldID(env, FileDescriptor_class, "fd", "I");
    RubyArray_values_field = getFieldID(env, RubyArray_class, "values", "[Lorg/jruby/runtime/builtin/IRubyObject;");
    RubyArray_begin_field = getFieldID(env, RubyArray_class, "begin", "I");
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
        jruby::is19 = (bool)(env->GetBooleanField(runtime, getFieldID(env, Ruby_class, "is1_9", "Z")) == JNI_TRUE);

        jruby::runtime = env->NewGlobalRef(runtime);
        jruby::nullBlock = env->NewGlobalRef(env->GetStaticObjectField(Block_class, Block_null_block_field));

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
