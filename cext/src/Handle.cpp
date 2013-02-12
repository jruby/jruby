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
DataSyncQueue jruby::cleanq = TAILQ_HEAD_INITIALIZER(cleanq);

Handle::Handle()
{
    obj = NULL;
    Init();
}

Handle::Handle(JNIEnv* env, jobject obj_, int type_)
{
    Init();
    setType(type_);
    this->obj = env->NewGlobalRef(obj_);
}

Handle::~Handle()
{
}

void
Handle::Init()
{
    flags = 0;
    setType(T_NONE);
    TAILQ_INSERT_TAIL(&liveHandles, this, all);
}

Handle*
Handle::specialHandle(VALUE v)
{
    if (likely((RSHIFT(v, 1) & ~3UL) == 0)) {
        return jruby::constHandles[(v >> 1) & 0x3UL];

    }

    rb_raise(rb_eTypeError, "%llx is not a valid handle", (unsigned long long) v);
    return NULL;
}

void
Handle::makeStrong_(JNIEnv* env)
{
    if (isWeak()) {
        jobject tmp = env->NewLocalRef(obj);
        if (unlikely(env->IsSameObject(tmp, NULL))) {
            rb_raise(rb_eRuntimeError, "weak handle is null");
        }
        env->DeleteWeakGlobalRef((jweak) obj);
        obj = env->NewGlobalRef(tmp);
        env->DeleteLocalRef(tmp);
        flags &= ~FL_WEAK;
    }
}

void
Handle::makeWeak_(JNIEnv* env)
{
    if (!isWeak()) {
        jobject tmp = env->NewLocalRef(obj);
        env->DeleteGlobalRef(obj);
        obj = env->NewWeakGlobalRef(tmp);
        env->DeleteLocalRef(tmp);
        flags |= FL_WEAK;
    }
}


RubyFixnum::RubyFixnum(JNIEnv* env, jobject obj_, jlong value_): Handle(env, obj_, T_FIXNUM)
{
    this->value = value_;
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

        T(BIGNUM);
        T(NIL);
        T(TRUE);
        T(FALSE);
        T(SYMBOL);
        T(REGEXP);
        T(HASH);
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

        case org_jruby_runtime_ClassIndex_FLOAT:
            h = new RubyFloat(env, obj, env->GetDoubleField(obj, RubyFloat_value_field));
            break;

        case org_jruby_runtime_ClassIndex_TIME:
            h = new Handle(env, obj, T_DATA); // Special case handling
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

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newFloatHandle(JNIEnv* env, jobject self, jobject obj, jdouble value)
{
    return jruby::p2j(new RubyFloat(env, obj, value));
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jruby_cext_Native_newIOHandle(JNIEnv* env, jobject self, jobject obj, jint fileno, jint mode)
{
    return jruby::p2j(new RubyIO(env, obj, fileno, mode));
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    freeHandle
 * Signature: (J)V
 */
extern "C" JNIEXPORT void
JNICALL Java_org_jruby_cext_Native_freeHandle(JNIEnv* env, jclass self, jlong address)
{
    Handle* h = reinterpret_cast<Handle*>(address);

    TAILQ_REMOVE(&liveHandles, h, all);

    if (h->isWeak()) {
        env->DeleteWeakGlobalRef((jweak) h->obj);
    } else {
        env->DeleteGlobalRef(h->obj);
    }

    delete h;
}


jobject
jruby::valueToObject(JNIEnv* env, VALUE v)
{
    if (FIXNUM_P(v)) {
        return fixnumToObject(env, v);

    } else if (SYMBOL_P(v)) {
        return idToObject(env, SYM2ID(v));
    }

    Handle* h = Handle::valueOf(v);
    // FIXME: Sometimes h will not be a pointer here, and segfault on the next line
    if (likely((h->flags & FL_WEAK) == 0)) {
        return h->obj;
    }

    jobject obj = env->NewLocalRef(h->obj);
    if (unlikely(env->IsSameObject(obj, NULL))) {
        rb_raise(rb_eRuntimeError, "weak handle is null");
    }

    return obj;
}

VALUE
jruby::objectToValue(JNIEnv* env, jobject obj)
{
    // Should never get null from JRuby, but check for it just in case
    if (env->IsSameObject(obj, NULL)) {
        return Qnil;
    }

    VALUE v = (VALUE) env->CallStaticLongMethod(Handle_class, Handle_nativeHandle, obj);
    makeStrongRef(env, v);
    checkExceptions(env);

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
