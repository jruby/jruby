/*
 * Copyright (C) 2010 Tim Felgentreff
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

#include "ruby.h"
#include "jruby.h"
#include "JLocalEnv.h"

using namespace jruby;

extern "C" VALUE
rb_struct_define(const char* name_cstr, ...)
{
    JLocalEnv env;
    va_list varargs;
    int capa = 4;
    int argc = 0;
    char** args = (char**)malloc(sizeof(char) * capa);

    va_start(varargs, name_cstr);
    while (args[argc] = va_arg(varargs, char*)) {
        argc++;
        if (argc >= capa) {
            args = (char**)realloc(args, sizeof(char) * capa * 2);
            capa = capa * 2;
        }
    }
    va_end(varargs);

    jobjectArray argArray = env->NewObjectArray(argc + 1, IRubyObject_class, NULL);
    checkExceptions(env);

    if (!name_cstr) {
        env->SetObjectArrayElement(argArray, 0, valueToObject(env, Qnil));
    } else {
        env->SetObjectArrayElement(argArray, 0, valueToObject(env, rb_str_new_cstr(name_cstr)));
    }
    checkExceptions(env);

    for (int i = 0; i < argc; i++) {
        env->SetObjectArrayElement(argArray, i + 1, valueToObject(env, rb_str_new_cstr(args[i])));
        checkExceptions(env);
    }

    jmethodID mid = getMethodID(env, Ruby_class, "getStructClass", "()Lorg/jruby/RubyClass;");
    jobject structClass = env->CallObjectMethod(getRuntime(), mid);

    jobject newStructSubclass = env->CallStaticObjectMethod(RubyStruct_class, RubyStruct_newInstance,
            structClass, argArray, getNullBlock());
    checkExceptions(env);

    return objectToValue(env, newStructSubclass);
}
