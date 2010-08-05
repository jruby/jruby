/*
 * Copyright (C) 2009, 2010 Wayne Meissner
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

#include "queue.h"

#include "JUtil.h"
#include "jruby.h"
#include "JavaException.h"
#include "ruby.h"
#include "JLocalEnv.h"
#include "Handle.h"

using namespace jruby;
DataHandleList jruby::dataHandles = TAILQ_HEAD_INITIALIZER(dataHandles);

static void rubydata_finalize(Handle *);

extern "C" VALUE
rb_data_object_alloc(VALUE klass, void* data, RUBY_DATA_FUNC dmark, RUBY_DATA_FUNC dfree)
{
    JLocalEnv env;

    RubyData* h = new RubyData;

    TAILQ_INSERT_TAIL(&dataHandles, h, dataList);
    h->toRData()->data = data;
    h->toRData()->dmark = dmark;
    h->toRData()->dfree = dfree;
    h->setType(T_DATA);

    jvalue params[3];
    params[0].l = getRuntime();
    params[1].l = valueToObject(env, klass);
    params[2].j = p2j(h);

    jobject obj = env->CallStaticObjectMethodA(RubyData_class, RubyData_newRubyData_method, params);
    checkExceptions(env);

    h->obj = env->NewGlobalRef(obj);
    checkExceptions(env);


    return (VALUE) (uintptr_t) h;
}

RubyData::~RubyData()
{
    TAILQ_REMOVE(&dataHandles, this, dataList);

    if (rdata.dfree == (void *) -1) {
        xfree(rdata.data);

    } else if (rdata.dfree != NULL) {
        (*rdata.dfree)(rdata.data);
    }
}

extern "C" void*
jruby_data(VALUE v)
{
    if (TYPE(v) != T_DATA) {
        rb_raise(rb_eTypeError, "not a data object");
        return NULL;
    }

    return ((RubyData *) v)->toRData()->data;
}

extern "C" struct RData*
jruby_rdata(VALUE v)
{
    if (TYPE(v) != T_DATA) {
        rb_raise(rb_eTypeError, "not a data object");
        return NULL;
    }

    RubyData* d = dynamic_cast<RubyData*>(Handle::valueOf(v));

    return d->toRData();
}
