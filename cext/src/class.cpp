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

static void class_variable_prefix(char* target, ID source);
static jobject getNotAllocatableAllocator(JNIEnv* env);
static jobject getDefaultAllocator(JNIEnv* env, VALUE parent);

extern "C" VALUE
rb_class_new(VALUE klass)
{
    JLocalEnv env;
    jobject jklass = env->CallStaticObjectMethod(RubyClass_class, RubyClass_newClass_method,
            getRuntime(), valueToObject(env, klass));
    checkExceptions(env);
    return objectToValue(env, jklass);
}

extern "C" VALUE
rb_class_inherited(VALUE super, VALUE klass)
{
    return callMethod(super ? super : rb_cObject, "inherited", 1, klass);
}

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
    if (rb_is_instance_id(name)) {
        // Class Instance variable
        return rb_ivar_defined(klass, name);
    }
    char target[strlen(rb_id2name(name)) + 3];
    class_variable_prefix(target, name);
    return callMethod(klass, "class_variable_defined?", 1, rb_str_new_cstr(target));
}

extern "C" VALUE
rb_cvar_get(VALUE klass, ID name)
{
    if (rb_is_instance_id(name)) {
        // Class Instance variable
        return rb_ivar_get(klass, name);
    }
    char target[strlen(rb_id2name(name)) + 3];
    class_variable_prefix(target, name);
    return callMethod(klass, "class_variable_get", 1, rb_str_new_cstr(target));
}

#undef rb_cvar_set

extern "C" VALUE
rb_cvar_set(VALUE klass, ID name, VALUE value)
{
    if (rb_is_instance_id(name)) {
        // Class Instance variable
        return rb_ivar_set(klass, name, value);
    }
    char target[strlen(rb_id2name(name)) + 3];
    class_variable_prefix(target, name);
    return callMethod(klass, "class_variable_set", 2, rb_str_new_cstr(target), value);
}

extern "C" VALUE
rb_define_class(const char* name, VALUE parent)
{
    JLocalEnv env;
    VALUE super = parent ? parent : rb_cObject;

    jobject result = env->CallObjectMethod(getRuntime(), Ruby_defineClass_method,
            env->NewStringUTF(name), valueToObject(env, super), getDefaultAllocator(env, super));
    checkExceptions(env);

    return objectToValue(env, result);
}

extern "C" VALUE
rb_define_class_under(VALUE module, const char* name, VALUE parent)
{
    JLocalEnv env;
    VALUE super = parent ? parent : rb_cObject;

    jobject result = env->CallObjectMethod(getRuntime(), Ruby_defineClassUnder_method,
            env->NewStringUTF(name), valueToObject(env, super), getDefaultAllocator(env, super),
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

    env->CallVoidMethod(valueToObject(env, klass), RubyClass_setAllocator_method, allocator);
}

extern "C" VALUE
rb_path2class(const char* path)
{
    JLocalEnv env;
    jobject klass = env->CallObjectMethod(getRuntime(), Ruby_getClassFromPath_method, env->NewStringUTF(path));
    checkExceptions(env);

    return objectToValue(env, klass);
}

extern "C" VALUE
rb_obj_alloc(VALUE klass)
{
    JLocalEnv env;
    jobject allocator = getDefaultAllocator(env, klass);
    jobject instance = env->CallObjectMethod(allocator, ObjectAllocator_allocate_method,
            getRuntime(), valueToObject(env, klass));
    checkExceptions(env);

    return objectToValue(env, instance);
}

extern "C" void
rb_include_module(VALUE self, VALUE module)
{
    callMethod(self, "include", 1, module);
}

static void
class_variable_prefix(char* target, ID source)
{
    int i = 0;
    if (!rb_is_class_id(source)) {
        target[i++] = '@';
        target[i++] = '@';
    }
    target[i] = '\0';
    strcat(target, rb_id2name(source));
}

static jobject
getNotAllocatableAllocator(JNIEnv* env)
{
    jobject allocator = env->GetStaticObjectField(ObjectAllocator_class, ObjectAllocator_NotAllocatableAllocator_field);
    checkExceptions(env);

    return allocator;
}

static jobject
getDefaultAllocator(JNIEnv* env, VALUE parent)
{
    jobject allocator = env->CallObjectMethod(valueToObject(env, parent), RubyClass_getAllocator_method);
    checkExceptions(env);

    return allocator;
}
