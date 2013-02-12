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

#ifndef JRUBY_H
#define JRUBY_H

#include <jni.h>
#include <map>
#include "queue.h"
#include "util.h"
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
    extern jclass RubyBignum_class;
    extern jclass RubyData_class;
    extern jclass RubyFloat_class;
    extern jclass RubyObject_class;
    extern jclass RubyBasicObject_class;
    extern jclass RubyClass_class;
    extern jclass RaiseException_class;
    extern jclass RubyModule_class;
    extern jclass RubyNumeric_class;
    extern jclass RubyFloat_class;
    extern jclass RubyArray_class;
    extern jclass RubyString_class;
    extern jclass RubyStruct_class;
    extern jclass IRubyObject_class;
    extern jclass GC_class;
    extern jclass Handle_class;
    extern jclass ThreadContext_class;
    extern jclass Symbol_class;
    extern jclass JRuby_class;
    extern jclass ByteList_class;
    extern jclass FileDescriptor_class;

    extern jmethodID IRubyObject_callMethod;
    extern jmethodID IRubyObject_asJavaString_method;
    extern jmethodID IRubyObject_respondsTo_method;
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
    extern jmethodID RubyBignum_big2long_method;
    extern jmethodID RubyBignum_big2ulong_method;
    extern jmethodID RubyBignum_big2dbl_method;
    extern jmethodID RubyNumeric_num2long_method;
    extern jmethodID RubyNumeric_num2chr_method;
    extern jmethodID RubyNumeric_num2dbl_method;
    extern jmethodID RubyNumeric_int2fix_method;
    extern jmethodID RubyString_newStringNoCopy;
    extern jmethodID RubyString_view;
    extern jmethodID RubySymbol_getSymbolLong;
    extern jmethodID RubyStruct_newInstance;
    extern jmethodID GC_trigger;
    extern jmethodID Handle_nativeHandle;
    extern jmethodID RubyObject_getNativeTypeIndex_method;
    extern jmethodID JRuby_callMethod;
    extern jmethodID JRuby_callMethodB;
    extern jmethodID JRuby_callMethod0;
    extern jmethodID JRuby_callMethod1;
    extern jmethodID JRuby_callMethod2;
    extern jmethodID JRuby_callMethod3;
    extern jmethodID JRuby_callSuperMethod;
    extern jmethodID JRuby_instanceEval;
    extern jmethodID JRuby_clearErrorInfo;
    extern jmethodID JRuby_newString;
    extern jmethodID JRuby_newFloat;
    extern jmethodID JRuby_ll2inum;
    extern jmethodID JRuby_ull2inum;
    extern jmethodID JRuby_int2big;
    extern jmethodID JRuby_uint2big;
    extern jmethodID JRuby_getRString;
    extern jmethodID JRuby_getRArray;
    extern jmethodID JRuby_yield;
    extern jmethodID JRuby_blockGiven;
    extern jmethodID JRuby_getBlockProc;
    extern jmethodID JRuby_gv_get_method;
    extern jmethodID JRuby_gv_set_method;
    extern jmethodID JRuby_nativeBlockingRegion;
    extern jmethodID JRuby_newThread;
    extern jmethodID JRuby_newProc;
    extern jmethodID JRuby_getMetaClass;

    extern jmethodID RubyArray_toJavaArray_method;
    extern jmethodID RubyClass_newClass_method;
    extern jmethodID Ruby_defineClass_method;
    extern jmethodID Ruby_defineClassUnder_method;
    extern jmethodID RubyClass_setAllocator_method;
    extern jmethodID RubyModule_undef_method;
    extern jmethodID Ruby_getClassFromPath_method;
    extern jmethodID ObjectAllocator_allocate_method;
    extern jmethodID RubyClass_getAllocator_method;
    extern jmethodID RubyBasicObject_getInstanceVariable_method;
    extern jmethodID RubyBasicObject_setInstanceVariable_method;
    extern jmethodID RubyBasicObject_hasInstanceVariable_method;
    extern jmethodID Ruby_defineReadonlyVariable_method;
    extern jmethodID JRuby_sysFail;
    extern jmethodID RubyString_resize_method;
    extern jmethodID RubyArray_newArray;
    extern jmethodID RubyArray_clear_method;
    extern jmethodID RubyArray_append_method;
    extern jmethodID JRuby_threadSleep;
    extern jfieldID Handle_address_field;
    extern jfieldID RubyString_value_field;
    extern jfieldID ByteList_bytes_field, ByteList_begin_field, ByteList_length_field;
    extern jfieldID RubyFloat_value_field;
    extern jfieldID RubySymbol_id_field;
    extern jfieldID RubySymbol_symbol_field;
    extern jfieldID RaiseException_exception_field;
    extern jfieldID RubyArray_length_field;
    extern jfieldID ObjectAllocator_NotAllocatableAllocator_field;
    extern jfieldID FileDescriptor_fd_field;
    extern jfieldID RubyArray_values_field;
    extern jfieldID RubyArray_begin_field;

    extern bool is19;
    extern jobject runtime;
    extern jobject nullBlock;
    extern jobject nilRef;
    extern jobject trueRef;
    extern jobject falseRef;
    extern std::map<const char*, jobject> methodNameMap;

    void initRubyClasses(JNIEnv* env, jobject runtime);

    Handle* newHandle(JNIEnv* env);

    VALUE callMethod(VALUE recv, jobject methodName, int argCount, ...);
    VALUE callMethodA(VALUE recv, jobject methodName, int argCount, VALUE* args);
    VALUE callMethodConst(VALUE recv, const char* methodName, int argCount, ...);
    VALUE callMethodNonConst(VALUE recv, const char* methodName, int argCount, ...);
    VALUE callMethodAConst(VALUE recv, const char* methodName, int argCount, VALUE* args);
    VALUE callMethodANonConst(VALUE recv, const char* methodName, int argCount, VALUE* args);
    VALUE callRubyMethod(JNIEnv* env, VALUE recv, jobject obj, int argCount, ...);
    VALUE callRubyMethodA(JNIEnv* env, VALUE recv, jobject obj, int argCount, VALUE* args);
    VALUE callRubyMethodB(JNIEnv* env, VALUE recv, jobject obj, int argCount, VALUE* args, VALUE block);
    VALUE callRubyMethodV(JNIEnv* env, VALUE recv, jobject obj, int argCount, va_list ap);

    jobject getConstMethodNameInstance(const char* methodName);
    jobject getConstMethodNameInstance(JNIEnv* env, const char* methodName);


