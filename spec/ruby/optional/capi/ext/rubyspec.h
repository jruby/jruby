#ifndef RUBYSPEC_H
#define RUBYSPEC_H

/* Define convenience macros similar to the RubySpec guards to assist
 * with version incompatibilities.
 */

#include "rubyspec_version.h"

#if RUBY_VERSION_MAJOR >= 2
#define RUBY_VERSION_IS_2_0
#endif

#if defined(RUBY_VERSION_IS_2_0) || (RUBY_VERSION_MINOR == 9 && RUBY_VERSION_TEENY >= 3)
#define RUBY_VERSION_IS_1_9_3
#endif

#if defined(RUBY_VERSION_IS_2_0) || (RUBY_VERSION_MINOR == 9 && RUBY_VERSION_TEENY >= 2)
#define RUBY_VERSION_IS_1_9_2
#endif

#if defined(RUBY_VERSION_IS_2_0) || RUBY_VERSION_MINOR == 9
#define RUBY_VERSION_IS_1_9
#endif

#if RUBY_VERSION_MAJOR == 1 && RUBY_VERSION_MINOR == 8
#define RUBY_VERSION_IS_1_8_EX_1_9
#endif

#if defined(RUBY_VERSION_IS_2_0) || RUBY_VERSION_MINOR >= 8
#define RUBY_VERSION_IS_1_8
#endif

#if defined(RUBY_VERSION_IS_1_9) || (RUBY_VERSION_MINOR == 8 && RUBY_VERSION_TEENY >= 7)
#define RUBY_VERSION_IS_1_8_7
#endif

#if RUBY_VERSION_MAJOR == 1 && RUBY_VERSION_MINOR == 8 && RUBY_VERSION_TEENY < 7
#define RUBY_VERSION_IS_1_8_EX_1_8_7
#endif


/* Define all function flags */

/* Array */
#define HAVE_RARRAY                        1
#define HAVE_RARRAY_LEN                    1
#define HAVE_RARRAY_PTR                    1
#define HAVE_RB_ARY_AREF                   1
#define HAVE_RB_ARY_CLEAR                  1
#define HAVE_RB_ARY_DELETE                 1
#define HAVE_RB_ARY_DELETE_AT              1
#define HAVE_RB_ARY_DUP                    1
#define HAVE_RB_ARY_ENTRY                  1
#define HAVE_RB_ARY_FREEZE                 1
#define HAVE_RB_ARY_INCLUDES               1
#define HAVE_RB_ARY_JOIN                   1
#define HAVE_RB_ARY_NEW                    1
#define HAVE_RB_ARY_NEW2                   1
#define HAVE_RB_ARY_NEW3                   1
#define HAVE_RB_ARY_NEW4                   1
#define HAVE_RB_ARY_POP                    1
#define HAVE_RB_ARY_PUSH                   1
#define HAVE_RB_ARY_REVERSE                1
#define HAVE_RB_ARY_SHIFT                  1
#define HAVE_RB_ARY_STORE                  1
#define HAVE_RB_ARY_CONCAT                 1
#define HAVE_RB_ARY_TO_ARY                 1
#define HAVE_RB_ARY_TO_S                   1
#define HAVE_RB_ARY_UNSHIFT                1
#define HAVE_RB_ASSOC_NEW                  1
#define HAVE_RB_INSPECTING_P               1
#define HAVE_RB_PROTECT_INSPECT            1

#define HAVE_RB_EACH                       1
#define HAVE_RB_ITERATE                    1
#define HAVE_RB_MEM_CLEAR                  1

/* Bignum */
#define HAVE_RBIGNUM_NEGATIVE_P            1
#define HAVE_RBIGNUM_POSITIVE_P            1
#define HAVE_RBIGNUM_SIGN                  1
#define HAVE_RB_BIG2DBL                    1
#define HAVE_RB_BIG2LL                     1
#define HAVE_RB_BIG2LONG                   1
#define HAVE_RB_BIG2STR                    1
#define HAVE_RB_BIG2ULONG                  1

