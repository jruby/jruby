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
 * Copyright (C) 1993-2007 Yukihiro Matsumoto
 * Copyright (C) 2000  Network Applied Communication Laboratory, Inc.
 * Copyright (C) 2000  Information-technology Promotion Agency, Japan
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/param.h>
#include <jni.h>

#include "JLocalEnv.h"
#include "jruby.h"
#include "JUtil.h"
#include "ruby.h"
#include "Handle.h"

using namespace jruby;

RubyString::RubyString(JNIEnv* env, jobject obj_): Handle(env, obj_, T_STRING)
{
    memset(&rwdata, 0, sizeof(rwdata));
}

RubyString::~RubyString()
{
    RString* rstring = rwdata.rstring;
    if (rstring != NULL) {
        free(rstring->ptr);
        free(rstring);
    }
}

int
RubyString::length()
{
    // If already synced with java, just return the cached length value
    if (rwdata.rstring != NULL && rwdata.valid) {
        return rwdata.rstring->len;
    }

    JLocalEnv env;

    jobject byteList = env->GetObjectField(obj, RubyString_value_field);

    return env->GetIntField(byteList, ByteList_length_field);
}

static bool
RubyString_jsync(JNIEnv* env, DataSync* data)
{
    return ((RubyString *) data->data)->jsync(env);
}

static bool
RubyString_nsync(JNIEnv* env, DataSync* data)
{
    return ((RubyString *) data->data)->nsync(env);
}

static bool
RubyString_clean(JNIEnv* env, DataSync* data)
{
    return ((RubyString *) data->data)->clean(env);
}

RString*
RubyString::toRString(bool readonly)
{
    if (rwdata.rstring != NULL && rwdata.valid) {
        if (readonly || !rwdata.readonly) {
            return rwdata.rstring;
        }

        // Switch from readonly to read-write
        rwdata.readonly = false;
	TAILQ_INSERT_TAIL(&jruby::jsyncq, &rwdata.jsync, syncq);
        JLocalEnv env;
        nsync(env);

        return rwdata.rstring;
    }

    rwdata.jsync.data = this;
    rwdata.jsync.sync = RubyString_jsync;
    rwdata.nsync.data = this;
    rwdata.nsync.sync = RubyString_nsync;
    rwdata.clean.data = this;
    rwdata.clean.sync = RubyString_clean;
    if (rwdata.rstring == NULL) {
	rwdata.rstring = (RString *) calloc(1, sizeof(RString));
	if (rwdata.rstring == NULL) {
	    rb_raise(rb_eNoMemError, "failed to allocate memory for RString");
	}
    }
    rwdata.readonly = readonly;

    TAILQ_INSERT_TAIL(&jruby::cleanq, &rwdata.clean, syncq);
    TAILQ_INSERT_TAIL(&jruby::nsyncq, &rwdata.nsync, syncq);
    if (!readonly) {
	TAILQ_INSERT_TAIL(&jruby::jsyncq, &rwdata.jsync, syncq);
    }

    JLocalEnv env;
    nsync(env);

    return rwdata.rstring;
}

bool
RubyString::clean(JNIEnv* env)
{
    // Invalidate the cached data
    rwdata.valid = false;

    return false;
}

bool
RubyString::jsync(JNIEnv* env)
{
    if (unlikely(rwdata.readonly)) {
        // Nothing to do for read-only

        return false;
    }

    if (rwdata.valid && rwdata.rstring != NULL && rwdata.rstring->ptr != NULL) {
        jobject byteList = env->GetObjectField(obj, RubyString_value_field);
        jobject bytes = env->GetObjectField(byteList, ByteList_bytes_field);
        jint begin = env->GetIntField(byteList, ByteList_begin_field);
        checkExceptions(env);

        env->DeleteLocalRef(byteList);

        RString* rstring = rwdata.rstring;
        env->SetByteArrayRegion((jbyteArray) bytes, begin, rstring->len,
                (jbyte *) rstring->ptr);
        checkExceptions(env);
        env->SetIntField(byteList, ByteList_length_field, rstring->len);

        env->DeleteLocalRef(bytes);
    }

    return true;
}

