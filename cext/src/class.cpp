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
 * Copyright (C) 2008, 2009 Wayne Meissner
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
    JLocalEnv env;
    return (VALUE) env->CallStaticLongMethod(JRuby_class, JRuby_getMetaClass, valueToObject(env, obj));
}

extern "C" VALUE
rb_class_name(VALUE klass)
{
    return callMethodA(klass, "name", 0, NULL);
}

extern "C" char*
rb_class2name(VALUE class_handle)
{
    return rb_str_ptr_readonly(rb_class_name(class_handle));
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

extern "C" void 
rb_cv_set(VALUE klass, const char* name, VALUE value)
{
    rb_cvar_set(klass, rb_intern(name), value);
}

extern "C" VALUE 
rb_cv_get(VALUE klass, const char* name)
{
    return rb_cvar_get(klass, rb_intern(name));
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
            getCachedMethodID(env, NativeObjectAllocator_class, "<init>", "(J)V"),
            p2j((void *) fn));
    checkExceptions(env);

    env->CallVoidMethod(valueToObject(env, klass), RubyClass_setAllocator_method, allocator);
}

extern "C" VALUE 
rb_path_to_class(VALUE pathname)
{
    JLocalEnv env;
    jobject klass = env->CallObjectMethod(getRuntime(), Ruby_getClassFromPath_method, env->NewStringUTF(rb_str_ptr_readonly(pathname)));
    checkExceptions(env);

    return objectToValue(env, klass);
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

extern "C" void 
rb_define_class_variable(VALUE klass, const char* name, VALUE val)
{
    rb_cv_set(klass, name, val);
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
