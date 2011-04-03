/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include "JUtil.h"
#include "jruby.h"
#include "ruby.h"
#include "Handle.h"
#include "JLocalEnv.h"

/* Array */
/* The same value as 1.8.x */
#define ARRAY_DEFAULT_SIZE 16

using namespace jruby;

RubyArray::RubyArray(JNIEnv* env, jobject obj_): Handle(env, obj_, T_ARRAY)
{
    rwdata.rarray = NULL;
}

RubyArray::~RubyArray()
{
    // See Java_org_jruby_cext_Native_freeRArray
}

static bool
RubyArray_jsync(JNIEnv* env, DataSync* data)
{
    return ((RubyArray *) data->data)->jsync(env);
}

static bool
RubyArray_nsync(JNIEnv* env, DataSync* data)
{
    return ((RubyArray *) data->data)->nsync(env);
}

static bool
RubyArray_clean(JNIEnv* env, DataSync* data)
{
    return ((RubyArray *) data->data)->clean(env);
}

struct RArray*
RubyArray::toRArray(bool readonly)
{
    if (rwdata.rarray != NULL) {
        if (readonly || !rwdata.readonly) {
            return rwdata.rarray;
        }

        // Switch from readonly to read-write
        rwdata.readonly = false;
        TAILQ_INSERT_TAIL(&jruby::nsyncq, &rwdata.nsync, syncq);
        JLocalEnv env;
        nsync(env);

        return rwdata.rarray;
    }

    JLocalEnv env;
    rwdata.jsync.data = this;
    rwdata.jsync.sync = RubyArray_jsync;
    rwdata.nsync.data = this;
    rwdata.nsync.sync = RubyArray_nsync;
    rwdata.clean.data = this;
    rwdata.clean.sync = RubyArray_clean;
    rwdata.rarray = (RArray *) j2p(env->CallStaticLongMethod(JRuby_class, JRuby_getRArray, obj));
    checkExceptions(env);
    rwdata.readonly = readonly;

    TAILQ_INSERT_TAIL(&jruby::cleanq, &rwdata.clean, syncq);
    TAILQ_INSERT_TAIL(&jruby::jsyncq, &rwdata.jsync, syncq);
    if (!readonly) {
        TAILQ_INSERT_TAIL(&jruby::nsyncq, &rwdata.nsync, syncq);
    }
    nsync(env);

    return rwdata.rarray;
}

bool
RubyArray::clean(JNIEnv* env)
{
    // Clear the cached data
    rwdata.readonly = false;
    rwdata.rarray = NULL;

    return false;
}

bool
RubyArray::jsync(JNIEnv* env)
{
    if (rwdata.readonly && rwdata.rarray != NULL) {
        // Readonly, just clear the cached data
        rwdata.rarray = NULL;
        rwdata.readonly = false;
        return false;
    }

    if (rwdata.rarray != NULL && rwdata.rarray->ptr != NULL) {
        jobjectArray values = (jobjectArray)(env->GetObjectField(obj, RubyArray_values_field));
        checkExceptions(env);
        jint begin = env->GetIntField(obj, RubyArray_begin_field);
        checkExceptions(env);

        RArray* rarray = rwdata.rarray;
        assert((env->GetArrayLength(values) - begin) >= rarray->aux.capa);

        long used_length = rarray->len;
        // Copy all values back into the Java array
        for (long i = 0; i < used_length; i++) {
            if (unlikely(rarray->ptr[i] == Qundef)) {
                used_length = i;
                break; // Qundef cannot be assigned normally. End here.
            } else {
                env->SetObjectArrayElement(values, i + begin, valueToObject(env, rarray->ptr[i]));
                checkExceptions(env);
            }
        }
        env->DeleteLocalRef(values);

        env->SetIntField(obj, RubyArray_length_field, (jint)used_length);
        checkExceptions(env);
    }
    return true;
}

bool
RubyArray::nsync(JNIEnv* env)
{
    // Retrieve real element array, it's length and the actual object array and it's length

    long len = (long)(env->GetIntField(obj, RubyArray_length_field));
    checkExceptions(env);
    jobjectArray values = (jobjectArray)(env->GetObjectField(obj, RubyArray_values_field));
    checkExceptions(env);
    jint begin = env->GetIntField(obj, RubyArray_begin_field);
    checkExceptions(env);
    long capa = (long)(env->GetArrayLength(values) - begin);
    checkExceptions(env);

    assert(len <= capa);

    RArray* rarray = rwdata.rarray;

    // If capacity has grown, reallocate the C array
    if ((capa > rarray->aux.capa) || (rarray->aux.capa == 0)) {
        rarray->aux.capa = capa;
        rarray->ptr = (VALUE*)realloc(rarray->ptr, sizeof(VALUE) * capa);
    }

    // If there is content, copy over
    for (long i = 0; i < len; i++) {
        rarray->ptr[i] = objectToValue(env, env->GetObjectArrayElement(values, i + begin));
        checkExceptions(env);
    }

    // Fill capacity with Qundefs
    for (long i = len; i < capa; i++) {
        rarray->ptr[i] = Qundef;
    }

    env->DeleteLocalRef(values);
    rarray->len = len;
    return true;
}

