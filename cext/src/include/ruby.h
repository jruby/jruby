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
#define JRUBY

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
#include <ctype.h>

#ifdef	__cplusplus
extern "C" {
#endif

#ifdef __cplusplus
# ifndef  HAVE_PROTOTYPES
#  define HAVE_PROTOTYPES 1
# endif
# ifndef  HAVE_STDARG_PROTOTYPES
#  define HAVE_STDARG_PROTOTYPES 1
# endif
#endif

#ifndef NORETURN
#define NORETURN(x) __attribute__ ((noreturn)) x
#endif

#undef _
#ifdef HAVE_PROTOTYPES
# define _(args) args
#else
# define _(args) ()
#endif

#undef __
#ifdef HAVE_STDARG_PROTOTYPES
# define __(args) args
#else
# define __(args) ()
#endif

#ifdef __cplusplus
# define ANYARGS ...
#else
# define ANYARGS
#endif
  
#define LONG_LONG long long

/** In MRI, ID represents an interned string, i.e. a Symbol. */    
typedef uintptr_t ID;
/** In MRI, VALUE represents an object. */
typedef uintptr_t VALUE;
typedef intptr_t SIGNED_VALUE;

#ifndef RSHIFT
# define RSHIFT(x,y) ((x)>>(int)y)
#endif

/** Zero out N elements of type starting at given pointer. */
#define MEMZERO(p,type,n) memset((p), 0, (sizeof(type) * (n)))
/** Copies n objects of type from p2 to p1. Behavior is undefined if objects overlap. */
#define MEMCPY(p1,p2,type,n) memcpy((p1), (p2), sizeof(type)*(n))
/** Copies n objects of type from p2 to p1. Objects may overlap. */
#define MEMMOVE(p1,p2,type,n) memmove((p1), (p2), sizeof(type)*(n))
/** Compares n objects of type. */
#define MEMCMP(p1,p2,type,n) memcmp((p1), (p2), sizeof(type)*(n))

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

#define OBJ_FROZEN(x) jruby_obj_frozen(x)

/** The false object. */
#define Qfalse ((VALUE)0)
/** The true object. */
#define Qtrue  ((VALUE)2)
/** The nil object. */
#define Qnil   ((VALUE)4)
/** The undef object. Value for placeholder */
#define Qundef ((VALUE)6)

struct RBasic {
    VALUE unused0;
    VALUE unused1;
};

struct RString {
    struct RBasic basic;
    long len;
    char *ptr;
    long capa;
};

struct RArray {
    VALUE* ptr;
    int len;
};

struct RIO {
    int fd;
    FILE* f;
};

typedef struct RIO rb_io_t;
typedef struct RIO OpenFile; // 1.8 compat
#define HAVE_RB_IO_T 1

struct RFloat {
    struct RBasic basic;
    double value;
};

struct RData {
    struct RBasic basic;
    void (*dmark)(void *);
    void (*dfree)(void *);
    void* data;
};

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

#ifdef __GNUC__
#define rb_special_const_p(obj) \
    __extension__ ({VALUE special_const_obj = (obj); (int)(SPECIAL_CONST_P(special_const_obj) ? Qtrue : Qfalse);})
#else
static inline int
rb_special_const_p(VALUE obj)
{
    if (SPECIAL_CONST_P(obj)) return (int)Qtrue;
    return (int)Qfalse;
}
#endif

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

/* need to include <ctype.h> to use these macros */
#ifndef ISPRINT
#define ISASCII(c) isascii((int)(unsigned char)(c))
#undef ISPRINT
#define ISPRINT(c) (ISASCII(c) && isprint((int)(unsigned char)(c)))
#define ISSPACE(c) (ISASCII(c) && isspace((int)(unsigned char)(c)))
#define ISUPPER(c) (ISASCII(c) && isupper((int)(unsigned char)(c)))
#define ISLOWER(c) (ISASCII(c) && islower((int)(unsigned char)(c)))
#define ISALNUM(c) (ISASCII(c) && isalnum((int)(unsigned char)(c)))
#define ISALPHA(c) (ISASCII(c) && isalpha((int)(unsigned char)(c)))
#define ISDIGIT(c) (ISASCII(c) && isdigit((int)(unsigned char)(c)))
#define ISXDIGIT(c) (ISASCII(c) && isxdigit((int)(unsigned char)(c)))
#endif

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
#define CLASS_OF(x) rb_class_of((VALUE)(x))

/** Convert a Fixnum into an int. */
#define FIX2INT(x) ((int) RSHIFT((SIGNED_VALUE)x,1))
/** Convert a Fixnum into an unsigned int. */
#define FIX2UINT(x) ((unsigned int) ((((VALUE)(x))>>1)&LONG_MAX))

#define FIX2LONG(x) RSHIFT((SIGNED_VALUE)x,1)
#define FIX2ULONG(x) ((((VALUE)(x))>>1)&LONG_MAX)

/** Convert a VALUE into a long int. */
#define NUM2ULONG(x) rb_num2ulong(x)
/** Convert a VALUE into a chr */
#define NUM2CHR(x) rb_num2chr(x)
/** Convert a VALUE into a long int. */
#define NUM2UINT(x) ((int) rb_num2uint(x))
/** Convert a VALUE into a long long */
#define NUM2LL(x) rb_num2ll(x)
/** Convert a VALUE into an unsigned long long */
#define NUM2ULL(x) rb_num2ull(x)
#define NUM2DBL(x) rb_num2dbl(x)

/** Convert int to a Ruby Integer. */
#define INT2FIX(i)   ((int)(((SIGNED_VALUE)(i))<<1 | FIXNUM_FLAG))
/** Convert unsigned int to a Ruby Integer. */
#define UINT2FIX(x)  rb_uint2inum(x)
/** Convert int to a Ruby Integer. */

#define LONG2FIX(x)  INT2FIX(x)
#define LONG2NUM(x)  rb_int2inum(x)
#define ULONG2NUM(x) rb_uint2inum(x)
#define LL2NUM(x)    rb_ll2inum(x)
#define ULL2NUM(x)   rb_ull2inum(x)
#define rb_int_new(v) rb_int2inum(v)

/** The length of string str. */
#define RSTRING_LEN(str)  jruby_str_length((str))
/** The pointer to the string str's data. */
#define RSTRING_PTR(str)  jruby_str_cstr((str))
/** Pointer to the MRI string structure */
#define RSTRING(str) jruby_rstring((str))
#define STR2CSTR(str)         rb_str2cstr((VALUE)(str), 0)
/** Modifies the VALUE object in place by calling rb_obj_as_string(). */
#define StringValue(v)        rb_string_value(&(v))
#define StringValuePtr(v)     rb_string_value_ptr(&(v))
#define StringValueCStr(str)  rb_string_value_cstr(&(str))

/** The length of the array. */
#define RARRAY_LEN(ary) RARRAY(ary)->len
/** Returns a pointer to a VALUE[] that mirrors the data in
 * the ruby array.
 * TODO: The pointer buffer should be flushed to the ruby array when
 * control returns to Ruby code and be updated with the array
 * contents when control crosses to C code.
 */
#define RARRAY_PTR(ary) RARRAY(ary)->ptr
/** Pointer to the MRI array structure */
#define RARRAY(ary) jruby_rarray(ary)

#define RHASH_SIZE(h) FIX2INT(rb_hash_size(h))
#define RHASH_LEN(h) FIX2INT(rb_hash_size(h))

#define RFLOAT(v) jruby_rfloat(v)
#define RFLOAT_VALUE(v) jruby_float_value(v)

#define DATA_PTR(dta) (RDATA(dta)->data)
#define RDATA(dta) jruby_rdata((dta))

#define OBJ_FREEZE(obj) (rb_obj_freeze(obj))

/* End of interface macros */

/**
 *  Process arguments using a template rather than manually.
 *
 *  The first two arguments are simple: the number of arguments given
 *  and an array of the args. Usually you get these as parameters to
 *  your function.
 *
 *  The spec works like this: it must have one (or more) of the following
 *  specifiers, and the specifiers that are given must always appear
 *  in the order given here. If the first character is a digit (0-9),
 *  it is the number of required parameters. If there is a second digit
 *  (0-9), it is the number of optional parameters. The next character
 *  may be "*", indicating a "splat" i.e. it consumes all remaining
 *  parameters. Finally, the last character may be "&", signifying
 *  that the block given (or Qnil) should be stored.
 *
 *  The remaining arguments are pointers to the variables in which
 *  the aforementioned format assigns the scanned parameters. For
 *  example in some imaginary function:
 *
 *    VALUE required1, required2, optional, splat, block
 *    rb_scan_args(argc, argv, "21*&", &required1, &required2,
 *                                     &optional,
 *                                     &splat,
 *                                     &block);
 *
 *  The required parameters must naturally always be exact. The
 *  optional parameters are set to nil when parameters run out.
 *  The splat is always an Array, but may be empty if there were
 *  no parameters that were not consumed by required or optional.
 *  Lastly, the block may be nil.
 */
int rb_scan_args(int argc, const VALUE* argv, const char* spec, ...);
/** Returns true on first load, false if already loaded or raises. */
VALUE rb_require(const char* name);

void rb_raise(VALUE exc, const char *fmt, ...) __attribute__((noreturn));
VALUE rb_rescue(VALUE(*)(ANYARGS),VALUE,VALUE(*)(ANYARGS),VALUE);
VALUE rb_rescue2(VALUE(*)(ANYARGS),VALUE,VALUE(*)(ANYARGS),VALUE,...);
VALUE rb_ensure(VALUE(*)(ANYARGS),VALUE,VALUE(*)(ANYARGS),VALUE);

void rb_fatal(const char *fmt, ...) __attribute__((noreturn));
void rb_sys_fail(const char *msg) __attribute__((noreturn));
void rb_bug(const char*, ...) __attribute__((noreturn));
VALUE rb_exc_new(VALUE, const char*, long);
VALUE rb_exc_new2(VALUE, const char*);
VALUE rb_exc_new3(VALUE, VALUE);
VALUE rb_exc_raise(VALUE);

void rb_secure(int);
int rb_safe_level(void);
void rb_set_safe_level(int);
void rb_set_safe_level_force(int);
void rb_secure_update(VALUE);

long rb_num2long(VALUE);
unsigned long rb_num2ulong(VALUE);
long rb_num2int(VALUE);
unsigned long rb_num2uint(VALUE);
char rb_num2chr(VALUE);
double rb_num2dbl(VALUE);
long rb_fix2int(VALUE);
unsigned long rb_fix2uint(VALUE);
long long rb_num2ll(VALUE);
unsigned long long rb_num2ull(VALUE);
double rb_num2dbl(VALUE);
SIGNED_VALUE rb_big2long(VALUE);
#define rb_big2int(x) rb_big2long(x)
VALUE rb_big2ulong(VALUE);
#define rb_big2uint(x) rb_big2ulong(x)
long long rb_big2ll(VALUE);
double rb_big2dbl(VALUE);
VALUE rb_big2str(VALUE, int);


VALUE rb_int2inum(long);
VALUE rb_uint2inum(unsigned long);
VALUE rb_ll2inum(long long);
VALUE rb_ull2inum(unsigned long long);
VALUE rb_int2big(long long);
VALUE rb_uint2big(unsigned long long);
VALUE rb_Integer(VALUE);

/** Converts an object to an Integer by calling #to_int. */
VALUE rb_to_int(VALUE);
/** Converts an object to an Integer using the specified method */
VALUE rb_to_integer(VALUE, const char*);

/** Convert a VALUE into a long int. */
static inline long
NUM2LONG(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2LONG(x) : rb_num2long(x);
}

/** Convert a VALUE into an int. */
static inline int
NUM2INT(VALUE x)
{
    return __builtin_expect(FIXNUM_P(x), 1) ? FIX2INT(x) : rb_num2int(x);
}

static inline VALUE
INT2NUM(long v)
{
    if (__builtin_expect(FIXABLE(v), 1)) {
        return INT2FIX(v);
    }

    return rb_int2inum(v);
}

static inline VALUE
UINT2NUM(unsigned long v)
{
    if (__builtin_expect(POSFIXABLE(v), 1)) {
        return LONG2FIX(v);
    }

    return rb_uint2inum(v);
}


VALUE rb_funcall(VALUE obj, ID meth, int cnt, ...);
VALUE rb_funcall2(VALUE obj, ID meth, int cnt, VALUE*);
/** Starts the lookup in the superclass to call a method on the current self */
VALUE rb_call_super(int argc, const VALUE *argv);

/** Returns a new, anonymous class inheriting from super_handle. */
VALUE rb_class_new(VALUE);
/** Calls the class method 'inherited' on super passing the class. */
VALUE rb_class_inherited(VALUE super, VALUE klass);
/** As Ruby's .new, with the given arguments. Returns the new object. */
VALUE rb_class_new_instance(int arg_count, VALUE* args, VALUE class_handle);
/** Returns the Class object this object is an instance of. */
VALUE rb_class_of(VALUE object_handle);
/** Returns String representation of the class' name. */
VALUE rb_class_name(VALUE class_handle);
/** C string representation of the class' name. You must free this string. */
char* rb_class2name(VALUE class_handle);
/** Convert a path string to a class */
VALUE rb_path2class(const char* path);
/** Include Module in another Module, just as Ruby's Module#include. */
void rb_include_module(VALUE self, VALUE module);
/** Return the object's singleton class */
VALUE rb_singleton_class(VALUE obj);

VALUE rb_define_class(const char*,VALUE);
VALUE rb_define_module(const char*);
VALUE rb_define_class_under(VALUE, const char*, VALUE);
VALUE rb_define_module_under(VALUE, const char*);
/** Ruby's attr_* for given name. Nonzeros to toggle read/write. */
void rb_define_attr(VALUE module_handle, const char* attr_name, int readable, int writable);
void rb_define_method(VALUE,const char*,VALUE(*)(ANYARGS),int);
void rb_define_private_method(VALUE,const char*,VALUE(*)(ANYARGS),int);
void rb_define_protected_method(VALUE,const char*,VALUE(*)(ANYARGS),int);
void rb_define_module_function(VALUE,const char*,VALUE(*)(ANYARGS),int);
void rb_define_global_function(const char*,VALUE(*)(ANYARGS),int);
void rb_define_singleton_method(VALUE object, const char* meth, VALUE(*fn)(ANYARGS), int arity);
#define HAVE_RB_DEFINE_ALLOC_FUNC 1
typedef VALUE (*rb_alloc_func_t)(VALUE);
void rb_define_alloc_func(VALUE, rb_alloc_func_t);

void rb_undef_method(VALUE, const char*);
void rb_undef(VALUE, ID);

#define rb_define_class_variable(klass, name, val) rb_cvar_set(klass, rb_intern(name), val, 0)
#define rb_cv_get(klass, name) rb_cvar_get(klass, rb_intern(name))
#define rb_cv_set(klass, name, value) rb_cvar_set(klass, rb_intern(name), value, 0)
/** Returns a value evaluating true if module has named class var.
 * TODO: @@ should be optional. 
 */
VALUE rb_cvar_defined(VALUE module_handle, ID name);
/** Returns class variable by (Symbol) name from module.
 * TODO: @@ should be optional. 
 */
VALUE rb_cvar_get(VALUE module_handle, ID name);
/** Set module's named class variable to given value. Returns the value. 
 * TODO: @@ should be optional. 
 */
VALUE rb_cvar_set(VALUE module_handle, ID name, VALUE value);
#define rb_cvar_set(klass, name, value, ...) __extension__(rb_cvar_set(klass, name, value))

/** Return object's instance variable by name. @ optional. */
VALUE rb_iv_get(VALUE obj, const char* name);
/** Set instance variable by name to given value. Returns the value. @ optional. */
VALUE rb_iv_set(VALUE obj, const char* name, VALUE value);
/** Get object's instance variable. */
VALUE rb_ivar_get(VALUE obj, ID ivar_name);
/** Set object's instance variable to given value. */
VALUE rb_ivar_set(VALUE obj, ID ivar_name, VALUE value);

/** Nonzero if constant corresponding to Symbol exists in the Module. */
int rb_const_defined(VALUE, ID);
/** Returns non-zero if the constant is defined in the Module, not searching outside */
int rb_const_defined_at(VALUE, ID);
/** Retrieve constant from given module. */
VALUE rb_const_get(VALUE, ID);
/** Returns a constant defined in module only. */
VALUE rb_const_get_at(VALUE, ID);
/** Retrieve constant from given module. */
VALUE rb_const_get_from(VALUE, ID);
/** Set constant on the given module */
void rb_const_set(VALUE, ID, VALUE);

/** Alias method by old name as new name. */
void rb_define_alias(VALUE klass, const char *new_name, const char *old_name);

/* Array */
VALUE rb_Array(VALUE val);
VALUE rb_ary_new(void);
VALUE rb_ary_new2(long length);
VALUE rb_ary_new4(long n, const VALUE *);
int rb_ary_size(VALUE self);
VALUE rb_ary_push(VALUE array, VALUE val);
VALUE rb_ary_pop(VALUE array);
VALUE rb_ary_entry(VALUE array, long offset);
VALUE rb_ary_clear(VALUE array);
VALUE rb_ary_dup(VALUE array);
VALUE rb_ary_join(VALUE array1, VALUE array2);
VALUE rb_ary_reverse(VALUE array);
VALUE rb_ary_unshift(VALUE array, VALUE val);
VALUE rb_ary_shift(VALUE array);
void rb_ary_store(VALUE array, long offset, VALUE val);
VALUE rb_ary_includes(VALUE, VALUE);
VALUE rb_ary_delete(VALUE, VALUE);
VALUE rb_ary_delete_at(VALUE, long);
VALUE rb_ary_aref(int, VALUE*, VALUE);


/** Returns a pointer to the readonly RArray structure
 * which exposes an MRI-like API to the C code.
 */
struct RArray* jruby_rarray(VALUE ary);

/* Hash */
VALUE rb_hash_new(void);
VALUE rb_hash_aref(VALUE hash, VALUE key);
VALUE rb_hash_aset(VALUE hash, VALUE key, VALUE val);
VALUE rb_hash_delete(VALUE hash, VALUE key);
VALUE rb_hash_size(VALUE hash);

/* String */
VALUE rb_str_new(const char*, long);
VALUE rb_str_new2(const char*);
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
VALUE rb_str_length(VALUE str);
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
/**
 * Returns a pointer to the String, the length is returned
 * in len parameter, which can be NULL.
 */
char* rb_str2cstr(VALUE str_handle, long *len);
/** Deprecated alias for rb_obj_freeze */
VALUE rb_str_freeze(VALUE str);
/** Return Integer obtained from String#to_i using given base. */
VALUE rb_str2inum(VALUE str, int base);
#define rb_cstr2inum(str, base) rb_str2inum(rb_str_new_cstr(str), base)
#define rb_cstr_to_inum(str, base, badcheck) rb_cstr2inum(str, base)
/** Return a String using #to_str. Error raised if invalid conversion. */
VALUE rb_str_to_str(VALUE);
/** Return a String using #to_s. Error raised if invalid conversion. */
VALUE rb_String(VALUE);
/** Call #to_s on object pointed to and _replace_ it with the String. */
VALUE rb_string_value(VALUE* object_variable);
char* rb_string_value_ptr(VALUE* object_variable);
/** As rb_string_value but also returns a C string of the new String. */
char* rb_string_value_cstr(VALUE* object_variable);

extern struct RString* jruby_rstring(VALUE v);
extern int jruby_str_length(VALUE v);
char* jruby_str_cstr(VALUE v);
char* jruby_str_cstr_readonly(VALUE v);

#define rb_str_ptr_readonly(v) jruby_str_cstr_readonly((v))
#define rb_str_ptr(v) jruby_str_cstr((v))
#define rb_str_new_cstr(str) __extension__ (            \
{                                                       \
    (__builtin_constant_p(str) && str != NULL) ?        \
        rb_str_new(str, (long)strlen(str)) :            \
        rb_str_new_cstr(str);                           \
})
#define rb_tainted_str_new_cstr(str) __extension__ (    \
{                                                       \
    (__builtin_constant_p(str) && str != NULL) ?        \
        rb_tainted_str_new(str, (long)strlen(str)) :          \
        rb_tainted_str_new_cstr(str);                   \
})
#define rb_str_buf_new_cstr(str) __extension__ (        \
{                                                       \
    (__builtin_constant_p(str) && str != NULL) ?        \
        rb_str_buf_cat(rb_str_buf_new((long)strlen(str)), \
                       str, (long)strlen(str)) : \
        rb_str_buf_new_cstr(str);               \
})
#define rb_str_buf_cat2(str, ptr) __extension__ (       \
{                                                       \
    (__builtin_constant_p(ptr) && ptr != NULL) ?        \
        rb_str_buf_cat(str, ptr, (long)strlen(ptr)) :  \
        rb_str_buf_cat2(str, ptr);                      \
})
#define rb_str_cat2(str, ptr) __extension__ (           \
{                                                       \
    (__builtin_constant_p(ptr) && ptr != NULL) ?                       \
        rb_str_cat(str, ptr, (long)strlen(ptr)) :       \
        rb_str_cat2(str, ptr);                          \
})

