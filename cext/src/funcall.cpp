/*
 * Copyright (C) 2008-2010 Wayne Meissner
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
rb_obj_instance_eval(int argc, VALUE* argv, VALUE self) {
    JLocalEnv env;

    jobjectArray argArray = env->NewObjectArray(argc, IRubyObject_class, NULL);
    checkExceptions(env);

    for (int i = 0; i < argc; i++) {
        env->SetObjectArrayElement(argArray, i, valueToObject(env, argv[i]));
        checkExceptions(env);
    }

    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_instanceEval, self, argArray);
    checkExceptions(env);

    return (VALUE) result;
}