bool
RubyString::nsync(JNIEnv* env)
{
    jobject byteList = env->GetObjectField(obj, RubyString_value_field);
    checkExceptions(env);
    jobject bytes = env->GetObjectField(byteList, ByteList_bytes_field);
    checkExceptions(env);
    jint begin = env->GetIntField(byteList, ByteList_begin_field);
    checkExceptions(env);
    long length = env->GetIntField(byteList, ByteList_length_field);
    checkExceptions(env);
    jint capacity = env->GetArrayLength((jarray) bytes) - begin;
    checkExceptions(env);
    env->DeleteLocalRef(byteList);

    RString* rstring = rwdata.rstring;

    if ((capacity > rstring->capa) || (rstring->capa == 0)) {
        rstring->capa = capacity;
        rstring->ptr = (char *) realloc(rstring->ptr, rstring->capa + 1);
	if (rstring->ptr == NULL) {
	    rb_raise(rb_eNoMemError, "failed to allocate memory for RString");
	}
    }

    env->GetByteArrayRegion((jbyteArray) bytes, begin, length,
            (jbyte *) rstring->ptr);
    checkExceptions(env);
    env->DeleteLocalRef(bytes);

    rstring->ptr[rstring->len = length] = 0;
    rwdata.valid = true;

    return true;
}

static VALUE
newString(const char* ptr, int len, int capacity = 0, bool tainted = false)
{
    if (len < 0) {
        rb_raise(rb_eArgError, "negative string size (or size too big)");
    }

    JLocalEnv env;

    jbyteArray bytes = env->NewByteArray(capacity > len ? capacity : len);
    checkExceptions(env);

    if (len > 0 && ptr != NULL) {
        env->SetByteArrayRegion(bytes, 0, len, (jbyte *) ptr);
        checkExceptions(env);
    }

    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_newString, jruby::getRuntime(), bytes, (jint)len, (jboolean) tainted);
    checkExceptions(env);

    return (VALUE) result;
}

extern "C" VALUE
rb_str_new(const char *ptr, long len)
{
    return newString(ptr, len);
}

#undef rb_str_new_cstr
extern "C" VALUE
rb_str_new_cstr(const char *ptr)
{
    if (!ptr) {
        rb_raise(rb_eArgError, "NULL pointer given");
    }

    return newString(ptr, ptr ? strlen(ptr) : 0);
}

extern "C" VALUE
rb_str_new4(VALUE str)
{
    if (callMethod(str, "frozen?", 0) == Qtrue) {
	return str;
    } else {
	return rb_str_new_frozen(str);
    }
}

#define STR_BUF_MIN_SIZE 1
extern "C" VALUE
rb_str_buf_new(long capacity)
{
    capacity = MAX(capacity, STR_BUF_MIN_SIZE);

    return newString(NULL, 0, MAX(capacity, STR_BUF_MIN_SIZE), false);
}

extern "C" VALUE
rb_str_dup(VALUE str)
{
    return callMethod(str, "dup", 0, NULL);
}

extern "C" VALUE
rb_str_dup_frozen(VALUE str)
{
    return rb_str_freeze(rb_str_dup(str));
}

extern "C" VALUE
rb_str_append(VALUE str, VALUE str2)
{
    return callMethod(str, "<<", 1, str2);
}

extern "C" VALUE
rb_str_cat(VALUE str, const char *ptr, long len)
{
    if (len < 0) {
        rb_raise(rb_eArgError, "negative string size (or size too big)");
    }

    return rb_str_concat(str, rb_str_new(ptr, len));
}

#undef rb_str_cat2
extern "C" VALUE
rb_str_cat2(VALUE str, const char *ptr)
{
    return rb_str_cat(str, ptr, ptr ? strlen(ptr) : 0);
}

extern "C" VALUE
rb_str_concat(VALUE str, VALUE other)
{
    return callMethod(str, "concat", 1, other);
}

extern "C" VALUE
rb_str_plus(VALUE str1, VALUE str2)
{
    return callMethod(str1, "+", 1, str2);
}

extern "C" VALUE
rb_str_buf_cat(VALUE str, const char *ptr, long len)
{
    if (len == 0) return str;
    if (len < 0) {
        rb_raise(rb_eArgError, "negative string size (or size too big)");
    }

    return callMethod(str, "concat", 1, rb_str_new(ptr, len));
}

#undef rb_str_buf_cat2
VALUE
rb_str_buf_cat2(VALUE str, const char *ptr)
{
    return rb_str_buf_cat(str, ptr, ptr ? strlen(ptr) : 0);
}

VALUE
rb_str_buf_append(VALUE str, VALUE str2)
{
    return callMethod(str, "concat", 1, str2);
}

extern "C" int
rb_str_cmp(VALUE str1, VALUE str2)
{
    return NUM2INT(callMethod(str1, "<=>", 1, str2));
}

extern "C" void
rb_str_update(VALUE str, long beg, long len, VALUE val)
{
  callMethod(str, "[]=", 3, LONG2NUM(beg), LONG2NUM(len), val);
}