#define rb_str_new2 rb_str_new_cstr
#define rb_str_new3 rb_str_dup
#define rb_str_new4 rb_str_new_frozen
#define rb_str_new5 rb_str_new_with_class
#define rb_tainted_str_new2 rb_tainted_str_new_cstr
#define rb_str_buf_new2 rb_str_buf_new_cstr
#define rb_usascii_str_new2 rb_usascii_str_new_cstr

/** Returns the string associated with a symbol. */
const char *rb_id2name(ID sym);
/** Call #to_sym on object. */
ID rb_to_id(VALUE);

/** Returns a Struct with the specified fields. */
VALUE rb_struct_define(const char *name, ...);

void* jruby_data(VALUE);
struct RData* jruby_rdata(VALUE);

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
/** Return the global variable. $ optional */
VALUE rb_gv_get(const char* name);
/** Set named global to given value, returning the value. $ optional. */
VALUE rb_gv_set(const char* name, VALUE value);
/** Return an array containing the names of all global variables */
VALUE rb_f_global_variables();

/** Print a warning if $VERBOSE is not nil. */
void rb_warn(const char *fmt, ...);
/** Print a warning if $VERBOSE is true. */
void rb_warning(const char *fmt, ...);

/** 1 if obj.respond_to? method_name evaluates true, 0 otherwise. */
int rb_respond_to(VALUE obj_handle, ID method_name);
/** Returns object returned by invoking method on object if right type, or raises error. */
VALUE rb_convert_type(VALUE object_handle, int type, const char* type_name, const char* method_name);
/** Returns object returned by invoking method on object or nil */
VALUE rb_check_convert_type(VALUE val, int type, const char* type_name, const char* method);
VALUE rb_check_array_type(VALUE val);
VALUE rb_check_string_type(VALUE val);

