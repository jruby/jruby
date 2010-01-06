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
#include "JUtil.h"


using namespace jruby;

static jobject getNotAllocatableAllocator(JNIEnv* env);
static jobject getDefaultAllocator(JNIEnv* env, VALUE parent);

extern "C" VALUE
rb_define_class(const char* name, VALUE parent)
{
    JLocalEnv env;
    
    jmethodID defineClass = getMethodID(env, Ruby_class, "defineClass",
            "(Ljava/lang/String;Lorg/jruby/RubyClass;Lorg/jruby/runtime/ObjectAllocator;)Lorg/jruby/RubyClass;");
    jobject result = env->CallObjectMethod(getRuntime(), defineClass,
            env->NewStringUTF(name), valueToObject(env, parent), getDefaultAllocator(env, parent));
    checkExceptions(env);
    
    return objectToValue(env, result);
}


extern "C" VALUE
rb_define_class_under(VALUE module, const char* name, VALUE parent)
{
    JLocalEnv env;

    jmethodID Ruby_defineClass_method = getMethodID(env, Ruby_class, "defineClassUnder",
            "(Ljava/lang/String;Lorg/jruby/RubyClass;Lorg/jruby/runtime/ObjectAllocator;Lorg/jruby/RubyClass;)Lorg/jruby/RubyClass;");

    jobject result = env->CallObjectMethod(getRuntime(), Ruby_defineClass_method,
            env->NewStringUTF(name), valueToObject(env, parent), getDefaultAllocator(env, parent),
            valueToObject(env, module));
    checkExceptions(env);

    return objectToValue(env, result);
}

extern "C" void
rb_define_alloc_func(VALUE klass, VALUE (*fn)(VALUE))
{
    JLocalEnv env;

    jobject allocator = env->NewObject(NativeObjectAllocator_class,
            getMethodID(env, NativeObjectAllocator_class, "<init>", "(J)V"),
            p2j((void *) fn));

    checkExceptions(env);

    jmethodID RubyClass_setAllocator_method = getMethodID(env, RubyClass_class,
            "setAllocator", "(Lorg/jruby/runtime/ObjectAllocator;)V");

    env->CallVoidMethod(valueToObject(env, klass), RubyClass_setAllocator_method, allocator);
}


static jobject
getNotAllocatableAllocator(JNIEnv* env)
{
    jfieldID NotAllocatableAllocator_field = env->GetStaticFieldID(ObjectAllocator_class, "NOT_ALLOCATABLE_ALLOCATOR",
            "Lorg/jruby/runtime/ObjectAllocator;");
    checkExceptions(env);
    jobject allocator = env->GetStaticObjectField(ObjectAllocator_class, NotAllocatableAllocator_field);
    checkExceptions(env);

    return allocator;
}

static jobject
getDefaultAllocator(JNIEnv* env, VALUE parent)
{
    jobject allocator = env->CallObjectMethod(valueToObject(env, parent),
            getMethodID(env, RubyClass_class, "getAllocator", "()Lorg/jruby/runtime/ObjectAllocator;"));
    checkExceptions(env);

    return allocator;
}