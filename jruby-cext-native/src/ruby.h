/**********************************************************************

  ruby/ruby.h -

  $Author: yugui $
  created at: Thu Jun 10 14:26:32 JST 1993

  Copyright (C) 1993-2008 Yukihiro Matsumoto
  Copyright (C) 2000  Network Applied Communication Laboratory, Inc.
  Copyright (C) 2000  Information-technology Promotion Agency, Japan

**********************************************************************/

#ifndef JRUBY_RUBY_H
#define	JRUBY_RUBY_H

#include <sys/types.h>
#include <stdint.h>

#ifdef	__cplusplus
extern "C" {
#endif

#ifdef __cplusplus
# define ANYARGS ...
#else
# define ANYARGS
#endif

typedef uintptr_t ID;
typedef uintptr_t VALUE;

#define ID2SYM(id) (id)
#define SYM2ID(value) (value)

#define Qfalse ((VALUE)0)
#define Qtrue  ((VALUE)2)
#define Qnil   ((VALUE)4)
#define Qundef ((VALUE)6)     /* undefined value for placeholder */

typedef enum JRubyType {
    T_NONE,
    T_NIL,
    T_OBJECT,
    T_CLASS,
    T_ICLASS,
    T_MODULE,
    T_FLOAT,
    T_STRING,
    T_REGEXP,
    T_ARRAY,
    T_FIXNUM,
    T_HASH,
    T_STRUCT,
    T_BIGNUM,
    T_FILE,

    T_TRUE,
    T_FALSE,
    T_DATA,
    T_MATCH,
    T_SYMBOL,

    T_BLKTAG,
    T_UNDEF,
    T_VARMAP,
    T_SCOPE,
    T_NODE,
} JRubyType;

#define T_MASK (0x1f)

#define RTEST(v) (((v) & ~Qnil) != 0)
#define NIL_P(v) ((v) == Qnil)
#define FIXNUM_P(v) (rb_type((v)) == T_FIXNUM)
#define SYMBOL_P(v) (rb_type((v)) == T_SYMBOL)
#define TYPE(x) rb_type((VALUE)(x))

int rb_type(VALUE);
void rb_check_type(VALUE, int);
#define Check_Type(v,t) rb_check_type((VALUE)(v),t)


#define xmalloc ruby_xmalloc
#define xmalloc2 ruby_xmalloc2
#define xcalloc ruby_xcalloc
#define xrealloc ruby_xrealloc
#define xrealloc2 ruby_xrealloc2
#define xfree ruby_xfree

void *xmalloc(size_t);
void *xmalloc2(size_t,size_t);
void *xcalloc(size_t,size_t);
void *xrealloc(void*,size_t);
void *xrealloc2(void*,size_t,size_t);
void xfree(void*);



void rb_raise(VALUE exc, const char *fmt, ...) __attribute__((noreturn));
void rb_fatal(const char *fmt, ...) __attribute__((noreturn));
void rb_sys_fail(const char *msg) __attribute__((noreturn));
void rb_bug(const char*, ...) __attribute__((noreturn));
VALUE rb_exc_new(VALUE, const char*, long);
VALUE rb_exc_new2(VALUE, const char*);
VALUE rb_exc_new3(VALUE, VALUE);

void rb_secure(int);
int rb_safe_level(void);
void rb_set_safe_level(int);
void rb_set_safe_level_force(int);
void rb_secure_update(VALUE);

long rb_num2long(VALUE);
unsigned long rb_num2ulong(VALUE);
long rb_num2int(VALUE);
unsigned long rb_num2uint(VALUE);
long rb_fix2int(VALUE);
unsigned long rb_fix2uint(VALUE);
long long rb_num2ll(VALUE);
unsigned long long rb_num2ull(VALUE);

VALUE rb_int2inum(long);
VALUE rb_uint2inum(unsigned long);
VALUE rb_ll2inum(long long);
VALUE rb_ull2inum(unsigned long long);
VALUE rb_int2big(long long);
VALUE rb_uint2big(unsigned long long);


#define NUM2LONG(x) rb_num2long(x)
#define NUM2ULONG(x) rb_num2ulong(x)
#define FIX2INT(x) ((int) rb_num2int(x))
#define FIX2UINT(x) ((int) rb_num2uint(x))
#define NUM2INT(x) ((int) rb_num2int(x))
#define NUM2UINT(x) ((int) rb_num2uint(x))
#define NUM2LL(x) rb_num2ll(x)
#define NUM2ULL(x) rb_num2ull(x)

#define INT2FIX(x)   rb_int2inum(x)
#define UINT2FIX(x)  rb_uint2inum(x)
#define INT2NUM(x)   rb_int2inum(x)
#define UINT2NUM(x)  rb_uint2inum(x)
#define LONG2FIX(x)  rb_int2inum(x)
#define LONG2NUM(x)  rb_int2inum(x)
#define ULONG2NUM(x) rb_uint2inum(x)
#define LL2NUM(x)    rb_ll2inum(x)
#define ULL2NUM(x)   rb_ull2inum(x)


VALUE rb_funcall(VALUE obj, ID meth, int cnt, ...);
VALUE rb_funcall2(VALUE obj, ID meth, int cnt, VALUE*);

VALUE rb_define_class(const char*,VALUE);
VALUE rb_define_module(const char*);
VALUE rb_define_class_under(VALUE, const char*, VALUE);
VALUE rb_define_module_under(VALUE, const char*);

void rb_define_method(VALUE,const char*,VALUE(*)(ANYARGS),int);
void rb_define_module_function(VALUE,const char*,VALUE(*)(ANYARGS),int);
void rb_define_global_function(const char*,VALUE(*)(ANYARGS),int);

#define HAVE_RB_DEFINE_ALLOC_FUNC 1
typedef VALUE (*rb_alloc_func_t)(VALUE);
void rb_define_alloc_func(VALUE, rb_alloc_func_t);





/* Array */
VALUE rb_Array(VALUE val);
VALUE rb_ary_new(void);
VALUE rb_ary_new2(long length);
VALUE rb_ary_new4(long n, const VALUE *);
int rb_ary_size(VALUE self);
VALUE rb_ary_push(VALUE array, VALUE val);
VALUE rb_ary_pop(VALUE array);
VALUE rb_ary_entry(VALUE array, int offset);
VALUE rb_ary_clear(VALUE array);
VALUE rb_ary_dup(VALUE array);
VALUE rb_ary_join(VALUE array1, VALUE array2);
VALUE rb_ary_reverse(VALUE array);
VALUE rb_ary_unshift(VALUE array, VALUE val);
VALUE rb_ary_shift(VALUE array);
void rb_ary_store(VALUE array, int offset, VALUE val);

/* Hash */
VALUE rb_hash_new(void);
VALUE rb_hash_aref(VALUE hash, VALUE key);
VALUE rb_hash_aset(VALUE hash, VALUE key, VALUE val);
VALUE rb_hash_delete(VALUE hash, VALUE key);

/* String */
VALUE rb_str_new(const char*, long);
VALUE rb_str_new_cstr(const char*);

VALUE rb_tainted_str_new_cstr(const char*);
VALUE rb_tainted_str_new(const char*, long);
VALUE rb_str_buf_new(long);
VALUE rb_str_buf_new_cstr(const char*);
VALUE rb_str_tmp_new(long);


VALUE rb_str_buf_append(VALUE, VALUE);
VALUE rb_str_buf_cat(VALUE, const char*, long);
VALUE rb_str_buf_cat2(VALUE, const char*);
VALUE rb_str_buf_cat_ascii(VALUE, const char*);
VALUE rb_obj_as_string(VALUE);
VALUE rb_check_string_type(VALUE);
VALUE rb_str_dup(VALUE);
VALUE rb_str_locktmp(VALUE);
VALUE rb_str_unlocktmp(VALUE);
VALUE rb_str_dup_frozen(VALUE);
#define rb_str_dup_frozen rb_str_new_frozen
VALUE rb_str_plus(VALUE, VALUE);
VALUE rb_str_times(VALUE, VALUE);
long rb_str_sublen(VALUE, long);
VALUE rb_str_substr(VALUE, long, long);
VALUE rb_str_subseq(VALUE, long, long);
void rb_str_modify(VALUE);
VALUE rb_str_freeze(VALUE);
void rb_str_set_len(VALUE, long);
VALUE rb_str_resize(VALUE, long);
VALUE rb_str_cat(VALUE, const char*, long);
VALUE rb_str_cat2(VALUE, const char*);
VALUE rb_str_append(VALUE, VALUE);
VALUE rb_str_concat(VALUE, VALUE);
int rb_memhash(const void *ptr, long len);
int rb_str_hash(VALUE);
int rb_str_hash_cmp(VALUE,VALUE);
int rb_str_comparable(VALUE, VALUE);
int rb_str_cmp(VALUE, VALUE);
VALUE rb_str_equal(VALUE str1, VALUE str2);
VALUE rb_str_drop_bytes(VALUE, long);
void rb_str_update(VALUE, long, long, VALUE);
VALUE rb_str_replace(VALUE, VALUE);
VALUE rb_str_inspect(VALUE);
VALUE rb_str_dump(VALUE);
VALUE rb_str_split(VALUE, const char*);
void rb_str_associate(VALUE, VALUE);
VALUE rb_str_associated(VALUE);
void rb_str_setter(VALUE, ID, VALUE*);
VALUE rb_str_intern(VALUE);
VALUE rb_sym_to_s(VALUE);
VALUE rb_str_length(VALUE);
long rb_str_offset(VALUE, long);
size_t rb_str_capacity(VALUE);

#define rb_str_new2 rb_str_new_cstr
#define rb_str_new3 rb_str_new_shared
#define rb_str_new4 rb_str_new_frozen
#define rb_str_new5 rb_str_new_with_class
#define rb_tainted_str_new2 rb_tainted_str_new_cstr
#define rb_str_buf_new2 rb_str_buf_new_cstr
#define rb_usascii_str_new2 rb_usascii_str_new_cstr

extern void* jruby_data(VALUE);

#define DATA_PTR(dta) (jruby_data((dta)))

typedef void (*RUBY_DATA_FUNC)(void*);

VALUE rb_data_object_alloc(VALUE,void*,RUBY_DATA_FUNC,RUBY_DATA_FUNC);

#define Data_Wrap_Struct(klass,mark,free,sval)\
    rb_data_object_alloc(klass,sval,(RUBY_DATA_FUNC)mark,(RUBY_DATA_FUNC)free)

#define Data_Make_Struct(klass,type,mark,free,sval) (\
    sval = ALLOC(type),\
    memset(sval, 0, sizeof(type)),\
    Data_Wrap_Struct(klass,mark,free,sval)\
)

