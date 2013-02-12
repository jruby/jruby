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
 * Copyright (C) 1993-2007 Yukihiro Matsumoto
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
#include "JavaException.h"

using namespace jruby;

extern "C" VALUE
rb_exc_new(VALUE etype, const char *ptr, long len)
{
    return rb_exc_new3(etype, rb_str_new(ptr, len));
}

#undef rb_exc_new2

extern "C" VALUE
rb_exc_new2(VALUE etype, const char *s)
{
    return rb_exc_new(etype, s, strlen(s));
}

extern "C" VALUE
rb_exc_new3(VALUE etype, VALUE str)
{
    StringValue(str);
    return callMethod(etype, "new", 1, str);
}

extern "C" void
rb_exc_raise(VALUE exc)
{
    using namespace jruby;
    JLocalEnv env;

    jmethodID ctor = getCachedMethodID(env, RaiseException_class, "<init>", "(Lorg/jruby/RubyException;)V");
    jthrowable jException = (jthrowable) env->NewObject(RaiseException_class, ctor, valueToObject(env, exc));
    checkExceptions(env);

    throw JavaException(env, jException);
}


extern "C" VALUE
rb_ensure(VALUE (*b_proc)(ANYARGS), VALUE data1, VALUE (*e_proc)(ANYARGS), VALUE data2)
{
    bool b_returned = false;
    VALUE result;
    try {
        result = (*b_proc)(data1);
        b_returned = true;
        (*e_proc)(data2);

        return result;

    } catch (jruby::JavaException& ex) {
        if (!b_returned) {
            (*e_proc)(data2);
        }

        throw;
    }
}


extern "C" VALUE
rb_rescue(VALUE (*b_proc)(ANYARGS), VALUE data1, VALUE (*r_proc)(ANYARGS), VALUE data2)
{
    try {
        return (*b_proc)(data1);

    } catch (jruby::JavaException& ex) {
        JLocalEnv env;

        jthrowable t = ex.getCause(env);
        if (!env->IsInstanceOf(t, RaiseException_class)) {
            // Not a ruby exception, just propagate
            throw;
        }

        jobject rubyException = env->GetObjectField(t, RaiseException_exception_field);
        checkExceptions(env);

        VALUE exc = objectToValue(env, rubyException);
        if (rb_obj_is_kind_of(exc, rb_eStandardError)) {

            VALUE result = (*r_proc)(data2);
            env->CallStaticVoidMethod(JRuby_class, JRuby_clearErrorInfo, jruby::getRuntime());

            return result;
        }

        rb_raise(rb_eTypeError, "wrong exception type raised (expected StandardError subclass)");

        return Qnil;
    }
}

extern "C" VALUE
rb_rescue2(VALUE (*b_proc)(ANYARGS), VALUE data1, VALUE (*r_proc)(ANYARGS), VALUE data2, ...)
{
    try {
        return (*b_proc)(data1);

    } catch (jruby::JavaException& ex) {
        JLocalEnv env;

        jthrowable t = ex.getCause(env);
        if (!env->IsInstanceOf(t, RaiseException_class)) {
            // Not a ruby exception, just propagate
            throw;
        }

        jobject rubyException = env->GetObjectField(t, RaiseException_exception_field);
        checkExceptions(env);

        VALUE exc = objectToValue(env, rubyException);

        va_list ap;
        va_start(ap, data2);
        VALUE eclass = 0;

        bool handle = false;
        while ((eclass = va_arg(ap, VALUE)) != 0) {
            if (rb_obj_is_kind_of(exc, eclass)) {
                handle = true;
                break;
            }
        }
        va_end(ap);

        if (handle) {

            VALUE result = (*r_proc)(data2);
            env->CallStaticVoidMethod(JRuby_class, JRuby_clearErrorInfo, jruby::getRuntime());

            return result;
        }

        rb_raise(rb_eTypeError, "wrong exception type raised");

        return Qnil;
    }
}

extern "C" VALUE
rb_protect(VALUE (*func)(VALUE), VALUE data, int* status)
{
    bool returned = false;
    VALUE result;
    try {
        result = (*func)(data);
        returned = true;
        *status = 0;
        return result;
    } catch (jruby::JavaException& ex) {
        *status = 1;
        return Qnil;
    }
}

extern "C" void
rb_jump_tag(int status) {
    if (status) {
        // TODO: Check if there is an exception here?
        rb_exc_raise(rb_gv_get("$!"));
    }
}