/** Define a constant in given Module's namespace. */
void rb_define_const(VALUE module, const char* name, VALUE obj);
/** Define a toplevel constant */
void rb_define_global_const(const char* name, VALUE obj);
ID rb_intern(const char*);
ID rb_intern2(const char*, long);
ID rb_intern_const(const char*);

#define CONST_ID_CACHE(result, str)                     \
    {                                                   \
        static ID rb_intern_id_cache;                   \
        if (__builtin_expect(!rb_intern_id_cache, 0))           \
            rb_intern_id_cache = rb_intern2(str, strlen(str));  \
        result rb_intern_id_cache;                      \
    }

#define rb_intern(str) \
    (__builtin_constant_p(str) \
        ? __extension__ (CONST_ID_CACHE(/**/, str)) : rb_intern(str))

#define rb_intern_const(str) \
    (__builtin_constant_p(str) \
        ? __extension__ (rb_intern2(str, strlen(str))) : rb_intern(str))

extern struct RFloat* jruby_rfloat(VALUE v);
extern VALUE rb_float_new(double value);
extern double jruby_float_value(VALUE v);
VALUE rb_Float(VALUE object_handle);

int rb_big_bytes_used(VALUE obj);
#define RBIGNUM_LEN(obj) rb_big_bytes_used(obj)
// fake out, used with RBIGNUM_LEN anyway, which provides the full answer
#define SIZEOF_BDIGITS 1