/* Class */
#define HAVE_RB_CALL_SUPER                 1
#define HAVE_RB_CLASS2NAME                 1
#define HAVE_RB_CLASS_INHERITED            1
#define HAVE_RB_CLASS_NAME                 1
#define HAVE_RB_CLASS_NEW                  1
#define HAVE_RB_CLASS_NEW_INSTANCE         1
#define HAVE_RB_CVAR_DEFINED               1
#define HAVE_RB_CVAR_GET                   1
#define HAVE_RB_CVAR_SET                   1
#define HAVE_RB_CV_GET                     1
#define HAVE_RB_CV_SET                     1
#define HAVE_RB_DEFINE_ATTR                1
#define HAVE_RB_DEFINE_CLASS_VARIABLE      1
#define HAVE_RB_INCLUDE_MODULE             1
#define HAVE_RB_PATH2CLASS                 1
#define HAVE_RB_PATH_TO_CLASS              1

/* Constants */
#define HAVE_RB_CARRAY                     1
#define HAVE_RB_CBIGNUM                    1
#define HAVE_RB_CCLASS                     1
#define HAVE_RB_CDATA                      1
#define HAVE_RB_CFALSECLASS                1
#define HAVE_RB_CFILE                      1
#define HAVE_RB_CFIXNUM                    1
#define HAVE_RB_CFLOAT                     1
#define HAVE_RB_CHASH                      1
#define HAVE_RB_CINTEGER                   1
#define HAVE_RB_CIO                        1
#define HAVE_RB_CMATCH                     1
#define HAVE_RB_CMODULE                    1
#define HAVE_RB_CNILCLASS                  1
#define HAVE_RB_CNUMERIC                   1
#define HAVE_RB_COBJECT                    1
#define HAVE_RB_CPROC                      1
#define HAVE_RB_CMETHOD                    1
#define HAVE_RB_CRANGE                     1
#define HAVE_RB_CREGEXP                    1
#define HAVE_RB_CSTRING                    1
#define HAVE_RB_CSTRUCT                    1
#define HAVE_RB_CSYMBOL                    1
#define HAVE_RB_CTHREAD                    1
#define HAVE_RB_CTRUECLASS                 1
#define HAVE_RB_CNUMERATOR                 1
#define HAVE_RB_EARGERROR                  1
#define HAVE_RB_EEOFERROR                  1
#define HAVE_RB_EEXCEPTION                 1
#define HAVE_RB_EFLOATDOMAINERROR          1
#define HAVE_RB_EINDEXERROR                1
#define HAVE_RB_EINTERRUPT                 1
#define HAVE_RB_EIOERROR                   1
#define HAVE_RB_ELOADERROR                 1
#define HAVE_RB_ELOCALJUMPERROR            1
#define HAVE_RB_EMATHDOMAINERROR           1
#define HAVE_RB_ENAMEERROR                 1
#define HAVE_RB_ENOMEMERROR                1
#define HAVE_RB_ENOMETHODERROR             1
#define HAVE_RB_ENOTIMPERROR               1
#define HAVE_RB_ERANGEERROR                1
#define HAVE_RB_EREGEXPERROR               1
#define HAVE_RB_ERUNTIMEERROR              1
#define HAVE_RB_ESCRIPTERROR               1
#define HAVE_RB_ESECURITYERROR             1
#define HAVE_RB_ESIGNAL                    1
#define HAVE_RB_ESTANDARDERROR             1
#define HAVE_RB_ESYNTAXERROR               1
#define HAVE_RB_ESYSSTACKERROR             1
#define HAVE_RB_ESYSTEMCALLERROR           1
#define HAVE_RB_ESYSTEMEXIT                1
#define HAVE_RB_ETHREADERROR               1
#define HAVE_RB_ETYPEERROR                 1
#define HAVE_RB_EZERODIVERROR              1
#define HAVE_RB_MCOMPARABLE                1
#define HAVE_RB_MENUMERABLE                1
#define HAVE_RB_MERRNO                     1
#define HAVE_RB_MKERNEL                    1

/* Data */
#define HAVE_DATA_WRAP_STRUCT              1
#define HAVE_RDATA                         1

/* Encoding */
#ifdef RUBY_VERSION_IS_1_9
#define HAVE_ENCODING_GET                  1
#define HAVE_ENCODING_SET                  1
#define HAVE_ENC_CODERANGE_ASCIIONLY       1

#define HAVE_RB_ASCII8BIT_ENCODING         1
#define HAVE_RB_ASCII8BIT_ENCINDEX         1
#define HAVE_RB_USASCII_ENCODING           1
#define HAVE_RB_USASCII_ENCINDEX           1
#define HAVE_RB_UTF8_ENCODING              1
#define HAVE_RB_UTF8_ENCINDEX              1
#define HAVE_RB_LOCALE_ENCODING            1
#define HAVE_RB_LOCALE_ENCINDEX            1
#define HAVE_RB_FILESYSTEM_ENCODING        1
#define HAVE_RB_FILESYSTEM_ENCINDEX        1

