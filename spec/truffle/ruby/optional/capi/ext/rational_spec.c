#include "ruby.h"
#include "rubyspec.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_RATIONAL
static VALUE rational_spec_rb_Rational(VALUE self, VALUE num, VALUE den) {
  return rb_Rational(num, den);
}
#endif

#ifdef HAVE_RB_RATIONAL1
static VALUE rational_spec_rb_Rational1(VALUE self, VALUE num) {
  return rb_Rational1(num);
}
#endif

#ifdef HAVE_RB_RATIONAL2
static VALUE rational_spec_rb_Rational2(VALUE self, VALUE num, VALUE den) {
  return rb_Rational2(num, den);
}
#endif

void Init_rational_spec() {
  VALUE cls;
  cls = rb_define_class("CApiRationalSpecs", rb_cObject);

#ifdef HAVE_RB_RATIONAL
  rb_define_method(cls, "rb_Rational", rational_spec_rb_Rational, 2);
#endif

#ifdef HAVE_RB_RATIONAL1
  rb_define_method(cls, "rb_Rational1", rational_spec_rb_Rational1, 1);
#endif

#ifdef HAVE_RB_RATIONAL2
  rb_define_method(cls, "rb_Rational2", rational_spec_rb_Rational2, 2);
#endif
}

#ifdef __cplusplus
}
#endif
