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

RubyFloat::RubyFloat(double value)
{
    setType(T_FLOAT);
    rfloat.value = value;
}

RubyFloat::RubyFloat(JNIEnv* env, jobject obj_, jdouble value_): Handle(env, obj_, T_FLOAT)
{
    rfloat.value = value_;
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
rb_Float(VALUE object_handle)
{
    return rb_convert_type(object_handle, 0, "Float", "to_f");
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
