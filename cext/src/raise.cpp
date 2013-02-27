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
 * Copyright (C) 2008,2009 Wayne Meissner
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



static jthrowable
newRaiseException(JNIEnv* env, VALUE exc, const char* fmt, va_list ap)
{
    char message[1024];
    vsnprintf(message, sizeof(message) - 1,fmt, ap);

    return (jthrowable) env->NewObject(RaiseException_class, RaiseException_constructor,
            getRuntime(), valueToObject(env, exc), env->NewStringUTF(message), true);
}


extern "C" void
rb_raise(VALUE exc, const char *fmt, ...)
{
    JLocalEnv env;
    va_list ap;
    va_start(ap, fmt);
    jthrowable jException = newRaiseException(env, exc, fmt, ap);
    va_end(ap);
    checkExceptions(env);
    throw JavaException(env, jException);
}

extern "C" void
rb_fatal(const char *fmt, ...)
{
    JLocalEnv env;
    va_list ap;
    va_start(ap, fmt);
    jthrowable jException = newRaiseException(env, rb_eFatal, fmt, ap);
    va_end(ap);
    checkExceptions(env);
    throw JavaException(env, jException);
}

extern "C" void
rb_bug(const char *fmt, ...)
{
    JLocalEnv env;
    va_list ap;
    va_start(ap, fmt);
    jthrowable jException = newRaiseException(env, rb_eFatal, fmt, ap);
    va_end(ap);
    checkExceptions(env);
    throw JavaException(env, jException);
}
