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

extern "C" VALUE
rb_define_module(const char* name)
{
    JLocalEnv env;
    jobject mod = env->CallObjectMethod(getRuntime(), Ruby_defineModule_method, env->NewStringUTF(name));
    checkExceptions(env);
    return objectToValue(env, mod);
}


extern "C" VALUE
rb_define_module_under(VALUE module, const char* name)
{
    JLocalEnv env;
    
    jmethodID Ruby_defineModuleUnder_method = getMethodID(env, Ruby_class, "defineModuleUnder",
            "(Ljava/lang/String;Lorg/jruby/RubyModule;)Lorg/jruby/RubyModule;");
    jobject mod = env->CallObjectMethod(getRuntime(), Ruby_defineModuleUnder_method,
            env->NewStringUTF(name), valueToObject(env, module));
    checkExceptions(env);

    return objectToValue(env, mod);
    
}

extern "C" void
rb_define_method(VALUE klass, const char* meth, VALUE(*fn)(ANYARGS), int arity)
{
    JLocalEnv env;
    
    jmethodID constructor = getMethodID(env, NativeMethod_class, "<init>", "(Lorg/jruby/RubyModule;IJ)V");
    jmethodID RubyModule_addMethod_method = getMethodID(env, RubyModule_class, "addMethod",
            "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");

    jobject module = valueToObject(env, klass);
    jobject method = env->NewObject(NativeMethod_class, constructor, module, arity, fn);
    checkExceptions(env);

    env->CallVoidMethod(module, RubyModule_addMethod_method, env->NewStringUTF(meth), method);
    checkExceptions(env);
}

extern "C" void
rb_define_module_function(VALUE klass,const char* meth, VALUE(*fn)(ANYARGS),int arity)
{
    JLocalEnv env;

    jmethodID constructor = getMethodID(env, NativeMethod_class, "<init>", "(Lorg/jruby/RubyModule;IJ)V");

    jmethodID RubyModule_addModuleFunction_method = getMethodID(env, RubyModule_class, "addModuleFunction",
            "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");

    jobject module = valueToObject(env, klass);
    jobject method = env->NewObject(NativeMethod_class, constructor, module, arity, fn);
    checkExceptions(env);

    env->CallVoidMethod(module, RubyModule_addModuleFunction_method, env->NewStringUTF(meth), method);
    checkExceptions(env);
}

#ifdef notyet
void rb_define_global_function(const char*,VALUE(*)(ANYARGS),int);
#endif