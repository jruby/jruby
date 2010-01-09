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
    if ((v & ~0x7L) == 0) {
        // special constant, ignore
        return;
    }

    Handle* h = (Handle *)v;
    if ((h->flags & FL_MARK) == 0) {
        h->flags |= FL_MARK;

        JLocalEnv env;
        jobject obj = env->NewLocalRef(h->obj);

        // Mark this object as strongly reachable
        if (!env->IsSameObject(obj, NULL)) {
            env->CallStaticVoidMethod(GC_class, GC_mark, obj);
        }

        // Mark any children if this is a data object
        if (h->dmark != NULL) {
            if (h->dmark != NULL) {
                h->dmark(h->data);
            }
        }
    }
}
