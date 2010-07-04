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

void
Handle::jsync(JNIEnv* env)
{
}

void
Handle::nsync(JNIEnv* env)
{
}


RubyFixnum::RubyFixnum(JNIEnv* env, jobject obj_, jlong value_): Handle(env, obj_, T_FIXNUM)
{
    this->value = value_;
}


RubyString::RubyString(JNIEnv* env, jobject obj_): Handle(env, obj_, T_STRING)
{
    rwdata = NULL;
}

RubyString::~RubyString()
{
    if (rwdata != NULL) {
        free(rwdata);
    }
}

int
RubyString::length()
{
    // If already synced with java, just return the cached length value
    if (rwdata != NULL) {
        return rwdata->rstring.length;
    }
    
    JLocalEnv env;

    jobject byteList = env->GetObjectField(obj, RubyString_value_field);

    return env->GetIntField(byteList, ByteList_length_field);
}

static bool
RubyString_jsync(JNIEnv* env, DataSync* data)
{
    ((RubyString *) data->data)->jsync(env);
    return true;
}

static bool
RubyString_nsync(JNIEnv* env, DataSync* data)
{
    ((RubyString *) data->data)->nsync(env);
    return true;
}

RString*
RubyString::toRString(bool readonly)
{
    if (rwdata != NULL) {
        return &rwdata->rstring;
    }
    
    if (rwdata == NULL) {
        rwdata = (RWData *) malloc(sizeof(*rwdata));
        rwdata->jsync.data = this;
        rwdata->jsync.sync = RubyString_jsync;
        rwdata->nsync.data = this;
        rwdata->nsync.sync = RubyString_nsync;
        rwdata->rstring.ptr = NULL;
        rwdata->rstring.length = -1;
        TAILQ_INSERT_TAIL(&jruby::jsyncq, &rwdata->jsync, syncq);
        TAILQ_INSERT_TAIL(&jruby::nsyncq, &rwdata->nsync, syncq);
    }

    JLocalEnv env;
    nsync(env);

    return &rwdata->rstring;
}

void
RubyString::jsync(JNIEnv* env)
{
    if (rwdata != NULL && rwdata->rstring.ptr != NULL) {
        jobject byteList = env->GetObjectField(obj, RubyString_value_field);
        jobject bytes = env->GetObjectField(byteList, ByteList_bytes_field);
        jint begin = env->GetIntField(byteList, ByteList_begin_field);
        
        env->SetByteArrayRegion((jbyteArray) bytes, begin, rwdata->rstring.length,
                (jbyte *) rwdata->rstring.ptr);
        
        env->DeleteLocalRef(byteList);
        env->DeleteLocalRef(bytes);
    }
}

void
RubyString::nsync(JNIEnv* env)
{
    jobject byteList = env->GetObjectField(obj, RubyString_value_field);
    jobject bytes = env->GetObjectField(byteList, ByteList_bytes_field);
    jint begin = env->GetIntField(byteList, ByteList_begin_field);
    jint length = env->GetIntField(byteList, ByteList_length_field);

    rwdata->rstring.ptr = (char *) realloc(rwdata->rstring.ptr, length + 1);
    rwdata->rstring.length = length;
    
    env->GetByteArrayRegion((jbyteArray) bytes, begin, length, 
            (jbyte *) rwdata->rstring.ptr);
    rwdata->rstring.ptr[length] = 0;

    env->DeleteLocalRef(byteList);
    env->DeleteLocalRef(bytes);
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
    RString* rstring = new RString;
    rstring->ptr = NULL;
    rstring->length = -1;

    return p2j(rstring);
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

    if (rstring->ptr != NULL) {
        free(rstring->ptr);
    }

    delete rstring;
}