#define CONST_METHOD_NAME_CACHE(name) __extension__({          \
        static jobject mid_;                             \
        if (__builtin_expect(!mid_, 0))                  \
            mid_ = jruby::getConstMethodNameInstance(name);      \
        mid_;                                            \
    })

#define callMethod(recv, method, argCount, a...) \
    (likely(__builtin_constant_p(method)) \
        ? jruby::callMethod(recv, CONST_METHOD_NAME_CACHE(method), argCount, ##a) \
        : jruby::callMethodNonConst(recv, method, argCount, ##a))

#define callMethodA(recv, method, argc, argv) __extension__ \
    (likely(__builtin_constant_p(method)) \
        ? jruby::callMethodA(recv, CONST_METHOD_NAME_CACHE(method), argc, argv) \
        : jruby::callMethodANonConst(recv, method, argc, argv))

#define getCachedMethodID(env, klass, methodName, signature) __extension__({ \
    static jmethodID mid_; \
    static jclass klass_; \
    jmethodID mid; \
    if (__builtin_constant_p(methodName) && __builtin_constant_p(signature) && (klass_ == NULL || env->IsSameObject(klass_, klass))) { \
        if (unlikely(mid_ == NULL)) { \
            mid_ = getMethodID(env, klass, methodName, signature); \
            if (klass_ == NULL) klass_ = (jclass) env->NewWeakGlobalRef(klass); \
        } \
        mid = mid_; \
    } else mid = getMethodID(env, klass, methodName, signature); \
    mid; \
})

    VALUE getClass(const char* className);
    VALUE getModule(const char* className);
    VALUE getSymbol(const char* name);

    int typeOf(JNIEnv* env, jobject obj);

    jobject valueToObject(JNIEnv* env, VALUE v);
    VALUE objectToValue(JNIEnv* env, jobject obj);

    inline bool is1_9() { return jruby::is19; }
    inline jobject getRuntime() { return jruby::runtime; }
    inline jobject getNullBlock() { return jruby::nullBlock; }
    jobject getTrue();
    jobject getFalse();
    jobject getNil();
    void checkExceptions(JNIEnv* env);

    VALUE getModule(JNIEnv* env, const char* className);
    VALUE getClass(JNIEnv* env, const char* className);

    jfieldID getFieldID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);
    jmethodID getStaticMethodID(JNIEnv* env, jclass klass, const char* methodName, const char* signature);

    TAILQ_HEAD(DataSyncQueue, DataSync);
    extern DataSyncQueue jsyncq, nsyncq, cleanq;
}

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

#endif  /* JRUBY_H */