#define Data_Get_Struct(obj,type,sval) do {\
    Check_Type(obj, T_DATA); \
    sval = (type*)DATA_PTR(obj);\
} while (0




extern VALUE rb_mKernel;
extern VALUE rb_mComparable;
extern VALUE rb_mEnumerable;
extern VALUE rb_mErrno;
extern VALUE rb_mFileTest;
extern VALUE rb_mGC;
extern VALUE rb_mMath;
extern VALUE rb_mProcess;


extern VALUE rb_cObject;
extern VALUE rb_cArray;
extern VALUE rb_cBignum;
extern VALUE rb_cBinding;
extern VALUE rb_cClass;
extern VALUE rb_cDir;
extern VALUE rb_cData;
extern VALUE rb_cFalseClass;
extern VALUE rb_cFile;
extern VALUE rb_cFixnum;
extern VALUE rb_cFloat;
extern VALUE rb_cHash;
extern VALUE rb_cInteger;
extern VALUE rb_cIO;
extern VALUE rb_cMethod;
extern VALUE rb_cModule;
extern VALUE rb_cNilClass;
extern VALUE rb_cNumeric;
extern VALUE rb_cProc;
extern VALUE rb_cRange;
extern VALUE rb_cRegexp;
extern VALUE rb_cString;
extern VALUE rb_cStruct;
extern VALUE rb_cSymbol;
extern VALUE rb_cThread;
extern VALUE rb_cTime;
extern VALUE rb_cTrueClass;


extern VALUE rb_eException;
extern VALUE rb_eStandardError;
extern VALUE rb_eSystemExit;
extern VALUE rb_eInterrupt;
extern VALUE rb_eSignal;
extern VALUE rb_eFatal;
extern VALUE rb_eArgError;
extern VALUE rb_eEOFError;
extern VALUE rb_eIndexError;
extern VALUE rb_eStopIteration;
extern VALUE rb_eRangeError;
extern VALUE rb_eIOError;
extern VALUE rb_eRuntimeError;
extern VALUE rb_eSecurityError;
extern VALUE rb_eSystemCallError;
extern VALUE rb_eThreadError;
extern VALUE rb_eTypeError;
extern VALUE rb_eZeroDivError;
extern VALUE rb_eNotImpError;
extern VALUE rb_eNoMemError;
extern VALUE rb_eNoMethodError;
extern VALUE rb_eFloatDomainError;
extern VALUE rb_eLocalJumpError;
extern VALUE rb_eSysStackError;
extern VALUE rb_eRegexpError;

extern VALUE rb_eScriptError;
extern VALUE rb_eNameError;
extern VALUE rb_eSyntaxError;
extern VALUE rb_eLoadError;


struct RBasic {
    int type;
};

#ifdef	__cplusplus
}
#endif

#endif	/* RUBY_H */