#define HAVE_RB_DEFAULT_INTERNAL_ENCODING  1
#define HAVE_RB_DEFAULT_EXTERNAL_ENCODING  1

#define HAVE_RB_ENCDB_ALIAS                1
#define HAVE_RB_ENC_ASSOCIATE              1
#define HAVE_RB_ENC_ASSOCIATE_INDEX        1
#define HAVE_RB_ENC_COMPATIBLE             1
#define HAVE_RB_ENC_COPY                   1
#define HAVE_RB_ENC_FIND                   1
#define HAVE_RB_ENC_FIND_INDEX             1
#define HAVE_RB_ENC_FROM_ENCODING          1
#define HAVE_RB_ENC_FROM_INDEX             1
#define HAVE_RB_ENC_GET                    1
#define HAVE_RB_ENC_GET_INDEX              1
#define HAVE_RB_ENC_SET_INDEX              1
#define HAVE_RB_ENC_STR_CODERANGE          1
#define HAVE_RB_ENC_STR_NEW                1
#define HAVE_RB_ENC_TO_INDEX               1
#define HAVE_RB_OBJ_ENCODING               1

#define HAVE_RB_STR_ENCODE                 1
#define HAVE_RB_STR_NEW_CSTR               1
#define HAVE_RB_USASCII_STR_NEW            1
#define HAVE_RB_USASCII_STR_NEW_CSTR       1
#define HAVE_RB_EXTERNAL_STR_NEW           1
#define HAVE_RB_EXTERNAL_STR_NEW_CSTR      1
#define HAVE_RB_EXTERNAL_STR_NEW_WITH_ENC  1

#define HAVE_RB_TO_ENCODING                1
#define HAVE_RB_TO_ENCODING_INDEX          1
#define HAVE_RB_ENC_NTH                    1

#define HAVE_RB_EENCCOMPATERROR            1

#define HAVE_RB_MWAITREADABLE              1
#define HAVE_RB_MWAITWRITABLE              1

#define HAVE_RSTRING_LENINT                1
#define HAVE_TIMET2NUM                     1

#ifdef RUBY_VERSION_IS_1_9_3
#define HAVE_RB_CLASS_SUPERCLASS           1
#endif
#define HAVE_RB_LONG2INT                   1
#endif

#define HAVE_RB_ITER_BREAK                 1
#define HAVE_RB_SOURCEFILE                 1
#define HAVE_RB_SOURCELINE                 1
#define HAVE_RB_METHOD_BOUNDP              1

/* Enumerable */
#ifdef RUBY_VERSION_IS_1_8_7
#define HAVE_RB_ENUMERATORIZE              1
#endif

/* Exception */
#define HAVE_RB_EXC_NEW                    1
#define HAVE_RB_EXC_NEW2                   1
#define HAVE_RB_EXC_NEW3                   1
#define HAVE_RB_EXC_RAISE                  1
#define HAVE_RB_SET_ERRINFO                1

/* File */
#define HAVE_RB_FILE_OPEN                  1
#ifdef RUBY_VERSION_IS_1_9
#define HAVE_RB_FILE_OPEN_STR              1
#define HAVE_FILEPATHVALUE                 1
#endif

/* Float */
#define HAVE_RB_FLOAT_NEW                  1
#define HAVE_RB_RFLOAT                     1
#define HAVE_RFLOAT                        1
#define HAVE_RFLOAT_VALUE                  1

/* Globals */
#define HAVE_RB_DEFAULT_RS                 1
#define HAVE_RB_DEFINE_HOOKED_VARIABLE     1
#define HAVE_RB_DEFINE_READONLY_VARIABLE   1
#define HAVE_RB_DEFINE_VARIABLE            1
#define HAVE_RB_F_GLOBAL_VARIABLES         1
#define HAVE_RB_GV_GET                     1
#define HAVE_RB_GV_SET                     1
#define HAVE_RB_RS                         1
#define HAVE_RB_OUTPUT_RS                  1
#define HAVE_RB_OUTPUT_FS                  1
#define HAVE_RB_SET_KCODE                  1

#define HAVE_RB_LASTLINE_SET               1
#define HAVE_RB_LASTLINE_GET               1