/** Call block with given argument or raise error if no block given. */
VALUE rb_yield(VALUE argument);
/** Return 1 if block given, 0 if not */
int rb_block_given_p();
/** Return the Proc for the implicit block */
VALUE rb_block_proc();

/** Freeze object and return it. */
VALUE rb_obj_freeze(VALUE obj);
/** Raise an error if the object is frozen */
void rb_check_frozen(VALUE obj);
/** Allocate uninitialised instance of given class. */
VALUE rb_obj_alloc(VALUE klass);
/** Call initialize */
void rb_obj_call_init(VALUE recv, int arg_count, VALUE* args);
/** String representation of the object's class' name. You must free this string. */
char* rb_obj_classname(VALUE object_handle);
/** Returns true-ish if module is object's class or other ancestor. */
VALUE rb_obj_is_kind_of(VALUE, VALUE);
VALUE rb_obj_is_instance_of(VALUE, VALUE);
/** Returns the Class object this object is an instance of. */
#define rb_obj_class(object) rb_class_of((object))

void rb_extend_object(VALUE, VALUE);
VALUE rb_obj_taint(VALUE);
VALUE rb_obj_tainted(VALUE);
VALUE rb_any_to_s(VALUE obj);
VALUE rb_inspect(VALUE obj);
VALUE rb_obj_as_string(VALUE obj);

#define OBJ_TAINT(obj) rb_obj_taint((obj))
#define OBJ_TAINTED(obj) rb_obj_tainted((obj))

VALUE rb_attr_get(VALUE obj, ID id);

VALUE rb_exc_new(VALUE, const char*, long);
VALUE rb_exc_new2(VALUE, const char*);
VALUE rb_exc_new3(VALUE, VALUE);

#define rb_exc_new2(klass, ptr) __extension__ ( \
{                                               \
    (__builtin_constant_p(ptr)) ?               \
        rb_exc_new(klass, ptr, (long)strlen(ptr)) : \
        rb_exc_new2(klass, ptr);                \
})
    
VALUE rb_io_write(VALUE io, VALUE str);
int rb_io_fd(VALUE io);
#define HAVE_RB_IO_FD 1
// Writes the OpenFile struct pointer of val into ptr
#define GetOpenFile(val, ptr) (ptr = jruby_io_struct(val))
#define GetReadFile(ptr) (ptr->f)
#define GetWriteFile(ptr) (ptr->f)

VALUE rb_range_new(VALUE, VALUE, int);


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

