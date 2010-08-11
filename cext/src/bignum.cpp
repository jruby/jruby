/*
 * Copyright (C) 2010 Tim Felgentreff
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

extern "C" int
jruby_big_bytes_used(VALUE obj)
{
    return NUM2INT(callMethod(obj, "size", 0));
}

extern "C" long long
rb_big2ll(VALUE obj)
{
    JLocalEnv env;
    jvalue params[1];

    params[0].l = valueToObject(env, obj);
    jlong result = env->CallStaticLongMethodA(RubyNumeric_class, RubyNumeric_num2long_method, params);
    checkExceptions(env);

    return (long long) result;
}

extern "C" long
rb_big2long(VALUE obj)
{
    return (long) rb_big2ll(obj);
}

extern "C" unsigned long
rb_big2ulong(VALUE obj)
{
    if (TYPE(obj) == T_BIGNUM) {
        JLocalEnv env;
        jlong result = env->CallStaticLongMethod(RubyBignum_class, RubyBignum_big2ulong_method, valueToObject(env, obj));
        checkExceptions(env);
        return (unsigned long) result;
    } else {
        return (unsigned long) rb_big2ll(obj);
    }
}

extern "C" double
rb_big2dbl(VALUE obj)
{
    JLocalEnv env;
    jvalue params[1];

    params[0].l = valueToObject(env, obj);
    jdouble result = env->CallStaticDoubleMethodA(RubyBignum_class, RubyBignum_big2dbl_method, params);
    checkExceptions(env);

    return result;
}

extern "C" VALUE
rb_big2str(VALUE obj, int radix)
{
    return callMethod(obj, "to_s", 1, INT2FIX(radix));
}
