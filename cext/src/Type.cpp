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
 * Copyright (C) 2008-2010 Wayne Meissner
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

#include "Handle.h"
#include "jruby.h"
#include "ruby.h"
#include "org_jruby_runtime_ClassIndex.h"
#include "JLocalEnv.h"

using namespace jruby;

extern "C" int
rb_type(VALUE obj)
{
    if (IMMEDIATE_P(obj)) {
        if (FIXNUM_P(obj)) return T_FIXNUM;
        if (obj == Qtrue) return T_TRUE;
        if (SYMBOL_P(obj)) return T_SYMBOL;
        if (obj == Qundef) return T_UNDEF;

    } else if (!RTEST(obj)) {
        if (obj == Qnil) return T_NIL;
        if (obj == Qfalse) return T_FALSE;

    }
    return Handle::valueOf(obj)->getType();
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
