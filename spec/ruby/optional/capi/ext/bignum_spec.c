#include "ruby.h"
#include "rubyspec.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_BIG2DBL
static VALUE bignum_spec_rb_big2dbl(VALUE self, VALUE num) {
  return rb_float_new(rb_big2dbl(num));
}
#endif

#ifdef HAVE_RB_DBL2BIG
static VALUE bignum_spec_rb_dbl2big(VALUE self, VALUE num) {
  double dnum = NUM2DBL(num);

  return rb_dbl2big(dnum);
}
#endif

#ifdef HAVE_RB_BIG2LL
static VALUE bignum_spec_rb_big2ll(VALUE self, VALUE num) {
  return rb_ll2inum(rb_big2ll(num));
}
#endif

#ifdef HAVE_RB_BIG2LONG
static VALUE bignum_spec_rb_big2long(VALUE self, VALUE num) {
  return LONG2NUM(rb_big2long(num));
}
#endif

#ifdef HAVE_RB_BIG2STR
static VALUE bignum_spec_rb_big2str(VALUE self, VALUE num, VALUE base) {
  return rb_big2str(num, FIX2INT(base));
}
#endif

#ifdef HAVE_RB_BIG2ULONG
static VALUE bignum_spec_rb_big2ulong(VALUE self, VALUE num) {
  return ULONG2NUM(rb_big2ulong(num));
}
#endif

#ifdef HAVE_RB_BIG_CMP
static VALUE bignum_spec_rb_big_cmp(VALUE self, VALUE x, VALUE y) {
  return rb_big_cmp(x, y);
}
#endif

#ifdef HAVE_RB_BIG_PACK
static VALUE bignum_spec_rb_big_pack(VALUE self, VALUE val) {
  unsigned long buff;

  rb_big_pack(val, &buff, 1);

  return ULONG2NUM(buff);
}
#endif

#ifdef HAVE_RBIGNUM_SIGN
static VALUE bignum_spec_RBIGNUM_SIGN(VALUE self, VALUE num) {
  return RBIGNUM_SIGN(num) ? Qtrue : Qfalse;
}
#endif

#ifdef HAVE_RBIGNUM_POSITIVE_P
static VALUE bignum_spec_RBIGNUM_POSITIVE_P(VALUE self, VALUE num) {
  return RBIGNUM_POSITIVE_P(num) ? Qtrue : Qfalse;
}
#endif

#ifdef HAVE_RBIGNUM_NEGATIVE_P
static VALUE bignum_spec_RBIGNUM_NEGATIVE_P(VALUE self, VALUE num) {
  return RBIGNUM_NEGATIVE_P(num) ? Qtrue : Qfalse;
}
#endif

#ifdef HAVE_RBIGNUM_LEN
static VALUE bignum_spec_RBIGNUM_LEN(VALUE self, VALUE num) {
  return LONG2NUM(RBIGNUM_LEN(num));
}
#endif

void Init_bignum_spec() {
  VALUE cls;
  cls = rb_define_class("CApiBignumSpecs", rb_cObject);

#ifdef HAVE_RB_BIG2DBL
  rb_define_method(cls, "rb_big2dbl", bignum_spec_rb_big2dbl, 1);
#endif

#ifdef HAVE_RB_DBL2BIG
  rb_define_method(cls, "rb_dbl2big", bignum_spec_rb_dbl2big, 1);
#endif

#ifdef HAVE_RB_BIG2LL
  rb_define_method(cls, "rb_big2ll", bignum_spec_rb_big2ll, 1);
#endif

#ifdef HAVE_RB_BIG2LONG
  rb_define_method(cls, "rb_big2long", bignum_spec_rb_big2long, 1);
#endif

#ifdef HAVE_RB_BIG2STR
  rb_define_method(cls, "rb_big2str", bignum_spec_rb_big2str, 2);
#endif

#ifdef HAVE_RB_BIG2ULONG
  rb_define_method(cls, "rb_big2ulong", bignum_spec_rb_big2ulong, 1);
#endif

#ifdef HAVE_RB_BIG_CMP
  rb_define_method(cls, "rb_big_cmp", bignum_spec_rb_big_cmp, 2);
#endif

#ifdef HAVE_RB_BIG_PACK
  rb_define_method(cls, "rb_big_pack", bignum_spec_rb_big_pack, 1);
#endif

#ifdef HAVE_RBIGNUM_SIGN
  rb_define_method(cls, "RBIGNUM_SIGN", bignum_spec_RBIGNUM_SIGN, 1);
#endif

#ifdef HAVE_RBIGNUM_POSITIVE_P
  rb_define_method(cls, "RBIGNUM_POSITIVE_P", bignum_spec_RBIGNUM_POSITIVE_P, 1);
#endif

#ifdef HAVE_RBIGNUM_NEGATIVE_P
  rb_define_method(cls, "RBIGNUM_NEGATIVE_P", bignum_spec_RBIGNUM_NEGATIVE_P, 1);
#endif

#ifdef HAVE_RBIGNUM_LEN
  rb_define_method(cls, "RBIGNUM_LEN", bignum_spec_RBIGNUM_LEN, 1);
#endif
}

#ifdef __cplusplus
extern "C" {
#endif
