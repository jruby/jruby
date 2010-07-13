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
rb_class_new_instance(int argc, VALUE* argv, VALUE klass)
{
    return callMethodA(klass, "new", argc, argv);
}

extern "C" VALUE
rb_class_of(VALUE obj)
{
    return callMethodA(obj, "class", 0, NULL);
}

extern "C" VALUE
rb_class_name(VALUE klass)
{
    return callMethodA(klass, "name", 0, NULL);
}

extern "C" char* 
rb_class2name(VALUE class_handle)
{
    return (char *) RSTRING_PTR(rb_class_name(class_handle));
}

extern "C" VALUE
rb_cvar_defined(VALUE klass, ID name)
{
    return callMethod(klass, "class_variable_defined?", 1, ID2SYM(name));
}

extern "C" VALUE
rb_cvar_get(VALUE klass, ID name)
{
    return callMethod(klass, "class_variable_get", 1, ID2SYM(name));
}

extern "C" VALUE
rb_cvar_set(VALUE klass, ID name, VALUE value, int unused)
{
    return callMethod(klass, "class_variable_set", 2, ID2SYM(name), value);
}

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
            "(Ljava/lang/String;Lorg/jruby/RubyClass;Lorg/jruby/runtime/ObjectAllocator;Lorg/jruby/RubyModule;)Lorg/jruby/RubyClass;");

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

extern "C" VALUE
rb_path2class(const char* path) {
    JLocalEnv env;
    jmethodID mid = getMethodID(env, Ruby_class, "getClassFromPath", "(Ljava/lang/String;)Lorg/jruby/RubyModule;");
    jobject klass = env->CallObjectMethod(getRuntime(), mid, env->NewStringUTF(path));
    checkExceptions(env);
    return objectToValue(env, klass);
}

extern "C" VALUE
rb_obj_alloc(VALUE klass) {
    JLocalEnv env;
    jobject allocator = getDefaultAllocator(env, klass);
    jmethodID mid = getMethodID(env, ObjectAllocator_class, "allocate",
            "(Lorg/jruby/Ruby;Lorg/jruby/RubyClass;)Lorg/jruby/runtime/builtin/IRubyObject;");
    jobject instance = env->CallObjectMethod(allocator, mid, getRuntime(), valueToObject(env, klass));
    checkExceptions(env);
    return objectToValue(env, instance);
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