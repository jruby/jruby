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

#include <errno.h>
#include "Handle.h"
#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"
#include "JString.h"

extern "C" {

RUBY_DLLSPEC VALUE rb_mKernel;
RUBY_DLLSPEC VALUE rb_mComparable;
RUBY_DLLSPEC VALUE rb_mEnumerable;
RUBY_DLLSPEC VALUE rb_mErrno;
RUBY_DLLSPEC VALUE rb_mFileTest;
RUBY_DLLSPEC VALUE rb_mGC;
RUBY_DLLSPEC VALUE rb_mMath;
RUBY_DLLSPEC VALUE rb_mProcess;

RUBY_DLLSPEC VALUE rb_cObject;
RUBY_DLLSPEC VALUE rb_cArray;
RUBY_DLLSPEC VALUE rb_cBignum;
RUBY_DLLSPEC VALUE rb_cBinding;
RUBY_DLLSPEC VALUE rb_cClass;
RUBY_DLLSPEC VALUE rb_cDir;
RUBY_DLLSPEC VALUE rb_cData;
RUBY_DLLSPEC VALUE rb_cFalseClass;
RUBY_DLLSPEC VALUE rb_cFile;
RUBY_DLLSPEC VALUE rb_cFixnum;
RUBY_DLLSPEC VALUE rb_cFloat;
RUBY_DLLSPEC VALUE rb_cHash;
RUBY_DLLSPEC VALUE rb_cInteger;
RUBY_DLLSPEC VALUE rb_cIO;
RUBY_DLLSPEC VALUE rb_cMatch;
RUBY_DLLSPEC VALUE rb_cMethod;
RUBY_DLLSPEC VALUE rb_cModule;
RUBY_DLLSPEC VALUE rb_cNilClass;
RUBY_DLLSPEC VALUE rb_cNumeric;
RUBY_DLLSPEC VALUE rb_cProc;
RUBY_DLLSPEC VALUE rb_cRange;
RUBY_DLLSPEC VALUE rb_cRegexp;
RUBY_DLLSPEC VALUE rb_cString;
RUBY_DLLSPEC VALUE rb_cStruct;
RUBY_DLLSPEC VALUE rb_cSymbol;
RUBY_DLLSPEC VALUE rb_cThread;
RUBY_DLLSPEC VALUE rb_cTime;
RUBY_DLLSPEC VALUE rb_cTrueClass;

RUBY_DLLSPEC VALUE rb_eException;
RUBY_DLLSPEC VALUE rb_eStandardError;
RUBY_DLLSPEC VALUE rb_eSystemExit;
RUBY_DLLSPEC VALUE rb_eInterrupt;
RUBY_DLLSPEC VALUE rb_eSignal;
RUBY_DLLSPEC VALUE rb_eFatal;
RUBY_DLLSPEC VALUE rb_eArgError;
RUBY_DLLSPEC VALUE rb_eEOFError;
RUBY_DLLSPEC VALUE rb_eIndexError;
RUBY_DLLSPEC VALUE rb_eStopIteration;
RUBY_DLLSPEC VALUE rb_eRangeError;
RUBY_DLLSPEC VALUE rb_eIOError;
RUBY_DLLSPEC VALUE rb_eRuntimeError;
RUBY_DLLSPEC VALUE rb_eSecurityError;
RUBY_DLLSPEC VALUE rb_eSystemCallError;
RUBY_DLLSPEC VALUE rb_eThreadError;
RUBY_DLLSPEC VALUE rb_eTypeError;
RUBY_DLLSPEC VALUE rb_eZeroDivError;
RUBY_DLLSPEC VALUE rb_eNotImpError;
RUBY_DLLSPEC VALUE rb_eNoMemError;
RUBY_DLLSPEC VALUE rb_eNoMethodError;
RUBY_DLLSPEC VALUE rb_eFloatDomainError;
RUBY_DLLSPEC VALUE rb_eLocalJumpError;
RUBY_DLLSPEC VALUE rb_eSysStackError;
RUBY_DLLSPEC VALUE rb_eRegexpError;


RUBY_DLLSPEC VALUE rb_eScriptError;
RUBY_DLLSPEC VALUE rb_eNameError;
RUBY_DLLSPEC VALUE rb_eSyntaxError;
RUBY_DLLSPEC VALUE rb_eLoadError;

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

    jmethodID mid = getCachedMethodID(env, Ruby_class, "defineGlobalConstant",
            "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)V");
    env->CallObjectMethod(getRuntime(), mid, env->NewStringUTF(name), valueToObject(env, obj));
}

extern "C" VALUE
rb_gv_get(const char* name)
{
    JLocalEnv env;

    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_gv_get_method, getRuntime(),
            getGlobalVariableName(env, name));
    checkExceptions(env);

    return (VALUE)result;
}

extern "C" VALUE
rb_gv_set(const char* name, VALUE value)
{
    JLocalEnv env;

    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_gv_set_method, getRuntime(),
            getGlobalVariableName(env, name), valueToObject(env, value));
    checkExceptions(env);

    return (VALUE)result;
}

extern "C" void
rb_define_variable(const char *name, VALUE *var)
{
    // FIXME this is not correct - we should define a VariableAccessor and have it read/write the C var©g168
    rb_gv_set(name, *var);
}

extern "C" void
rb_define_readonly_variable(const char* name, VALUE* value)
{
    JLocalEnv env;
    jstring varName;
    if (name[0] == '$') {
        varName = env->NewStringUTF(name);
    } else {
        char _name[strlen(name) + 2];
        _name[0] = '$';
        _name[1] = '\0';
        strcat(_name, name);
        varName = env->NewStringUTF(_name);
    }
    env->CallVoidMethod(getRuntime(), Ruby_defineReadonlyVariable_method, varName, valueToObject(env, *value));
    checkExceptions(env);
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

    jmethodID mid = getCachedMethodID(env, Ruby_class, "evalScriptlet",
            "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
    jobject result = env->CallObjectMethod(getRuntime(), mid, env->NewStringUTF(string));
    checkExceptions(env);
    return objectToValue(env, result);
}

extern "C" void
rb_sys_fail(const char* msg)
{
    JLocalEnv env;
    env->CallVoidMethod(JRuby_class, JRuby_sysFail, getRuntime(), env->NewStringUTF(msg), errno);
    rb_bug("rb_sys_fail does return"); // to suppress warning: noreturn function does return
}

extern "C" void
rb_throw(const char* symbol, VALUE result)
{
    VALUE params[2] = {ID2SYM(rb_intern(symbol)), result};
    callMethodA(rb_mKernel, "throw", 2, params);
    rb_bug("rb_throw does return"); // to suppress warning: noreturn function does return
}

extern "C" VALUE 
rb_errinfo(void)
{
    return rb_gv_get("$!");
}

extern "C" void 
rb_set_errinfo(VALUE err)
{
    rb_gv_set("$!", err);
}

extern "C" VALUE 
ruby_verbose(void)
{
    return rb_gv_get("$VERBOSE");
}

extern "C" VALUE 
ruby_debug(void)
{
    return rb_gv_get("$DEBUG");
}

const char* ruby_sourcefile = "unknown";

RUBY_DLLSPEC const char *
rb_sourcefile(void)
{
    return "unknown";
}

RUBY_DLLSPEC int 
rb_sourceline(void)
{
    return -1;
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
    rb_cMatch = getConstClass(env, "MatchData");
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
