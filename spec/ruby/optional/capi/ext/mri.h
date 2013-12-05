#ifndef RUBYSPEC_CAPI_MRI_H
#define RUBYSPEC_CAPI_MRI_H

/* #undef any HAVE_ defines that MRI does not have. */
#undef HAVE_RB_HASH_LOOKUP
#undef HAVE_RB_HASH_SIZE

#undef HAVE_RB_OBJ_FROZEN_P

#undef HAVE_RB_STR_PTR
#undef HAVE_RB_STR_PTR_READONLY

#undef HAVE_THREAD_BLOCKING_REGION

#ifdef RUBY_VERSION_IS_1_8_EX_1_8_7
#undef HAVE_RB_EXEC_RECURSIVE
#undef HAVE_RBIGNUM_SIGN
#undef HAVE_RBIGNUM_POSITIVE_P
#undef HAVE_RBIGNUM_NEGATIVE_P
#undef HAVE_RB_STR_SET_LEN
#undef HAVE_RB_BLOCK_CALL
#undef HAVE_RB_OBJ_METHOD
#endif

#ifdef RUBY_VERSION_IS_1_8_EX_1_9
#undef HAVE_RB_THREAD_BLOCKING_REGION
#undef HAVE_RB_RATIONAL
#undef HAVE_RB_RATIONAL1
#undef HAVE_RB_RATIONAL2
#undef HAVE_RB_NUM_COERCE_BIN
#undef HAVE_RB_NUM_COERCE_CMP
#undef HAVE_RB_NUM_COERCE_RELOP
#undef HAVE_RB_EMATHDOMAINERROR
#undef HAVE_RB_PATH_TO_CLASS
#endif

#ifdef RUBY_VERSION_IS_1_9
#undef HAVE_RARRAY
#undef HAVE_RFLOAT
#undef HAVE_RSTRING
#undef HAVE_STR2CSTR
#undef HAVE_RB_STR2CSTR
#undef HAVE_RB_SET_KCODE
#undef HAVE_RB_PROTECT_INSPECT
#endif

/* RubySpec assumes following are public API */
#ifndef rb_proc_new
VALUE rb_proc_new _((VALUE (*)(ANYARGS/* VALUE yieldarg[, VALUE procarg] */), VALUE));
#endif
#ifndef rb_str_len
int rb_str_len(VALUE);
#endif
#ifndef rb_set_errinfo
void rb_set_errinfo(VALUE);
#endif

/* Macros that may not be defined in old versions */
#ifndef RARRAY_PTR
#define RARRAY_PTR(s) (*(VALUE *const *)&RARRAY(s)->ptr)
#endif

#ifndef RARRAY_LEN
#define RARRAY_LEN(s) (*(const long *)&RARRAY(s)->len)
#endif

#ifndef RFLOAT_VALUE
#define RFLOAT_VALUE(v) (RFLOAT(v)->value)
#endif

#endif
