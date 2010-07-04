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
#include "JUtil.h"
#include "Handle.h"
#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"
#include "org_jruby_cext_Native.h"
#include "JavaException.h"
#include "org_jruby_runtime_ClassIndex.h"

using namespace jruby;

Handle* jruby::constHandles[3];
HandleList jruby::liveHandles = TAILQ_HEAD_INITIALIZER(liveHandles);
HandleList jruby::deadHandles = TAILQ_HEAD_INITIALIZER(deadHandles);
DataSyncQueue jruby::nsyncq = TAILQ_HEAD_INITIALIZER(nsyncq);
DataSyncQueue jruby::jsyncq = TAILQ_HEAD_INITIALIZER(jsyncq);

static int allocCount;
static const int GC_THRESHOLD = 10000;

Handle::Handle()
{
    obj = NULL;
    Init();
}

Handle::Handle(JNIEnv* env, jobject obj_, int type_)
{
    Init();
    this->obj = env->NewGlobalRef(obj_);
    this->type = type_;
}

Handle::~Handle()
{
}

void
Handle::Init()
{
    flags = 0;
    type = T_NONE;
    TAILQ_INSERT_TAIL(&liveHandles, this, all);

    if (++allocCount > GC_THRESHOLD) {
        allocCount = 0;
        JLocalEnv env;
        env->CallStaticVoidMethod(GC_class, GC_trigger);
    }
}

RubyFixnum::RubyFixnum(JNIEnv* env, jobject obj_, jlong value_): Handle(env, obj_, T_FIXNUM)
{
    this->value = value_;
}


RubyString::RubyString(JNIEnv* env, jobject obj_): Handle(env, obj_, T_STRING)
{
    rwdata.rstring = NULL;
}

RubyString::~RubyString()
{
}

int
RubyString::length()
{
    // If already synced with java, just return the cached length value
    if (rwdata.rstring != NULL) {
        return rwdata.rstring->as.heap.len;
    }
    
    JLocalEnv env;

    jobject byteList = env->GetObjectField(obj, RubyString_value_field);

    return env->GetIntField(byteList, ByteList_length_field);
}

static bool
RubyString_jsync(JNIEnv* env, DataSync* data)
{
    return ((RubyString *) data->data)->jsync(env);
}

static bool
RubyString_nsync(JNIEnv* env, DataSync* data)
{
    return ((RubyString *) data->data)->nsync(env);
}

RString*
RubyString::toRString(bool readonly)
{
    if (rwdata.rstring != NULL) {
        if (readonly || !rwdata.readonly) {
            return rwdata.rstring;
        }

        // Switch from readonly to read-write
        rwdata.readonly = false;
        TAILQ_INSERT_TAIL(&jruby::nsyncq, &rwdata.nsync, syncq);
        JLocalEnv env;
        nsync(env);

        return rwdata.rstring;
    }

    JLocalEnv env;
    rwdata.jsync.data = this;
    rwdata.jsync.sync = RubyString_jsync;
    rwdata.nsync.data = this;
    rwdata.nsync.sync = RubyString_nsync;
    rwdata.rstring = (RString *) j2p(env->CallStaticLongMethod(JRuby_class, JRuby_getRString, obj));
    rwdata.readonly = readonly;

    TAILQ_INSERT_TAIL(&jruby::jsyncq, &rwdata.jsync, syncq);
    if (!readonly) {
        TAILQ_INSERT_TAIL(&jruby::nsyncq, &rwdata.nsync, syncq);
    }
    nsync(env);

    return rwdata.rstring;
}

bool
RubyString::jsync(JNIEnv* env)
{
    if (rwdata.readonly && rwdata.rstring != NULL) {
        // Don't sync anything, just clear the cached data
        rwdata.rstring = NULL;
        rwdata.readonly = false;
        return false;
    }

    if (rwdata.rstring != NULL && rwdata.rstring->as.heap.ptr != NULL) {
        jobject byteList = env->GetObjectField(obj, RubyString_value_field);
        jobject bytes = env->GetObjectField(byteList, ByteList_bytes_field);
        jint begin = env->GetIntField(byteList, ByteList_begin_field);

        RString* rstring = rwdata.rstring;
        env->SetByteArrayRegion((jbyteArray) bytes, begin, rstring->as.heap.len,
                (jbyte *) rstring->as.heap.ptr);
        
        env->DeleteLocalRef(byteList);
        env->DeleteLocalRef(bytes);
    }

    return true;
}

