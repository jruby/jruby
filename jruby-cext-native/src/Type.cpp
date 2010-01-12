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

#include "Handle.h"
#include "jruby.h"
#include "ruby.h"
#include "org_jruby_runtime_ClassIndex.h"
#include "JLocalEnv.h"

using namespace jruby;

extern "C" int
rb_type(VALUE val)
{
    if (IS_CONST(val)) {
        switch (val) {
            case Qnil:
                return T_NIL;

            case Qfalse:
                return T_FALSE;

            case Qtrue:
                return T_TRUE;

            case Qundef:
                return T_UNDEF;

            default:
                rb_raise(rb_eTypeError, "invalid constant");
        }
    }

    Handle* h = (Handle *) val;
    if (h->type != T_NONE) {
        return h->type;
    }

    // Lazy lookup the type
    JLocalEnv env;
    jobject obj = env->NewLocalRef(h->obj);
    if (env->IsSameObject(obj, NULL)) {
        rb_raise(rb_eRuntimeError, "failed to get type of NULL object");
    }

    return h->type = typeOf(env, obj);
}

static struct types {
    int type;
    const char *name;
} builtin_types[] = {
    {T_NIL,	"nil"},
    {T_OBJECT,	"Object"},
    {T_CLASS,	"Class"},
    {T_ICLASS,	"iClass"},	/* internal use: mixed-in module holder */
    {T_MODULE,	"Module"},
    {T_FLOAT,	"Float"},
    {T_STRING,	"String"},
    {T_REGEXP,	"Regexp"},
    {T_ARRAY,	"Array"},
    {T_FIXNUM,	"Fixnum"},
    {T_HASH,	"Hash"},
    {T_STRUCT,	"Struct"},
    {T_BIGNUM,	"Bignum"},
    {T_FILE,	"File"},
    {T_TRUE,	"true"},
    {T_FALSE,	"false"},
    {T_SYMBOL,	"Symbol"},	/* :symbol */
    {T_DATA,	"Data"},	/* internal use: wrapped C pointers */
    {T_MATCH,	"MatchData"},	/* data of $~ */
    {T_VARMAP,	"Varmap"},	/* internal use: dynamic variables */
    {T_SCOPE,	"Scope"},	/* internal use: variable scope */
    {T_NODE,	"Node"},	/* internal use: syntax tree node */
    {T_UNDEF,	"undef"},	/* internal use: #undef; should not happen */
    {-1,	0}
};

extern "C" void
rb_check_type(VALUE x, int t)
{
    struct types *type = builtin_types;

    if (x == Qundef) {
	rb_bug("undef leaked to the Ruby space");
    }

    if (TYPE(x) != t) {
	while (type->type >= 0) {
	    if (type->type == t) {
		const char *etype = "unknown";

		if (NIL_P(x)) {
		    etype = "nil";
		}
		else if (FIXNUM_P(x)) {
		    etype = "Fixnum";
		}
		else if (SYMBOL_P(x)) {
		    etype = "Symbol";
		}
#ifdef notyet // FIXME 
		else if (rb_special_const_p(x)) {
		    etype = RSTRING(rb_obj_as_string(x))->ptr;
		}
		else {
		    etype = rb_obj_classname(x);
		}
#endif // notyet
		rb_raise(rb_eTypeError, "wrong argument type %s (expected %s)",
			 etype, type->name);
	    }
	    type++;
	}
	rb_bug("unknown type 0x%x", t);
    }
}

int
jruby::typeOf(JNIEnv* env, jobject obj) {

    if (env->IsSameObject(obj, NULL)) {
        rb_raise(rb_eRuntimeError, "invalid object");
        return 0;
    }

    // Non RubyObject subclasses are just treated as objects
    if (!env->IsInstanceOf(obj, jruby::RubyObject_class)) {
        return T_OBJECT;
    }

    // FIXME ClassIndex.DATA should really be declared
    if (env->IsInstanceOf(obj, jruby::RubyData_class)) {
        return T_DATA;
    }

    int type = env->CallIntMethod(obj, jruby::RubyObject_getNativeTypeIndex_method);
    jruby::checkExceptions(env);

    switch (type) {
#define T(x) case org_jruby_runtime_ClassIndex_##x: return T_##x
        T(NIL);
        T(OBJECT);
        T(CLASS);
        T(MODULE);
        T(FLOAT);
        T(STRING);
        T(REGEXP);
        T(ARRAY);
        T(FIXNUM);
        T(HASH);
        T(STRUCT);
        T(BIGNUM);
        T(TRUE);
        T(FALSE);
        T(SYMBOL);
    }

    return T_OBJECT;
}