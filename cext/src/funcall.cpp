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

#include <jni.h>
#include "JLocalEnv.h"
#include "Handle.h"
#include "jruby.h"
#include "ruby.h"

using namespace jruby;

extern "C" VALUE
rb_funcall(VALUE recv, ID meth, int argCount, ...)
{
    VALUE argv[argCount];
    va_list ap;
    va_start(ap, argCount);

    for (int i = 0; i < argCount; i++) {
        argv[i] = va_arg(ap, VALUE);
    }

    va_end(ap);

    JLocalEnv env;

    return callRubyMethodA(env, recv, idToObject(env, meth), argCount, argv);
}

extern "C" VALUE
rb_funcall2(VALUE recv, ID meth, int argCount, VALUE* args)
{
    JLocalEnv env;

    return callRubyMethodA(env, recv, idToObject(env, meth), argCount, args);
}

extern "C" VALUE
jruby_funcall2b(VALUE recv, ID meth, int argCount, VALUE* args, VALUE block)
{
    JLocalEnv env;
    return callRubyMethodB(env, recv, idToObject(env, meth), argCount, args, block);
}

extern "C" VALUE
rb_funcall3(VALUE recv, ID mid, int argc, const VALUE *argv)
{
    // FIXME: This is supposed to only call public methods
    return rb_funcall2(recv, mid, argc, (VALUE*)argv);
}

extern "C" VALUE
rb_call_super(int argc, const VALUE *argv)
{
    JLocalEnv env;

    jobjectArray argArray = env->NewObjectArray(argc, IRubyObject_class, NULL);
    checkExceptions(env);

    for (int i = 0; i < argc; i++) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, argv[i]));
        checkExceptions(env);
    }


    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_callSuperMethod, getRuntime(), argArray);
    checkExceptions(env);

    return (VALUE) result;
}

extern "C" VALUE
rb_obj_instance_eval(int argc, VALUE* argv, VALUE self)
{
    JLocalEnv env;

    jobjectArray argArray = env->NewObjectArray(argc, IRubyObject_class, NULL);
    checkExceptions(env);

    for (int i = 0; i < argc; i++) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, argv[i]));
        checkExceptions(env);
    }

    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_instanceEval, valueToObject(env, self), argArray);
    checkExceptions(env);

    return (VALUE) result;
}
