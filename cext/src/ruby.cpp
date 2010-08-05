/*
 * Copyright (C) 2008-2010 Wayne Meissner
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
#include "JLocalEnv.h"
#include "JString.h"

extern "C" {

VALUE rb_mKernel;
VALUE rb_mComparable;
VALUE rb_mEnumerable;
VALUE rb_mErrno;
VALUE rb_mFileTest;
VALUE rb_mGC;
VALUE rb_mMath;
VALUE rb_mProcess;

VALUE rb_cObject;
VALUE rb_cArray;
VALUE rb_cBignum;
VALUE rb_cBinding;
VALUE rb_cClass;
VALUE rb_cDir;
VALUE rb_cData;
VALUE rb_cFalseClass;
VALUE rb_cFile;
VALUE rb_cFixnum;
VALUE rb_cFloat;
VALUE rb_cHash;
VALUE rb_cInteger;
VALUE rb_cIO;
VALUE rb_cMethod;
VALUE rb_cModule;
VALUE rb_cNilClass;
VALUE rb_cNumeric;
VALUE rb_cProc;
VALUE rb_cRange;
VALUE rb_cRegexp;
VALUE rb_cString;
VALUE rb_cStruct;
VALUE rb_cSymbol;
VALUE rb_cThread;
VALUE rb_cTime;
VALUE rb_cTrueClass;

VALUE rb_eException;
VALUE rb_eStandardError;
VALUE rb_eSystemExit;
VALUE rb_eInterrupt;
VALUE rb_eSignal;
VALUE rb_eFatal;
VALUE rb_eArgError;
VALUE rb_eEOFError;
VALUE rb_eIndexError;
VALUE rb_eStopIteration;
VALUE rb_eRangeError;
VALUE rb_eIOError;
VALUE rb_eRuntimeError;
VALUE rb_eSecurityError;
VALUE rb_eSystemCallError;
VALUE rb_eThreadError;
VALUE rb_eTypeError;
VALUE rb_eZeroDivError;
VALUE rb_eNotImpError;
VALUE rb_eNoMemError;
VALUE rb_eNoMethodError;
VALUE rb_eFloatDomainError;
VALUE rb_eLocalJumpError;
VALUE rb_eSysStackError;
VALUE rb_eRegexpError;


VALUE rb_eScriptError;
VALUE rb_eNameError;
VALUE rb_eSyntaxError;
VALUE rb_eLoadError;

}
static VALUE getConstClass(JNIEnv* env, const char* name);
static VALUE getConstModule(JNIEnv* env, const char* name);

using namespace jruby;

jstring
getGlobalVariableName(JNIEnv* env, const char* name)
{
    char var_name[strlen(name) + 1];
    (name[0] != '$') ? strcpy(var_name, "$")[0] : var_name[0] = '\0';
    strcat(var_name, name);

    return env->NewStringUTF(var_name);
}

/**
 * Define a global constant. Uses the corresponding Java method on
 * the Ruby class.
 * @param c string with the constant name
 * @param Ruby object to define the variable on
 */
extern "C" void
rb_define_global_const(const char* name, VALUE obj)
{
    JLocalEnv env;

    jmethodID mid = getMethodID(env, Ruby_class, "defineGlobalConstant",
            "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)V");
    env->CallObjectMethod(getRuntime(), mid, env->NewStringUTF(name), valueToObject(env, obj));
}

extern "C" VALUE
rb_gv_get(const char* name)
{
    JLocalEnv env;

    jmethodID mid = getStaticMethodID(env, JRuby_class, "gv_get",
            "(Lorg/jruby/Ruby;Ljava/lang/String;)J");
    jlong result = env->CallStaticLongMethod(JRuby_class, mid, getRuntime(),
            getGlobalVariableName(env, name));
    checkExceptions(env);

    return (VALUE)result;
}

extern "C" VALUE
rb_gv_set(const char* name, VALUE value)
{
    JLocalEnv env;

    jmethodID mid = getStaticMethodID(env, JRuby_class, "gv_set",
            "(Lorg/jruby/Ruby;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)J");
    jlong result = env->CallStaticLongMethod(JRuby_class, mid, getRuntime(),
            getGlobalVariableName(env, name), valueToObject(env, value));
    checkExceptions(env);

    return (VALUE)result;
}

extern "C" VALUE
rb_f_global_variables()
{
    return callMethod(rb_mKernel, "global_variables", 0);
}

extern "C" void
rb_set_kcode(const char *code)
{
    rb_gv_set("$KCODE", rb_str_new_cstr(code));
}

extern "C" VALUE
rb_eval_string(const char* string)
{
    JLocalEnv env;

    jmethodID mid = getMethodID(env, Ruby_class, "evalScriptlet",
            "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
    jobject result = env->CallObjectMethod(getRuntime(), mid, env->NewStringUTF(string));
    checkExceptions(env);
    return objectToValue(env, result);
}

#define M(x) rb_m##x = getConstModule(env, #x)
#define C(x) rb_c##x = getConstClass(env, #x)
#define E(x) rb_e##x = getConstClass(env, #x)

void
jruby::initRubyClasses(JNIEnv* env, jobject runtime)
{
    M(Kernel);
    M(Comparable);
    M(Enumerable);
    M(Errno);
    M(FileTest);
    M(Math);
    M(Process);

    C(Object);
    C(Array);
    C(Bignum);
    C(Binding);
    C(Class);
    C(Dir);
    C(Data);
    C(FalseClass);
    C(File);
    C(Fixnum);
    C(Float);
    C(Hash);
    C(Integer);
    C(IO);
    C(Method);
    C(Module);
    C(NilClass);
    C(Numeric);
    C(Proc);
    C(Range);
    C(Regexp);
    C(String);
    C(Struct);
    C(Symbol);
    C(Thread);
    C(Time);
    C(TrueClass);

    E(Exception);
    E(StandardError);
    E(SystemExit);
    E(Interrupt);
    rb_eSignal = getConstClass(env, "SignalException");
    E(Fatal);
    rb_eArgError = getConstClass(env, "ArgumentError");
    E(EOFError);
    E(IndexError);
    E(StopIteration);
    E(RangeError);
    E(IOError);
    E(RuntimeError);
    E(SecurityError);
    E(SystemCallError);
    E(ThreadError);
    E(TypeError);
    rb_eZeroDivError = getConstClass(env, "ZeroDivisionError");
    rb_eNotImpError = getConstClass(env, "NotImplementedError");
    rb_eNoMemError = getConstClass(env, "NoMemoryError");
    E(NoMethodError);
    E(FloatDomainError);
    E(LocalJumpError);
    rb_eSysStackError = getConstClass(env, "SystemStackError");
    E(RegexpError);


    E(ScriptError);
    E(NameError);
    E(SyntaxError);
    E(LoadError);
}

static VALUE
getConstClass(JNIEnv* env, const char* name)
{
    VALUE v = jruby::getClass(env, name);
    jruby::Handle::valueOf(v)->flags |= FL_CONST;
    return v;
}

static VALUE
getConstModule(JNIEnv* env, const char* name)
{
    VALUE v = jruby::getModule(env, name);
    jruby::Handle::valueOf(v)->flags |= FL_CONST;
    return v;
}
