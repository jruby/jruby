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
#include <limits.h>

// A number of extensions expect these to be already included
#include <stddef.h>
#include <stdlib.h>
#include <sys/time.h>
#include <stdio.h>
#include <sys/select.h>
#include <string.h>  
#include <unistd.h>
#include <fcntl.h>

#ifdef	__cplusplus
extern "C" {
#endif

#ifdef __cplusplus
# define ANYARGS ...
#else
# define ANYARGS
#endif
  
#define LONG_LONG long long

typedef uintptr_t ID;
typedef uintptr_t VALUE;
typedef intptr_t SIGNED_VALUE;

#ifndef RSHIFT
# define RSHIFT(x,y) ((x)>>(int)y)
#endif


#define FIXNUM_MAX (LONG_MAX>>1)
#define FIXNUM_MIN RSHIFT((long)LONG_MIN,1)

#define FIXNUM_P(f) (((SIGNED_VALUE)(f))&FIXNUM_FLAG)
#define POSFIXABLE(f) ((f) < FIXNUM_MAX+1)
#define NEGFIXABLE(f) ((f) >= FIXNUM_MIN)
#define FIXABLE(f) (POSFIXABLE(f) && NEGFIXABLE(f))

#define IMMEDIATE_MASK 0x3
#define IMMEDIATE_P(x) ((VALUE)(x) & IMMEDIATE_MASK)
#define SPECIAL_CONST_P(x) (IMMEDIATE_P(x) || !RTEST(x))

#define FIXNUM_FLAG 0x1
#define SYMBOL_FLAG 0x0e
#define SYMBOL_P(x) (((VALUE)(x)&0xff)==SYMBOL_FLAG)
#define ID2SYM(x) ((VALUE)(((long)(x))<<8|SYMBOL_FLAG))
#define SYM2ID(x) RSHIFT((unsigned long)x,8)

/** The false object. */
#define Qfalse ((VALUE)0)
/** The true object. */
#define Qtrue  ((VALUE)2)
/** The nil object. */
#define Qnil   ((VALUE)4)
/** The undef object. Value for placeholder */
#define Qundef ((VALUE)6)

typedef struct RArrayFacade {
    VALUE* ptr;
    int len;
} RArrayFacade;

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
#define TYPE(x) rb_type((VALUE)(x))

struct RBasic {
    VALUE unused0;
    VALUE unused1;
};

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

/* Interface macros */

/** Allocate memory for type. Must NOT be used to allocate Ruby objects. */
#define ALLOC(type) (type*)xmalloc(sizeof(type))

/** Allocate memory for N of type. Must NOT be used to allocate Ruby objects. */
#define ALLOC_N(type,n) (type*)xmalloc(sizeof(type)*(n))

/** Reallocate memory allocated with ALLOC or ALLOC_N. */
#define REALLOC_N(var,type,n) (var)=(type*)xrealloc((char*)(var),sizeof(type)*(n))

/** Interrupt checking (no-op). */
#define CHECK_INTS        /* No-op */

/** Test macros */
#define RTEST(v) (((v) & ~Qnil) != 0)
#define NIL_P(v) ((v) == Qnil)
#define TYPE(x) rb_type((VALUE)(x))

/** Convert a Fixnum into an int. */
#define FIX2INT(x) ((int) RSHIFT((SIGNED_VALUE)x,1))
/** Convert a Fixnum into an unsigned int. */
#define FIX2UINT(x) ((unsigned int) ((((VALUE)(x))>>1)&LONG_MAX))

#define FIX2LONG(x) RSHIFT((SIGNED_VALUE)x,1)
#define FIX2ULONG(x) ((((VALUE)(x))>>1)&LONG_MAX)

/** Convert a VALUE into a long int. */
#define NUM2LONG(x) rb_num2long(x)
/** Convert a VALUE into a long int. */
#define NUM2ULONG(x) rb_num2ulong(x)
/** Convert a VALUE into an int. */
#define NUM2INT(x) ((int) rb_num2int(x))
/** Convert a VALUE into a long int. */
#define NUM2UINT(x) ((int) rb_num2uint(x))
/** Convert a VALUE into a long long */
#define NUM2LL(x) rb_num2ll(x)
/** Convert a VALUE into an unsigned long long */
#define NUM2ULL(x) rb_num2ull(x)
#define NUM2DBL(x) rb_num2dbl(x)

/** Convert int to a Ruby Integer. */
#define INT2FIX(x)   rb_int2inum(x)
/** Convert unsigned int to a Ruby Integer. */
#define UINT2FIX(x)  rb_uint2inum(x)
/** Convert int to a Ruby Integer. */
#define INT2NUM(x)   rb_int2inum(x)
#define UINT2NUM(x)  rb_uint2inum(x)
#define LONG2FIX(x)  rb_int2inum(x)
#define LONG2NUM(x)  rb_int2inum(x)
#define ULONG2NUM(x) rb_uint2inum(x)
#define LL2NUM(x)    rb_ll2inum(x)
#define ULL2NUM(x)   rb_ull2inum(x)

/** The length of string str. */
#define RSTRING_LEN(str)  jruby_str_length((str))
/** The pointer to the string str's data. */
#define RSTRING_PTR(str)  jruby_str_ptr((str))
/** Pointer to the MRI string structure */
#define RSTRING(str) jruby_rstring((str))

/** The length of the array. */
#define RARRAY_LEN(ary)   rb_ary_size(ary)
/** The pointer to the array's data. */
#define RARRAY_PTR(ary)   rb_ary_ptr(ary)
/** Pointer to the MRI array structure */
#define RARRAY(str) rb_ary_struct_readonly(str);

#define DATA_PTR(dta) (jruby_data((dta)))

/* End of interface macros */

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
double rb_num2dbl(VALUE);

VALUE rb_int2inum(long);
VALUE rb_uint2inum(unsigned long);
VALUE rb_ll2inum(long long);
VALUE rb_ull2inum(unsigned long long);
VALUE rb_int2big(long long);
VALUE rb_uint2big(unsigned long long);


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
/** Returns a pointer to a persistent VALUE [] that mirrors the data in
 * the ruby array. 
 * TODO: The pointer buffer is flushed to the ruby array when
 * control returns to Ruby code. The buffer is updated with the array
 * contents when control crosses to C code.
 *
 * @note This is NOT an MRI C-API function.
 */
VALUE *rb_ary_ptr(VALUE self);
/** Returns a pointer to the readonly RArrayFacade structure
 * which exposes an MRI-like API to the C code.
 *
 * @note This is NOT an MRI C-API function.
 */
struct RArrayFacade rb_ary_struct_readonly(VALUE ary);

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
} while (0)

