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
 * Copyright (C) 2010 Tim Felgentreff
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
#include "ruby.h"
#include "JLocalEnv.h"
#include <math.h>

using namespace jruby;

extern "C" int
jruby_big_bytes_used(VALUE obj)
{
    return NUM2INT(callMethod(obj, "size", 0));
}

extern "C" VALUE
jruby_big_sign(VALUE obj)
{
    return callMethod(obj, ">", 1, INT2NUM(0));
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

    if (unlikely(isnan(result))) {
	return (signbit(result)) ? -INFINITY : INFINITY;
    } else {
	return result;
    }
}

extern "C" VALUE
rb_big2str(VALUE obj, int radix)
{
    return callMethod(obj, "to_s", 1, INT2FIX(radix));
}
