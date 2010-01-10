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

using namespace jruby;

#define IS_CONST(x) (((x) & ~0x7L) == 0L)

Handle::Handle()
{
    obj = NULL;
    flags = 0;
    type = 0;
    finalize = NULL;
    dmark = dfree = NULL;
    data = NULL;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newHandle(JNIEnv* env, jobject self, jobject obj)
{
    Handle* h = new Handle();
    h->obj = env->NewWeakGlobalRef(obj);
    h->flags = 0;
    h->type = T_NONE;
    h->finalize = NULL;
    h->dmark = h->dfree = NULL;
    h->data = NULL;

    return jruby::p2j(h);
}

extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_freeHandle(JNIEnv* env, jobject self, jlong address)
{
    if (IS_CONST(address)) {
        return; // special constant, ignore
    }

    Handle* h = (Handle *) jruby::j2p(address);
    if (h == NULL) {
        jruby::throwExceptionByName(env, NullPointerException, "null handle");
        return;
    }
    
    if (h->finalize != NULL) {
        (*h->finalize)(h);
    }

    env->DeleteWeakGlobalRef(h->obj);

    delete h;
}

extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_markHandle(JNIEnv* env, jobject self, jlong address)
{
    if (IS_CONST(address)) {
        return; // special constant, ignore
    }
    
    Handle* h = (Handle *) jruby::j2p(address);
    if (h == NULL) {
        jruby::throwExceptionByName(env, NullPointerException, "null handle");
        return;
    }

    // Mark this handle's children
    if (h->dmark != NULL) {
        (*h->dmark)(h->data);
    }
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
    if (IS_CONST(v)) {
        switch (v) {
            case Qnil:
                return getNilRef(env);

            case Qfalse:
                return getFalseRef(env);

            case Qtrue:
                return getTrueRef(env);
        }
        rb_raise(rb_eRuntimeError, "invalid C ext constant");

        return NULL;
    } else {

        jobject ref = env->NewLocalRef(((Handle *) v)->obj);
        if (env->IsSameObject(ref, NULL)) {
            throw JavaException(env, NullPointerException, "invalid RubyObject reference");
        }

        return ref;
    }
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

