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
#include "Handle.h"

using namespace jruby;

extern "C" void
rb_define_attr(VALUE klass, const char* attr_name, int readable, int writable)
{
    VALUE rbName = ID2SYM(rb_intern(attr_name));

    if (readable) {
        callMethodA(klass, "attr_reader", 1, &rbName);
    }

    if (writable) {
        callMethodA(klass, "attr_writer", 1, &rbName);
    }
  }


extern "C" void
rb_define_const(VALUE module, const char* name, VALUE obj)
{
    JLocalEnv env;
    jmethodID mid = getCachedMethodID(env, RubyModule_class, "defineConstant",
            "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)V");
    env->CallVoidMethod(valueToObject(env, module), mid, env->NewStringUTF(name), valueToObject(env, obj));
    checkExceptions(env);
}

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

    jmethodID Ruby_defineModuleUnder_method = getCachedMethodID(env, Ruby_class, "defineModuleUnder",
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

    jmethodID JRuby_newMethod = getStaticMethodID(env, JRuby_class, "newMethod", "(Lorg/jruby/RubyModule;JI)Lorg/jruby/internal/runtime/methods/DynamicMethod;");
    jmethodID RubyModule_addMethod_method = getCachedMethodID(env, RubyModule_class, "addMethod",
            "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");

    jobject module = valueToObject(env, klass);
    env->CallVoidMethod(module, RubyModule_addMethod_method, env->NewStringUTF(meth),
            env->CallStaticObjectMethod(JRuby_class, JRuby_newMethod, module, (jlong)(intptr_t) fn, arity));
    checkExceptions(env);
}

extern "C" void
rb_define_private_method(VALUE klass, const char* meth, VALUE(*fn)(ANYARGS), int arity)
{
    rb_define_method(klass, meth, fn, arity);
    callMethod(klass, "private", 1, ID2SYM(rb_intern(meth)));
}

extern "C" void
rb_define_protected_method(VALUE klass, const char* meth, VALUE(*fn)(ANYARGS), int arity)
{
    rb_define_method(klass, meth, fn, arity);
    callMethod(klass, "protected", 1, ID2SYM(rb_intern(meth)));
}

extern "C" void
rb_define_module_function(VALUE klass,const char* meth, VALUE(*fn)(ANYARGS),int arity)
{
    JLocalEnv env;

    jmethodID JRuby_newMethod = getStaticMethodID(env, JRuby_class, "newMethod", "(Lorg/jruby/RubyModule;JI)Lorg/jruby/internal/runtime/methods/DynamicMethod;");

    jmethodID RubyModule_addModuleFunction_method = getCachedMethodID(env, RubyModule_class, "addModuleFunction",
            "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");

    jobject module = valueToObject(env, klass);

    env->CallVoidMethod(module, RubyModule_addModuleFunction_method, env->NewStringUTF(meth),
            env->CallStaticObjectMethod(JRuby_class, JRuby_newMethod, module, (jlong)(intptr_t) fn, arity));
    checkExceptions(env);

    if (TYPE(klass) == T_MODULE) {
        callMethod(klass, "module_function", 1, ID2SYM(rb_intern(meth)));
    }
}

extern "C" void
rb_define_singleton_method(VALUE object, const char* meth, VALUE(*fn)(ANYARGS), int arity)
{
    JLocalEnv env;

    jmethodID IRubyObject_getSingletonClass_method = getCachedMethodID(env, IRubyObject_class, "getSingletonClass",
            "()Lorg/jruby/RubyClass;");
    jobject singleton = env->CallObjectMethod(valueToObject(env, object), IRubyObject_getSingletonClass_method);

    jmethodID JRuby_newMethod = getStaticMethodID(env, JRuby_class, "newMethod",
            "(Lorg/jruby/RubyModule;JI)Lorg/jruby/internal/runtime/methods/DynamicMethod;");
    jmethodID RubyModule_addMethod_method = getCachedMethodID(env, RubyModule_class, "addMethod",
            "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");

    env->CallVoidMethod(singleton, RubyModule_addMethod_method, env->NewStringUTF(meth),
            env->CallStaticObjectMethod(JRuby_class, JRuby_newMethod, singleton, (jlong)(intptr_t) fn, arity));
    checkExceptions(env);
}

extern "C" void
rb_undef_method(VALUE klass, const char* method)
{
    JLocalEnv env;

    jobject ctxt = env->CallObjectMethod(getRuntime(), Ruby_getCurrentContext_method);
    checkExceptions(env);
    env->CallObjectMethod(valueToObject(env, klass), RubyModule_undef_method, ctxt, env->NewStringUTF(method));
    checkExceptions(env);
}

extern "C" void
rb_undef(VALUE klass, ID method)
{
    rb_undef_method(klass, rb_id2name(method));
}

extern "C" int
rb_const_defined(VALUE module, ID symbol)
{
    return RTEST(rb_const_defined_at(module, symbol))
            || RTEST(callMethod(rb_cObject, "const_defined?", 1, ID2SYM(symbol)));
}

extern "C" int
rb_const_defined_at(VALUE module, ID symbol)
{
    return RTEST(callMethod(module, "const_defined?", 1, ID2SYM(symbol)));
}

extern "C" VALUE
rb_const_get(VALUE module, ID symbol)
{
    JLocalEnv env;
    jmethodID mid = getCachedMethodID(env, RubyModule_class, "getConstant",
            "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
    jobject c = env->CallObjectMethod(valueToObject(env, module), mid, idToString(env, symbol));
    checkExceptions(env);

    return objectToValue(env, c);
}

extern "C" VALUE
rb_const_get_at(VALUE module, ID symbol)
{
    JLocalEnv env;
    jmethodID mid = getCachedMethodID(env, RubyModule_class, "getConstantAt",
            "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
    jobject c = env->CallObjectMethod(valueToObject(env, module), mid, idToString(env, symbol));
    checkExceptions(env);

    // Check for null return and call const_missing, if neccessary
    return env->IsSameObject(c, NULL) ? callMethod(module, "const_missing", 1, ID2SYM(symbol)) : objectToValue(env, c);
}

extern "C" VALUE
rb_const_get_from(VALUE module, ID symbol)
{
    JLocalEnv env;
    jmethodID mid = getCachedMethodID(env, RubyModule_class, "getConstantFrom",
            "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
    jobject c = env->CallObjectMethod(valueToObject(env, module), mid, idToString(env, symbol));
    checkExceptions(env);

    return objectToValue(env, c);
}

extern "C" void
rb_const_set(VALUE parent, ID name, VALUE object)
{
    JLocalEnv env;
    jmethodID mid = getCachedMethodID(env, RubyModule_class, "setConstant",
            "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
    env->CallObjectMethod(valueToObject(env, parent), mid, idToString(env, name),
            valueToObject(env, object));
    checkExceptions(env);
}

extern "C" void
rb_define_global_function(const char* name, VALUE(*fn)(ANYARGS), int arity)
{
    rb_define_module_function(rb_mKernel, name, fn, arity);
}

extern "C" void
rb_undef_alloc_func(VALUE klass) 
{
  rb_undef_method(rb_singleton_class(klass), "allocate");
}

extern "C" void
rb_alias(VALUE module, ID id_new, ID id_old)
{
    callMethod(module, "alias_method", 2, ID2SYM(id_new), ID2SYM(id_old));
}