bool
RubyString::nsync(JNIEnv* env)
{
    jobject byteList = env->GetObjectField(obj, RubyString_value_field);
    jobject bytes = env->GetObjectField(byteList, ByteList_bytes_field);
    jint begin = env->GetIntField(byteList, ByteList_begin_field);
    long length = env->GetIntField(byteList, ByteList_length_field);

    RString* rstring = rwdata.rstring;

    if (length > rstring->as.heap.capa) {
        rstring->as.heap.capa = env->GetArrayLength((jarray) bytes);
        rstring->as.heap.ptr = (char *) realloc(rstring->as.heap.ptr, rstring->as.heap.capa + 1);
    }
    
    rstring->as.heap.len = length;
    
    env->GetByteArrayRegion((jbyteArray) bytes, begin, length, 
            (jbyte *) rstring->as.heap.ptr);
    rstring->as.heap.ptr[length] = 0;

    env->DeleteLocalRef(byteList);
    env->DeleteLocalRef(bytes);

    return true;
}

RubyArray::RubyArray(JNIEnv* env, jobject obj_): Handle(env, obj_, T_ARRAY)
{
}

RubyArray::~RubyArray()
{
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newHandle(JNIEnv* env, jobject self, jobject obj, jint type)
{
    Handle* h;
    switch (type) {
#define T(x) \
        case org_jruby_runtime_ClassIndex_##x: \
            h = new Handle(env, obj, T_##x); \
            break;
        T(FIXNUM);
        T(BIGNUM);
        T(NIL);
        T(TRUE);
        T(FALSE);
        T(SYMBOL);
        T(REGEXP);
        T(HASH);
        T(FLOAT);
        T(MODULE);
        T(CLASS);
        T(OBJECT);
        T(STRUCT);
        T(FILE);

        case org_jruby_runtime_ClassIndex_NO_INDEX:
            h = new Handle(env, obj, T_NONE);
            break;

        case org_jruby_runtime_ClassIndex_MATCHDATA:
            h = new Handle(env, obj, T_MATCH);
            break;

        case org_jruby_runtime_ClassIndex_STRING:
            h = new RubyString(env, obj);
            break;

        case org_jruby_runtime_ClassIndex_ARRAY:
            h = new RubyArray(env, obj);
            break;

        default:
            h = new Handle(env, obj, T_OBJECT);
            break;
    }

    return jruby::p2j(h);
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newFixnumHandle(JNIEnv* env, jobject self, jobject obj, jlong value)
{
    return jruby::p2j(new RubyFixnum(env, obj, value));
}

jobject
jruby::valueToObject(JNIEnv* env, VALUE v)
{
    return env->NewLocalRef(Handle::valueOf(v)->obj);
}

VALUE
jruby::objectToValue(JNIEnv* env, jobject obj)
{
    // Should never get null from JRuby, but check it anyway
    if (env->IsSameObject(obj, NULL)) {
    
        return Qnil;
    }

    jobject handleObject = env->CallStaticObjectMethod(Handle_class, Handle_valueOf, obj);
    checkExceptions(env);

    VALUE v = (VALUE) env->GetLongField(handleObject, Handle_address_field);
    checkExceptions(env);
    
    env->DeleteLocalRef(handleObject);

    return v;
}

void
jruby::runSyncQueue(JNIEnv *env, DataSyncQueue* q)
{
    DataSync* d;

    for (d = TAILQ_FIRST(q); d != TAILQ_END(q); ) {
        DataSync* next = TAILQ_NEXT(d, syncq);

        if (!(*d->sync)(env, d)) {
            TAILQ_REMOVE(q, d, syncq);
        }

        d = next;
    }
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    newRString
 * Signature: ()J
 */
extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newRString(JNIEnv* env, jclass self)
{
    return p2j(calloc(1, sizeof(struct RString)));
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    freeRString
 * Signature: (J)V
 */
extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_freeRString(JNIEnv* env, jclass self, jlong address)
{
    RString* rstring = (RString *) j2p(address);
    if (rstring != NULL) {
        if (rstring->as.heap.ptr != NULL) {
            free(rstring->as.heap.ptr);
        }

        free(rstring);
    }
}