/* Hash */
#define HAVE_RB_HASH                       1
#define HAVE_RB_HASH_AREF                  1
#define HAVE_RB_HASH_ASET                  1
#define HAVE_RB_HASH_DELETE                1
#define HAVE_RB_HASH_DELETE_IF             1
#define HAVE_RB_HASH_FOREACH               1
#define HAVE_RB_HASH_LOOKUP                1
#define HAVE_RB_HASH_NEW                   1
#define HAVE_RB_HASH_SIZE                  1

/* IO */
#define HAVE_GET_OPEN_FILE                 1
#define HAVE_RB_IO_ADDSTR                  1
#define HAVE_RB_IO_CHECK_CLOSED            1
#define HAVE_RB_IO_CHECK_READABLE          1
#define HAVE_RB_IO_CHECK_WRITABLE          1
#define HAVE_RB_IO_CLOSE                   1
#define HAVE_RB_IO_PRINT                   1
#define HAVE_RB_IO_PRINTF                  1
#define HAVE_RB_IO_PUTS                    1
#define HAVE_RB_IO_WAIT_READABLE           1
#define HAVE_RB_IO_WAIT_WRITABLE           1
#define HAVE_RB_IO_WRITE                   1
#define HAVE_RB_IO_BINMODE                 1

#define HAVE_RB_THREAD_FD_WRITABLE         1
#define HAVE_RB_THREAD_WAIT_FD             1

/* Kernel */
#define HAVE_RB_BLOCK_GIVEN_P              1
#define HAVE_RB_BLOCK_PROC                 1
#define HAVE_RB_BLOCK_CALL                 1
#define HAVE_RB_ENSURE                     1
#define HAVE_RB_EVAL_STRING                1
#define HAVE_RB_EXEC_RECURSIVE             1
#define HAVE_RB_F_SPRINTF                  1
#define HAVE_RB_NEED_BLOCK                 1
#define HAVE_RB_RAISE                      1
#define HAVE_RB_RESCUE                     1
#define HAVE_RB_RESCUE2                    1
#define HAVE_RB_SET_END_PROC               1
#define HAVE_RB_SYS_FAIL                   1
#ifdef RUBY_VERSION_IS_1_9_3
#define HAVE_RB_SYSERR_FAIL                1
#define HAVE_RB_MAKE_BACKTRACE             1
#endif
#define HAVE_RB_THROW                      1
#define HAVE_RB_CATCH                      1
#ifdef RUBY_VERSION_IS_1_9
#define HAVE_RB_THROW_OBJ                  1
#define HAVE_RB_CATCH_OBJ                  1
#endif
#define HAVE_RB_WARN                       1
#define HAVE_RB_YIELD                      1
#define HAVE_RB_YIELD_SPLAT                1
#define HAVE_RB_YIELD_VALUES               1

/* GC */
#define HAVE_RB_GC_REGISTER_ADDRESS        1
#define HAVE_RB_GC_ENABLE                  1
#define HAVE_RB_GC_DISABLE                 1

/* Marshal */
#define HAVE_RB_MARSHAL_DUMP               1
#define HAVE_RB_MARSHAL_LOAD               1

/* Module */
#define HAVE_RB_ALIAS                      1
#define HAVE_RB_CONST_DEFINED              1
#define HAVE_RB_CONST_DEFINED_AT           1
#define HAVE_RB_CONST_GET                  1
#define HAVE_RB_CONST_GET_AT               1
#define HAVE_RB_CONST_GET_FROM             1
#define HAVE_RB_CONST_SET                  1
#define HAVE_RB_DEFINE_ALIAS               1
#define HAVE_RB_DEFINE_CLASS_UNDER         1
#define HAVE_RB_DEFINE_CONST               1
#define HAVE_RB_DEFINE_GLOBAL_CONST        1
#define HAVE_RB_DEFINE_GLOBAL_FUNCTION     1
#define HAVE_RB_DEFINE_METHOD              1
#define HAVE_RB_DEFINE_MODULE_FUNCTION     1
#define HAVE_RB_DEFINE_MODULE_UNDER        1
#define HAVE_RB_DEFINE_PRIVATE_METHOD      1
#define HAVE_RB_DEFINE_PROTECTED_METHOD    1
#define HAVE_RB_DEFINE_SINGLETON_METHOD    1
#define HAVE_RB_UNDEF                      1
#define HAVE_RB_UNDEF_METHOD               1

