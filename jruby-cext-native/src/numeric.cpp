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

#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"

using namespace jruby;


extern "C" long
rb_num2long(VALUE v)
{
    return (long) rb_num2ll(v);
}

extern "C" unsigned long
rb_num2ulong(VALUE v)
{
    return (unsigned long) rb_num2ull(v);
}

extern "C" long
rb_num2int(VALUE v)
{
    return rb_num2long(v);
}

extern "C" unsigned long
rb_num2uint(VALUE v)
{
    return rb_num2ulong(v);
}

extern "C" long
rb_fix2int(VALUE v)
{
    return rb_num2long(v);
}

extern "C" unsigned long
rb_fix2uint(VALUE v)
{
    return rb_num2ulong(v);
}

extern "C" long long
rb_num2ll(VALUE v)
{
    JLocalEnv env;
    jvalue params[1];
    params[0].l = valueToObject(env, v);

    jlong result = env->CallStaticLongMethodA(RubyNumeric_class, RubyNumeric_num2long_method, params);
    checkExceptions(env);
    
    return (long long) result;
}

extern "C" unsigned long long
rb_num2ull(VALUE v)
{
    return (unsigned long long) rb_num2ll(v);
}

extern "C" VALUE
rb_int2inum(long v)
{
    return rb_ll2inum(v);
}

extern "C" VALUE
rb_uint2inum(unsigned long v)
{
    return rb_ull2inum(v);
}

extern "C" VALUE
rb_ll2inum(long long v)
{
    JLocalEnv env;
    jvalue params[1];
    params[0].j = (jlong) v;

    jobject result = env->CallObjectMethodA(getRuntime(), Ruby_newFixnum_method, params);
    checkExceptions(env);

    return objectToValue(env, result);
}

extern "C" VALUE
rb_ull2inum(unsigned long long v)
{
    return rb_ll2inum(v);
}

extern "C" VALUE
rb_int2big(long long v)
{
    return rb_ll2inum(v);
}

extern "C" VALUE
rb_uint2big(unsigned long long v)
{
    return rb_ull2inum(v);
}

