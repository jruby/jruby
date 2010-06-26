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

Handle* constHandles[3];

Handle::Handle()
{
    obj = NULL;
    flags = 0;
    type = T_NONE;
    finalize = NULL;
}

Handle::~Handle()
{
}

void
Handle::mark()
{
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newHandle(JNIEnv* env, jobject self, jobject obj)
{
    Handle* h = new Handle();
    h->obj = env->NewWeakGlobalRef(obj);
    
    return jruby::p2j(h);
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newFixnumHandle(JNIEnv* env, jobject self, jobject obj, jlong value)
{
    Fixnum* h = new Fixnum(value);
    h->obj = env->NewWeakGlobalRef(obj);
    h->type = T_FIXNUM;

    return jruby::p2j(h);
}

extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_freeHandle(JNIEnv* env, jobject self, jlong address)
{
    if (address == 0LL) {
        jruby::throwExceptionByName(env, NullPointerException, "null handle");
        return;
    }
    
    Handle* h = Handle::valueOf((VALUE) address);
    
    if (h->finalize != NULL) {
        (*h->finalize)(h);
    }

    env->DeleteWeakGlobalRef(h->obj);

    delete h;
}

extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_markHandle(JNIEnv* env, jobject self, jlong address)
{
    if (address == 0LL) {
        jruby::throwExceptionByName(env, NullPointerException, "null handle");
        return;
    }

    // Mark this handle's children
    Handle::valueOf((VALUE) address)->mark();
}

extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_unmarkHandle(JNIEnv* env, jobject self, jlong address)
{
    if (IS_CONST(address)) {
        return; // special constant, ignore
    }

    Handle* h = (Handle *) jruby::j2p(address);
    if (h == NULL) {
        jruby::throwExceptionByName(env, NullPointerException, "null handle");
        return;
    }

    h->flags &= ~FL_MARK;
}

jobject
jruby::valueToObject(JNIEnv* env, VALUE v)
{
    Handle* h = Handle::valueOf(v);
    
    jobject ref = env->NewLocalRef(h->obj);
    if (env->IsSameObject(ref, NULL)) {
        throw JavaException(env, NullPointerException, "invalid RubyObject reference");
    }

    return ref;
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

