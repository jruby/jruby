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
 * Copyright (C) 2010 Wayne Meissner
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

#include <vector>
#include <list>
#include "jruby.h"
#include "ruby.h"
#include "Handle.h"
#include "JLocalEnv.h"

using namespace jruby;

static std::list<VALUE*> globalVariables;

extern "C" void
rb_gc_mark_locations(VALUE* first, VALUE* last)
{
    for (VALUE* vp = first; vp < last; ++vp) {
        rb_gc_mark(*vp);
    }
}

static void
gc_mark_children(Handle* h)
{
    switch (h->getType()) {
        case T_ARRAY:
            dynamic_cast<RubyArray *>(h)->markElements();
            break;

        case T_DATA: {
            RData* rdata = dynamic_cast<RubyData *>(h)->toRData();
            if (rdata->dmark != NULL) {
	        (*rdata->dmark)(rdata->data);
	    }
            break;
        }
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
        gc_mark_children(h);
    }
}

extern "C" void
rb_gc_mark_maybe(VALUE v)
{
    if (SPECIAL_CONST_P(v)) {
        return;
    }

    Handle* h;
    TAILQ_FOREACH(h, &liveHandles, all) {
        if (h->asValue() == v) {
            rb_gc_mark(v);
            break;
        }
    }
}

extern "C" void
rb_gc_register_address(VALUE *addr)
{
    globalVariables.push_back(addr);
}

extern "C" void
rb_gc_unregister_address(VALUE *addr)
{
    globalVariables.remove(addr);
}

extern "C" void 
rb_gc() 
{
    // We'll let the Java GC decide when to run
}

extern "C" void
rb_global_variable(VALUE *var)
{
    rb_gc_register_address(var);
}

/*
 * Class:     org_jruby_cext_Native
 * Method:    gc
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL
Java_org_jruby_cext_Native_gc(JNIEnv* env, jobject self)
{
    Handle* h;

    /*
     * Mark on all global vars, so they don't get pruned out
     */
    for (std::list<VALUE*>::iterator it = globalVariables.begin(); it != globalVariables.end(); ++it) {
        VALUE* vp = *it;
        rb_gc_mark(*vp);
    }
    
    /*
     * Mark the children of all handles, but not the handle itself, so if it 
     * is not referenced by other handles, it can be pruned.
     */
    TAILQ_FOREACH(h, &liveHandles, all) {
        if ((h->flags & FL_MARK) == 0) {
            gc_mark_children(h);
        }
    }

    TAILQ_FOREACH(h, &liveHandles, all) {

        if ((h->flags & (FL_MARK | FL_CONST)) == 0) {

            h->makeWeak(env);

        } else if ((h->flags & FL_MARK) != 0) {
	    // If the handle was marked, but was not strongly reffed, make it a strong ref again
            if (h->isWeak()) {
		jobject tmp = env->NewLocalRef(h->obj);
		if (!env->IsSameObject(tmp, NULL)) {
                    h->makeStrong(env);
		}
	    }
            h->flags &= ~FL_MARK;
        }
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
    return NULL;
}
