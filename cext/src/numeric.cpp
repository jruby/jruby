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

#include "jruby.h"
#include "JUtil.h"
#include "ruby.h"
#include "Handle.h"
#include "JLocalEnv.h"

using namespace jruby;

#define CACHE_OFFSET (128L)
static jobject fixnumCache[2 * CACHE_OFFSET];
static inline RubyFixnum* newNumber(jmethodID method, long long v);


extern "C" int
rb_num2int(VALUE v)
{
    return likely(FIXNUM_P(v)) ? FIX2INT(v) : (int) rb_num2ll(v);
}

extern "C" unsigned int
rb_num2uint(VALUE v)
{
    return likely(FIXNUM_P(v)) ? FIX2UINT(v) : (unsigned int) rb_num2ull(v);
}

extern "C" long
rb_num2long(VALUE v)
{
    return likely(FIXNUM_P(v)) ? FIX2LONG(v) : (long) rb_num2ll(v);
}

extern "C" unsigned long
rb_num2ulong(VALUE v)
{
    return likely(FIXNUM_P(v)) ? FIX2ULONG(v) : (unsigned long) rb_num2ull(v);
}

extern "C" long
rb_fix2int(VALUE v)
{
    return FIX2LONG(v);
}

extern "C" unsigned long
rb_fix2uint(VALUE v)
{
    return FIX2ULONG(v);
}

extern "C" long long
rb_num2ll(VALUE v)
{
    if (FIXNUM_P(v)) {
        return FIX2LONG(v);
    }

    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_FIXNUM) {
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

extern "C" char
rb_num2chr(VALUE v)
{
    JLocalEnv env;
    jbyte result = env->CallStaticByteMethod(RubyNumeric_class, RubyNumeric_num2chr_method, valueToObject(env, v));
    checkExceptions(env);
    return (char) result;
}


extern "C" VALUE
rb_int2inum(long v)
{
    return FIXABLE(v) ? INT2FIX(v) : rb_ll2inum(v);
}

extern "C" VALUE
rb_uint2inum(unsigned long v)
{
    return POSFIXABLE(v) ? UINT2FIX(v) : rb_ull2inum(v);
}

extern "C" VALUE
rb_Integer(VALUE val)
{
    return rb_to_integer(val, "to_i");
}

extern "C" VALUE
rb_to_int(VALUE val)
{
    return rb_to_integer(val, "to_int");
}

static inline RubyFixnum*
newNumber(jmethodID method, long long v)
{
    JLocalEnv env;
    jvalue params[2];

    params[0].l = getRuntime();
    params[1].j = (jlong) v;

    jlong result = env->CallStaticLongMethodA(JRuby_class, method, params);
    checkExceptions(env);

    return (RubyFixnum *) j2p(result);
}


jobject
jruby::fixnumToObject(JNIEnv* env, VALUE v)
{
    SIGNED_VALUE i  = RSHIFT((SIGNED_VALUE) v, 1);
    jobject obj;

    if (likely(i >= -CACHE_OFFSET && i < CACHE_OFFSET && fixnumCache[i + CACHE_OFFSET] != NULL)) {
        return fixnumCache[i + CACHE_OFFSET];
    }

    jvalue params[2];

    params[0].l = getRuntime();
    params[1].j = i;

    obj = env->CallStaticObjectMethodA(RubyNumeric_class, RubyNumeric_int2fix_method, params);

    if (unlikely(i >= -CACHE_OFFSET && i < CACHE_OFFSET)) {
        fixnumCache[i + CACHE_OFFSET] = env->NewGlobalRef(obj);
    }

    return obj;
}

extern "C" VALUE
rb_ll2inum(long long v)
{
    if (v < FIXNUM_MAX && v >= FIXNUM_MIN) {
        return ((VALUE)(((SIGNED_VALUE)(v))<<1 | FIXNUM_FLAG));
    }

    return newNumber(JRuby_ll2inum, v)->asValue();
}

extern "C" VALUE
rb_ull2inum(unsigned long long v)
{
    if (v < (unsigned long long) FIXNUM_MAX) {
        return ((VALUE)(((SIGNED_VALUE)(v))<<1 | FIXNUM_FLAG));
    }

    return newNumber(JRuby_ull2inum, (long long) v)->asValue();
}

extern "C" VALUE
rb_int2big(long long v)
{
    return newNumber(JRuby_int2big, v)->asValue();
}

extern "C" VALUE
rb_uint2big(unsigned long long v)
{
    return newNumber(JRuby_uint2big, (long long) v)->asValue();
}

extern "C" int
rb_cmpint(VALUE val, VALUE a, VALUE b)
{
    if (NIL_P(val)) rb_cmperr(a, b);
    if (FIXNUM_P(val)) return FIX2INT(val);
    if (TYPE(val) == T_BIGNUM) {
        if (RBIGNUM_SIGN(val)) return 1;
        return -1;
    }

    if (RTEST(rb_funcall(val, rb_intern(">"), 1, INT2FIX(0)))) return 1;
    if (RTEST(rb_funcall(val, rb_intern("<"), 1, INT2FIX(0)))) return -1;
    return 0;
}

extern "C" void
rb_cmperr(VALUE x, VALUE y)
{
    const char *classname;

    if (SPECIAL_CONST_P(y)) {
        y = rb_inspect(y);
        classname = StringValuePtr(y);
    } else {
        classname = rb_obj_classname(y);
    }

    rb_raise(rb_eArgError, "comparison of %s with %s failed",
             rb_obj_classname(x), classname);
}

extern "C" VALUE
jruby_timet2num(time_t v)
{
    if ((time_t)-1 < 0) {
        if (sizeof(int) == sizeof(time_t)) {
            return INT2NUM(v);
        }
        else if (sizeof(long) == sizeof(time_t)) {
            return LONG2NUM(v);
        }
        else if (sizeof(LONG_LONG) == sizeof(time_t)) {
            return LL2NUM(v);
        }
    }
    else {
        if (sizeof(int) == sizeof(time_t)) {
            return UINT2NUM(v);
        }
        else if (sizeof(long) == sizeof(time_t)) {
            return ULONG2NUM(v);
        }
        else if (sizeof(LONG_LONG) == sizeof(time_t)) {
            return ULL2NUM(v);
        }
    }
}


extern "C" void
rb_num_zerodiv(void)
{
    rb_raise(rb_eZeroDivError, "divided by 0");
}