/* Numeric */
#define HAVE_NUM2CHR                       1
#define HAVE_RB_CMPINT                     1
#define HAVE_RB_INT2INUM                   1
#define HAVE_RB_INTEGER                    1
#define HAVE_RB_LL2INUM                    1
#define HAVE_RB_NUM2DBL                    1
#define HAVE_RB_NUM2LONG                   1
#define HAVE_RB_NUM2ULONG                  1
#define HAVE_RB_NUM_COERCE_BIN             1
#define HAVE_RB_NUM_COERCE_CMP             1
#define HAVE_RB_NUM_COERCE_RELOP           1
#define HAVE_RB_NUM_ZERODIV                1

/* Object */
#define HAVE_OBJ_TAINT                     1
#define HAVE_OBJ_TAINTED                   1
#define HAVE_OBJ_INFECT                    1
#define HAVE_RB_ANY_TO_S                   1
#define HAVE_RB_ATTR_GET                   1
#define HAVE_RB_OBJ_INSTANCE_VARIABLES	   1
#define HAVE_RB_CHECK_ARRAY_TYPE           1
#define HAVE_RB_CHECK_CONVERT_TYPE         1
#ifdef RUBY_VERSION_IS_1_8_7
#define HAVE_RB_CHECK_TO_INTEGER           1
#endif
#define HAVE_RB_CHECK_FROZEN               1
#define HAVE_RB_CHECK_STRING_TYPE          1
#define HAVE_RB_CLASS_OF                   1
#define HAVE_RB_CONVERT_TYPE               1
#define HAVE_RB_EQUAL                      1
#define HAVE_RB_CLASS_INHERITED_P          1
#define HAVE_RB_EXTEND_OBJECT              1
#define HAVE_RB_INSPECT                    1
#define HAVE_RB_IVAR_DEFINED               1
#define HAVE_RB_IVAR_GET                   1
#define HAVE_RB_IVAR_SET                   1
#define HAVE_RB_IV_GET                     1
#define HAVE_RB_IV_SET                     1
#define HAVE_RB_OBJ_ALLOC                  1
#define HAVE_RB_OBJ_CALL_INIT              1
#define HAVE_RB_OBJ_CLASSNAME              1
#define HAVE_RB_OBJ_DUP                    1
#define HAVE_RB_OBJ_FREEZE                 1
#define HAVE_RB_OBJ_FROZEN_P               1
#define HAVE_RB_OBJ_ID                     1
#define HAVE_RB_OBJ_INSTANCE_EVAL          1
#define HAVE_RB_OBJ_IS_INSTANCE_OF         1
#define HAVE_RB_OBJ_IS_KIND_OF             1
#define HAVE_RB_OBJ_TAINT                  1
#define HAVE_RB_OBJ_METHOD                 1
#define HAVE_RB_REQUIRE                    1
#define HAVE_RB_RESPOND_TO                 1
#define HAVE_RB_OBJ_RESPOND_TO             1
#define HAVE_RB_SPECIAL_CONST_P            1
#define HAVE_RB_TO_ID                      1
#define HAVE_RB_TO_INT                     1
#define HAVE_RTEST                         1
#define HAVE_TYPE                          1
#ifdef RUBY_VERSION_IS_1_9
#define HAVE_RB_TYPE_P                     1
#endif
#define HAVE_BUILTIN_TYPE                  1

/* Proc */
#define HAVE_RB_PROC_NEW                   1

/* Range */
#define HAVE_RB_RANGE_NEW                  1

/* Rational */
#define HAVE_RB_RATIONAL                   1
#define HAVE_RB_RATIONAL1                  1
#define HAVE_RB_RATIONAL2                  1

/* Regexp */
#define HAVE_RB_BACKREF_GET                1
#define HAVE_RB_REG_MATCH                  1
#define HAVE_RB_REG_NEW                    1
#define HAVE_RB_REG_NTH_MATCH              1
#define HAVE_RB_REG_OPTIONS                1
#define HAVE_RB_REG_REGCOMP                1

/* Safe */
#define HAVE_RB_SAFE_LEVEL                 1
#define HAVE_RB_SECURE                     1

