/*
 * Copyright (C) 2008 - 2010 Wayne Meissner
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

#define CACHE_OFFSET (128)
VALUE fixnumCache[2 * CACHE_OFFSET];

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
    Handle* h = Handle::valueOf(v);
    if (h->type == T_FIXNUM) {
        return ((RubyFixnum *) h)->longValue();
    }

    JLocalEnv env;

    jsync(env);

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

static inline VALUE
newNumber(jmethodID method, long long v)
{
    JLocalEnv env;
    jvalue params[2];

    params[0].l = getRuntime();
    params[1].j = (jlong) v;

    jlong result = env->CallStaticLongMethodA(JRuby_class, method, params);
    checkExceptions(env);
    
    return (VALUE) result;
}


static VALUE
getCachedFixnum(int i)
{
    VALUE v = fixnumCache[i];
    if (v != 0) {
        return v;
    }

    fixnumCache[i] = v = newNumber(JRuby_ll2inum, i);
    Handle::valueOf(v)->flags |= FL_CONST;
    
    return v;
}

extern "C" VALUE
rb_ll2inum(long long v)
{
    if (v >= (long long) -CACHE_OFFSET && v < (long long) CACHE_OFFSET) {
        return getCachedFixnum((int) v);
    }

    return newNumber(JRuby_ll2inum, v);
}

extern "C" VALUE
rb_ull2inum(unsigned long long v)
{
    if (v < (unsigned long long) CACHE_OFFSET) {
        return getCachedFixnum((int) v);
    }

    return newNumber(JRuby_ull2inum, (long long) v);
}

extern "C" VALUE
rb_int2big(long long v)
{
    return newNumber(JRuby_int2big, v);
}

extern "C" VALUE
rb_uint2big(unsigned long long v)
{
    return newNumber(JRuby_uint2big, (long long) v);
}