extern "C" VALUE
rb_str_inspect(VALUE str)
{
  return callMethod(str, "inspect", 0);
}

extern "C" VALUE
rb_str_split(VALUE str, const char *sep)
{
    return callMethod(str, "split", 1, rb_str_new2(sep));
}

extern "C" VALUE
rb_str2inum(VALUE str, int base)
{
    return callMethod(str, "to_i", 1, INT2NUM(base));
}

extern "C" VALUE
rb_str_substr(VALUE str, long beg, long len)
{
    return callMethod(str, "[]", 2, LONG2NUM(beg), LONG2NUM(len));
}

extern "C" VALUE
rb_tainted_str_new(const char* ptr, long len)
{
    return newString(ptr, len, len, true);
}

#undef rb_tainted_str_new_cstr
extern "C" VALUE
rb_tainted_str_new_cstr(const char *ptr)
{
    int len;

    if (!ptr) {
        rb_raise(rb_eArgError, "NULL pointer given");
    }

    len = strlen(ptr);

    return newString(ptr, len, len, true);
}


extern "C" VALUE
rb_str_to_str(VALUE obj)
{
    return rb_convert_type(obj, T_STRING, "String", "to_str");
}

extern "C" VALUE
rb_string_value(VALUE* ptr)
{
    if (TYPE(*ptr) != T_STRING) {
        *ptr = rb_str_to_str(*ptr);
    }
    return *ptr;
}

extern "C" char*
rb_string_value_ptr(VALUE* object_variable)
{
    VALUE str = rb_string_value(object_variable);
    return (char*) (RSTRING_PTR(str));
}

extern "C" char*
rb_string_value_cstr(VALUE* object_variable)
{
    VALUE str = rb_string_value(object_variable);
    long long str_size = NUM2LL(callMethod(str, "length", 0));
    char* cstr = (char*) (RSTRING_PTR(str));

    if ((unsigned int) str_size != strlen(cstr)) {
        rb_raise(rb_eArgError, "string contains NULL byte");
    }

    return cstr;
}

extern "C" VALUE
rb_str_freeze(VALUE str)
{
    return callMethodA(str, "freeze", 0, NULL);
}

extern "C" VALUE
rb_str_intern(VALUE str)
{
    return callMethodA(str, "to_sym", 0, NULL);
}

extern "C" VALUE
rb_str_length(VALUE str)
{
    return INT2NUM(jruby_str_length(str));
}

extern "C" VALUE
rb_String(VALUE obj)
{
    return rb_convert_type(obj, T_STRING, "String", "to_s");
}

extern "C" VALUE
rb_check_string_type(VALUE val)
{
    return rb_check_convert_type(val, 0, "String", "to_str");
}

extern "C" VALUE
rb_str_resize(VALUE str, long size) {
    long length = (long) jruby_str_length(str);
    if (size != length) {
        JLocalEnv env;
        env->CallVoidMethod(valueToObject(env, str), RubyString_resize_method, (jint)size);
        checkExceptions(env);
        jruby_rstring(str)->len = size;
    }
    return str;
}

extern "C" void
rb_str_set_len(VALUE str, long size) {
    rb_str_resize(str, size);
}

static RubyString*
jruby_str(VALUE v)
{
    if (TYPE(v) != T_STRING) {
        rb_raise(rb_eTypeError, "wrong type (expected String)");
    }

    return (RubyString *) v;
}

extern "C" char*
jruby_str_cstr(VALUE v)
{
    return jruby_str(v)->toRString(false)->ptr;
}

extern "C" char*
jruby_str_cstr_readonly(VALUE v)
{
    return jruby_str(v)->toRString(true)->ptr;
}

extern "C" int
jruby_str_length(VALUE v)
{
    return jruby_str(v)->length();
}

extern "C" struct RString*
jruby_rstring(VALUE v)
{
    return jruby_str(v)->toRString(false);
}

extern "C" char*
rb_str2cstr(VALUE str, long* len)
{
    char* cstr = RSTRING_PTR(str);

    if (len) {
        *len = RSTRING_LEN(str);

    } else if ((unsigned int) RSTRING_LEN(str) != strlen(cstr)) {
        rb_warn("string contains \\0 character");
    }

    return cstr;
}


extern "C" void
rb_str_modify(VALUE str)
{
    JLocalEnv env;
    jmethodID mid = getCachedMethodID(env, RubyString_class, "modify", "()V");

    env->CallVoidMethod(valueToObject(env, str), mid);
    checkExceptions(env);
}

