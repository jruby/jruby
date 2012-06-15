/*
 * Copyright (C) 1993-2003 Yukihiro Matsumoto
 * Copyright (C) 2000 Network Applied Communication Laboratory, Inc.
 * Copyright (C) 2000 Information-technology Promotion Agency, Japan
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

#include <stdio.h>
#include <stdlib.h>
#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"
#include "Handle.h"

using namespace jruby;

extern "C" bool
jruby_obj_frozen(VALUE obj)
{
    return callMethod(obj, "frozen?", 0) == Qtrue;
}

static VALUE
convert_type(VALUE val, const char* type_name, const char* method, int raise)
{
    ID m = rb_intern(method);
    if (!rb_respond_to(val, m)) {
        if (raise) {
            rb_raise(rb_eTypeError, "can't convert %s into %s",
                    NIL_P(val) ? "nil" :
                    val == Qtrue ? "true" :
                    val == Qfalse ? "false" :
                    rb_obj_classname(val),
                    type_name);
        } else {
            return Qnil;
        }
    }
    return rb_funcall(val, m, 0);
}

extern "C" VALUE
rb_obj_freeze(VALUE obj)
{
    return callMethodA(obj, "freeze", 0, NULL);
}

extern "C" char*
rb_obj_classname(VALUE obj)
{
    return rb_class2name(rb_class_of(obj));
}

extern "C" void
rb_extend_object(VALUE object, VALUE module)
{
    callMethod(object, "extend", 1, module);
}

extern "C" int
rb_respond_to(VALUE obj, ID id)
{
    JLocalEnv env;
    jboolean ret = env->CallBooleanMethod(valueToObject(env, obj), IRubyObject_respondsTo_method, idToString(env, id));
    checkExceptions(env);
    return ret != JNI_FALSE;
}

extern "C" int
rb_obj_respond_to(VALUE obj, ID id, int include_private)
{
    return RTEST(callMethod(obj, "respond_to?", 2, ID2SYM(id), include_private ? Qtrue : Qfalse));
}

extern "C" VALUE
rb_convert_type(VALUE val, int type, const char* type_name, const char* method)
{
    VALUE v;

    if (TYPE(val) == type) {
        return val;
    }

    v = convert_type(val, type_name, method, Qtrue);

    if (TYPE(v) != type) {
        rb_raise(rb_eTypeError, "%s#%s should return %s",
            rb_obj_classname(val), method, type_name);
    }

    return v;
}

extern "C" VALUE
rb_check_convert_type(VALUE val, int type, const char* type_name, const char* method)
{
    if (TYPE(val) == type) {
        return val;
    }

    return convert_type(val, type_name, method, 0);
}

extern "C" VALUE
rb_check_to_integer(VALUE object_handle, const char *method_name)
{
    if(FIXNUM_P(object_handle)) {
        return object_handle;
    }
    VALUE result = rb_check_convert_type(object_handle, 0, "Integer", method_name);
    if(rb_obj_is_kind_of(result, rb_cInteger)) {
        return result;
    }
    return Qnil;
}

extern "C" VALUE
rb_iv_get(VALUE obj, const char* name)
{
    JLocalEnv env;

    char var_name[strlen(name) + 2];
    (name[0] != '@') ? strcpy(var_name, "@")[0] : var_name[0] = '\0';
    strcat(var_name, name);

    jobject retval = env->CallObjectMethod(valueToObject(env, obj), RubyBasicObject_getInstanceVariable_method,
            env->NewStringUTF(var_name));
    checkExceptions(env);

    return objectToValue(env, retval);
}

extern "C" VALUE
rb_iv_set(VALUE obj, const char* name, VALUE value)
{
    JLocalEnv env;

    char var_name[strlen(name) + 2];
    (name[0] != '@') ? strcpy(var_name, "@")[0] : var_name[0] = '\0';
    strcat(var_name, name);

    jobject retval = env->CallObjectMethod(valueToObject(env, obj), RubyBasicObject_setInstanceVariable_method,
            env->NewStringUTF(var_name), valueToObject(env, value));
    checkExceptions(env);
    return objectToValue(env, retval);
}

extern "C" VALUE
rb_ivar_get(VALUE obj, ID ivar_name)
{
    return rb_iv_get(obj, rb_id2name(ivar_name));
}

extern "C" VALUE
rb_ivar_set(VALUE obj, ID ivar_name, VALUE value)
{
    return rb_iv_set(obj, rb_id2name(ivar_name), value);
}

extern "C" VALUE
rb_ivar_defined(VALUE obj, ID ivar)
{
    JLocalEnv env;
    const char* name = rb_id2name(ivar);

    char var_name[strlen(name) + 2];
    (name[0] != '@') ? strcpy(var_name, "@")[0] : var_name[0] = '\0';
    strcat(var_name, name);

    jboolean retval = env->CallBooleanMethod(valueToObject(env, obj), RubyBasicObject_hasInstanceVariable_method,
            env->NewStringUTF(var_name));
    checkExceptions(env);

    return (retval == JNI_TRUE) ? Qtrue : Qfalse;
}

extern "C" void
rb_obj_call_init(VALUE recv, int arg_count, VALUE* args)
{
    callMethodANonConst(recv, "initialize", arg_count, args);
}

extern "C" VALUE
rb_obj_is_kind_of(VALUE obj, VALUE module)
{
    return callMethod(obj, "kind_of?", 1, module);
}

extern "C" VALUE
rb_obj_is_instance_of(VALUE obj, VALUE klass)
{
    return callMethodA(obj, "instance_of?", 1, &klass);
}

extern "C" VALUE
rb_obj_taint(VALUE obj)
{
    return callMethodA(obj, "taint", 0, NULL);
}

extern "C" VALUE
rb_obj_tainted(VALUE obj)
{
    return callMethodA(obj, "tainted?", 0, NULL);
}

extern "C" VALUE
rb_attr_get(VALUE obj, ID id)
{
    return callMethod(obj, "instance_variable_get", 1, ID2SYM(id));
}

extern "C" VALUE
rb_obj_as_string(VALUE obj)
{
    VALUE str;

    if (TYPE(obj) == T_STRING) {
        return obj;
    }

    str = callMethodA(obj, "to_s", 0, NULL);
    if (TYPE(str) != T_STRING) {
        return rb_any_to_s(obj);
    }

    if (OBJ_TAINTED(obj)) {
        OBJ_TAINT(str);
    }

    return str;
}

extern "C" VALUE
rb_to_integer(VALUE val, const char *method)
{
    VALUE v;

    if (FIXNUM_P(val)) return val;
    if (TYPE(val) == T_BIGNUM) return val;
    v = convert_type(val, "Integer", method, 1);
    if (!rb_obj_is_kind_of(v, rb_cInteger)) {
        const char *cname = rb_obj_classname(val);
        rb_raise(rb_eTypeError, "can't convert %s to Integer (%s#%s gives %s)",
                 cname, cname, method, rb_obj_classname(v));
    }
    return v;
}

extern "C" VALUE
rb_inspect(VALUE obj)
{
    return rb_obj_as_string(callMethodA(obj, "inspect", 0, 0));
}

extern "C" VALUE
rb_any_to_s(VALUE obj)
{
    char* buf;
    int len = 128, buflen;

    do {
	buf = (char *) alloca(buflen = len);
        len = snprintf(buf, buflen, "#<%s:%p>", rb_obj_classname(obj), (void *) obj);
    } while (len >= buflen);

    return rb_str_new_cstr(buf);
}

extern "C" void
rb_check_frozen(VALUE obj)
{
    if (OBJ_FROZEN(obj)) {
        rb_raise(is1_9() ? rb_eRuntimeError : rb_eTypeError, "can't modify frozen %s", rb_obj_classname(obj));
    }
}

extern "C" VALUE
rb_singleton_class(VALUE obj)
{
    JLocalEnv env;

    jmethodID IRubyObject_getSingletonClass_method = getCachedMethodID(env, IRubyObject_class, "getSingletonClass",
            "()Lorg/jruby/RubyClass;");
    jobject singleton = env->CallObjectMethod(valueToObject(env, obj), IRubyObject_getSingletonClass_method);
    checkExceptions(env);

    return objectToValue(env, singleton);
}

extern "C" VALUE
rb_class_inherited_p(VALUE mod, VALUE arg)
{
    if (TYPE(arg) != T_MODULE && TYPE(arg) != T_CLASS) {
        rb_raise(rb_eTypeError, "compared with non class/module");
    }

    return callMethodA(mod, "<=", 1, &arg);
}

extern "C" VALUE 
rb_class_superclass(VALUE klass)
{
    return callMethodA(klass, "superclass", 0, NULL);
}

extern "C" VALUE
rb_obj_dup(VALUE obj)
{
    return callMethod(obj, "dup", 0);
}

extern "C" VALUE
rb_obj_clone(VALUE obj)
{
    return callMethod(obj, "clone", 0);
}

extern "C" VALUE
rb_obj_id(VALUE obj)
{
    return callMethod(obj, "object_id", 0);
}

extern "C" VALUE
rb_equal(VALUE obj, VALUE other)
{
    return RTEST(callMethod(obj, "==", 1, other)) ? Qtrue : Qfalse;
}


extern "C" int
rb_eql(VALUE obj1, VALUE obj2)
{
    return RTEST(callMethodA(obj1, "eql?", 1, &obj2));
}

extern "C" void
jruby_infect(VALUE object1, VALUE object2)
{
    if (OBJ_TAINTED(object1)) {
        JLocalEnv env;
        jmethodID mid = getCachedMethodID(env, IRubyObject_class, "infectBy",
            "(Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        env->CallObjectMethod(valueToObject(env, object2), mid, object1);
        checkExceptions(env);
    }
}

extern "C" VALUE
rb_hash(VALUE obj)
{
    VALUE hash = callMethod(obj, "hash", 0);
    return convert_type(hash, "Fixnum", "to_int", true);
}