/* String */
#define HAVE_RB_CSTR2INUM                  1
#define HAVE_RB_CSTR_TO_INUM               1
#define HAVE_RB_STR2CSTR                   1
#define HAVE_RB_STR2INUM                   1
#define HAVE_RB_STR_APPEND                 1
#define HAVE_RB_STR_BUF_CAT                1
#define HAVE_RB_STR_BUF_NEW                1
#define HAVE_RB_STR_BUF_NEW2               1
#define HAVE_RB_STR_CAT                    1
#define HAVE_RB_STR_CAT2                   1
#define HAVE_RB_STR_CMP                    1
#define HAVE_RB_STR_DUP                    1
#define HAVE_RB_STR_FLUSH                  1
#define HAVE_RB_STR_FREEZE                 1
#define HAVE_RB_STR_HASH                   1
#define HAVE_RB_STR_INSPECT                1
#define HAVE_RB_STR_INTERN                 1
#define HAVE_RB_STR_LEN                    1
#define HAVE_RB_STR_NEW                    1
#define HAVE_RB_STR_NEW2                   1
#define HAVE_RB_STR_NEW3                   1
#define HAVE_RB_STR_NEW4                   1
#define HAVE_RB_STR_PLUS                   1
#define HAVE_RB_STR_PTR                    1
#define HAVE_RB_STR_PTR_READONLY           1
#define HAVE_RB_STR_RESIZE                 1
#define HAVE_RB_STR_SET_LEN                1
#define HAVE_RB_STR_SPLIT                  1
#define HAVE_RB_STR_SUBSTR                 1
#define HAVE_RB_STR_TO_STR                 1
#define HAVE_RSTRING                       1
#define HAVE_RSTRING_LEN                   1
#define HAVE_RSTRING_PTR                   1
#define HAVE_STR2CSTR                      1
#define HAVE_STRINGVALUE                   1

#ifdef RUBY_VERSION_IS_1_9
#define HAVE_RB_SPRINTF                    1
#define HAVE_RB_LOCALE_STR_NEW             1
#define HAVE_RB_LOCALE_STR_NEW_CSTR        1
#define HAVE_RB_STR_CONV_ENC               1
#define HAVE_RB_STR_CONV_ENC_OPTS          1
#define HAVE_RB_STR_EXPORT                 1
#define HAVE_RB_STR_EXPORT_LOCALE          1
#define HAVE_RB_STR_LENGTH                 1
#define HAVE_RB_STR_EQUAL                  1
#define HAVE_RB_STR_SUBSEQ                 1
#endif

/* Struct */
#define HAVE_RB_STRUCT_AREF                1
#define HAVE_RB_STRUCT_ASET                1
#define HAVE_RB_STRUCT_DEFINE              1
#define HAVE_RB_STRUCT_NEW                 1
#define HAVE_RB_STRUCT_GETMEMBER           1

/* Symbol */
#define HAVE_RB_ID2NAME                    1
#ifdef RUBY_VERSION_IS_1_9
#define HAVE_RB_ID2STR                     1
#endif
#define HAVE_RB_INTERN                     1
#define HAVE_RB_IS_CLASS_ID                1
#define HAVE_RB_IS_CONST_ID                1
#define HAVE_RB_IS_INSTANCE_ID             1

/* Thread */
#define HAVE_RB_THREAD_ALONE               1
#define HAVE_RB_THREAD_BLOCKING_REGION     1
#define HAVE_RB_THREAD_CURRENT             1
#define HAVE_RB_THREAD_LOCAL_AREF          1
#define HAVE_RB_THREAD_LOCAL_ASET          1
#define HAVE_RB_THREAD_SELECT              1
#define HAVE_RB_THREAD_WAIT_FOR            1
#define HAVE_RB_THREAD_WAKEUP              1
#define HAVE_RB_THREAD_CREATE              1

/* Time */
#define HAVE_RB_TIME_NEW                   1
#ifdef RUBY_VERSION_IS_1_9
#define HAVE_RB_TIME_NANO_NEW              1
#endif

/* Util */
#define HAVE_RB_SCAN_ARGS                  1
#define HAVE_RUBY_SETENV                   1
#define HAVE_RUBY_STRDUP                   1

/* Now, create the differential set. The format of the preprocessor directives
 * is significant. The alternative implementations should define RUBY because
 * some extensions depend on that. But only one alternative implementation
 * macro should be defined at a time. The conditional is structured so that if
 * no alternative implementation is defined then MRI is assumed and "mri.h"
 * will be included.
 */

#if defined(RUBINIUS)
#include "rubinius.h"
#elif defined(JRUBY)
#include "jruby.h"
#else /* MRI */
#include "mri.h"
#endif

#endif