static VALUE
newArray(long len)
{
    if (len < 0) {
        rb_raise(rb_eArgError, "negative array size (or size too big)");
    }

    JLocalEnv env;
    jobject ary = env->CallStaticObjectMethod(RubyArray_class, RubyArray_newArray, getRuntime(), (jlong)len);
    checkExceptions(env);

    VALUE ary_value = objectToValue(env, ary);
    return ary_value;
}

extern "C" RArray*
jruby_rarray(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_ARRAY) {
        return ((RubyArray *) h)->toRArray(false);
    }

    rb_raise(rb_eTypeError, "wrong type (expected Array)");
}

extern "C" VALUE
rb_Array(VALUE val)
{
    return callMethod(rb_cArray, "new", 1, val);
}

extern "C" VALUE
rb_ary_new2(long length)
{
    return newArray(length);
}

extern "C" VALUE
rb_ary_new(void)
{
    return newArray(0);
}

extern "C" VALUE
rb_ary_new3(long size, ...)
{
    va_list args;
    VALUE ary = newArray(size);

    va_start(args, size);
    for (long i = 0; i < size; i++) {
        rb_ary_push(ary, va_arg(args, VALUE));
    }
    va_end(args);
    return ary;
}

extern "C" VALUE
rb_ary_new4(long n, const VALUE* argv)
{
    VALUE ary = newArray(n);

    for (long i = 0; i < n; ++i) {
        rb_ary_push(ary, argv[i]);
    }

    return ary;
}

extern "C" VALUE
rb_assoc_new(VALUE key, VALUE value) {
    VALUE ary = newArray(2);
    rb_ary_push(ary, key);
    rb_ary_push(ary, value);
    return ary;
}

extern "C" VALUE
rb_ary_push(VALUE array, VALUE val)
{
    return callMethod(array, "push", 1, val);
}

extern "C" VALUE
rb_ary_pop(VALUE array)
{
    return callMethod(array, "pop", 0);
}

extern "C" VALUE
rb_ary_entry(VALUE array, long offset)
{
    return callMethod(array, "[]", 1, LONG2NUM(offset));
}

extern "C" VALUE
rb_ary_clear(VALUE array)
{
    return callMethod(array, "clear", 0);
}

VALUE
rb_ary_dup(VALUE array)
{
    return callMethod(array, "dup", 0);
}

extern "C" VALUE
rb_ary_join(VALUE array1, VALUE array2)
{
    return callMethod(array1, "join", 1, array2);
}

extern "C" VALUE
rb_ary_reverse(VALUE array)
{
    return callMethod(array, "reverse!", 0);
}

extern "C" VALUE
rb_ary_unshift(VALUE array, VALUE val)
{
    return callMethod(array, "unshift", 1, val);
}

extern "C" VALUE
rb_ary_shift(VALUE array)
{
    return callMethod(array, "shift", 0);
}

extern "C" void
rb_ary_store(VALUE array, long offset, VALUE val)
{
    callMethod(array, "[]=", 2, LONG2NUM(offset), val);
}

extern "C" VALUE
rb_ary_includes(VALUE ary, VALUE item)
{
    return callMethodA(ary, "include?", 1, &item);
}

extern "C" VALUE
rb_ary_delete(VALUE ary, VALUE item)
{
    return callMethodA(ary, "delete", 1, &item);
}

extern "C" VALUE
rb_ary_delete_at(VALUE ary, long pos)
{
    return callMethod(ary, "delete_at", 1, LONG2NUM(pos));
}

extern "C" VALUE
rb_ary_aref(int argc, VALUE* argv, VALUE ary)
{
    return callMethodA(ary, "slice", argc, argv);
}

extern "C" VALUE
rb_check_array_type(VALUE val)
{
    return rb_check_convert_type(val, 0, "Array", "to_ary");
}

extern "C" int
rb_ary_size(VALUE ary)
{
    JLocalEnv env;
    int length = (int)(env->GetIntField(valueToObject(env, ary), RubyArray_length_field));
    checkExceptions(env);
    return length;
}

// Copyied from Rbx
// Really just used as a placeholder/sentinal value to half implement
// rb_iterate
extern "C" VALUE
rb_each(VALUE ary)
{
    rb_raise(rb_eArgError, "rb_each not fully supported", 0);
    return Qnil;
}

extern "C" VALUE
rb_iterate(VALUE(*ifunc)(VALUE), VALUE ary, VALUE(*cb)(ANYARGS), VALUE cb_data)
{
    if (ifunc != rb_each || !rb_obj_is_kind_of(ary, rb_cArray)) {
        rb_raise(rb_eArgError, "rb_iterate only supported with rb_each and an Array");
        return Qnil;
    }

    for (int i = 0; i < rb_ary_size(ary); i++) {
        (*cb)(rb_ary_entry(ary, i), cb_data, Qnil);
    }

    return ary;
}
