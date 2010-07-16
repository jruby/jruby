/*
 * Copyright (C) 2010 Wayne Meissner, Tim Felgentreff
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
#include "Handle.h"
#include "JLocalEnv.h"
#include "JUtil.h"

using namespace jruby;

RubyFloat::RubyFloat(double value): registered_(false)
{
    setType(T_FLOAT);
    rfloat_.value = value;
}

RubyFloat::RubyFloat(JNIEnv* env, jobject obj_, jdouble value_): Handle(env, obj_, T_FLOAT)
{
    rfloat_.value = value_;
}


static bool
RubyFloat_jsync(JNIEnv* env, DataSync* data)
{
    return ((RubyFloat *) data->data)->jsync(env);
}

static bool
RubyFloat_nsync(JNIEnv* env, DataSync* data)
{
    return ((RubyFloat *) data->data)->nsync(env);
}

static bool
RubyFloat_clean(JNIEnv* env, DataSync* data)
{
    return ((RubyFloat *) data->data)->clean(env);
}

struct RFloat*
RubyFloat::toRFloat()
{
    if (!registered_) {
        jsync_.data = this;
        jsync_.sync = RubyFloat_jsync;
        nsync_.data = this;
        nsync_.sync = RubyFloat_nsync;
        clean_.data = this;
        clean_.sync = RubyFloat_clean;
        TAILQ_INSERT_TAIL(&jruby::cleanq, &clean_, syncq);
        TAILQ_INSERT_TAIL(&jruby::jsyncq, &jsync_, syncq);
        TAILQ_INSERT_TAIL(&jruby::nsyncq, &nsync_, syncq);
        registered_ = true;
    }
    
    return &rfloat_;
}

bool
RubyFloat::jsync(JNIEnv* env)
{
    env->SetDoubleField(obj, RubyFloat_value_field, rfloat_.value);
    return true;
}

bool
RubyFloat::nsync(JNIEnv* env)
{
    rfloat_.value = env->GetDoubleField(obj, RubyFloat_value_field);
    return true;
}

bool
RubyFloat::clean(JNIEnv* env)
{
    registered_ = false;
    return true;
}

extern "C" struct RFloat*
jruby_rfloat(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_FLOAT) {
        return ((RubyFloat *) h)->toRFloat();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Float)");
}

extern "C" VALUE
rb_float_new(double value)
{
    JLocalEnv env;

    //env->CallStaticObjectMethod();
    RubyFloat* f = new RubyFloat(value);
    jvalue params[3];
    params[0].l = jruby::getRuntime();
    params[1].j = p2j(f);
    params[2].d = value;
    
    jobject rubyFloat = env->CallStaticObjectMethodA(JRuby_class, JRuby_newFloat, params);
    f->obj = env->NewGlobalRef(rubyFloat);

    return (VALUE) f;
}

extern "C" double
jruby_float_value(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_FLOAT) {
        return ((RubyFloat *) h)->doubleValue();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Float)");
}

extern "C" VALUE
rb_Float(VALUE obj)
{
    if (TYPE(obj) == T_FLOAT) return obj;
    return callMethod(obj, "to_f", 0);
}

extern "C" double
rb_num2dbl(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_FLOAT) {
        return ((RubyFloat *) h)->doubleValue();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Float)");
}