void rb_gc_mark_locations(VALUE*, VALUE*);
void rb_gc_mark(VALUE);
/** Mark variable global */
void rb_global_variable(VALUE* handle_address);
void rb_gc_register_address(VALUE* address);
/** Unmark variable as global */
void rb_gc_unregister_address(VALUE* address);

/** Define a toplevel constant */
void rb_define_global_const(const char* name, VALUE obj);

extern ID rb_intern_const(const char *);
extern ID jruby_intern_nonconst(const char *);
#define rb_intern(name) \
    (__builtin_constant_p(name) ? rb_intern_const(name) : jruby_intern_nonconst(name))

struct RString {
    struct RBasic basic;
    union {
        struct {
            long len;
            char *ptr;
            long capa;
        } heap;
        char unused[sizeof(VALUE) * 3];
    } as;
};

extern struct RString* jruby_rstring(VALUE v);
extern int jruby_str_length(VALUE v);
extern char* jruby_str_ptr(VALUE v);
extern char* rb_str_ptr_readonly(VALUE v);

struct RFloat {
    struct RBasic basic;
    double value;
};

extern struct RFloat* jruby_rfloat(VALUE v);
extern VALUE rb_float_new(double value);
extern double jruby_float_value(VALUE v);
#define RFLOAT_VALUE(v) jruby_float_value(v)

/** Call block with given argument or raise error if no block given. */
VALUE rb_yield(VALUE argument_handle);

/** Freeze object and return it. */
VALUE rb_obj_freeze(VALUE obj);
#define OBJ_FREEZE(obj) (rb_obj_freeze(obj))

/* Global Module objects. */
extern VALUE rb_mKernel;
extern VALUE rb_mComparable;
extern VALUE rb_mEnumerable;
extern VALUE rb_mErrno;
extern VALUE rb_mFileTest;
extern VALUE rb_mGC;
extern VALUE rb_mMath;
extern VALUE rb_mProcess;

/* Global Class objects */
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

/* Exception classes. */
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


#ifdef	__cplusplus
}
#endif

#endif	/* JRUBY_RUBY_H */

