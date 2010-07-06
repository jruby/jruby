/*
 * Copyright (C) 2010 Wayne Meissner
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

#include "jruby.h"
#include "ruby.h"
#include "Handle.h"
#include "JLocalEnv.h"

using namespace jruby;


extern "C" void
rb_gc_mark_locations(VALUE* first, VALUE* last)
{
    for (VALUE* vp = first; vp < last; ++vp) {
        rb_gc_mark(*vp);
    }
}

extern "C" void
rb_gc_mark(VALUE v)
{
    if (SPECIAL_CONST_P(v)) {
        // special constant, ignore
        return;
    }

    Handle* h = Handle::valueOf(v);
    if ((h->flags & FL_MARK) == 0) {
        h->flags |= FL_MARK;
    }
}


/*
 * Class:     org_jruby_cext_Native
 * Method:    gc
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_gc(JNIEnv* env, jobject self)
{
    RubyData* dh;
    Handle* h;

    TAILQ_FOREACH(dh, &dataHandles, dataList) {
        if ((dh->flags & FL_MARK) == 0 && dh->dmark != NULL) {
            dh->flags |= FL_MARK;
            (*dh->dmark)(dh->data);
            dh->flags &= ~FL_MARK;
        }
    }

    for (h = TAILQ_FIRST(&liveHandles); h != TAILQ_END(&liveHandles); ) {
        Handle* next = TAILQ_NEXT(h, all);

        if ((h->flags & (FL_MARK | FL_CONST)) == 0) {

            if (unlikely(h->type == T_DATA)) {
                if ((h->flags & FL_WEAK) == 0) {
                    h->flags |= FL_WEAK;
                    jobject obj = env->NewWeakGlobalRef(h->obj);
                    env->DeleteGlobalRef(h->obj);
                    h->obj = obj;
                }
                
            } else {
                TAILQ_REMOVE(&liveHandles, h, all);
                TAILQ_INSERT_TAIL(&deadHandles, h, all);
            }

        } else if ((h->flags & FL_MARK) != 0) {
            h->flags &= ~FL_MARK;
        }

        h = next;
    }
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    pollGC
 * Signature: ()Ljava/lang/Object;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_org_jruby_cext_Native_pollGC(JNIEnv* env, jobject self)
{
    Handle* h = TAILQ_FIRST(&deadHandles);
    if (h == TAILQ_END(&deadHandles)) {
        return NULL;
    }
    TAILQ_REMOVE(&deadHandles, h, all);

    jobject obj = env->NewLocalRef(h->obj);
    env->DeleteGlobalRef(h->obj);
    delete h;

    return obj;
}

