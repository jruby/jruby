#include "ruby.h"
#include "rubyspec.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_SAFE_LEVEL
static VALUE safe_specs_safe_level(VALUE self) {
  int sl = rb_safe_level();
  return INT2FIX(sl);
}
#endif

#ifdef HAVE_RB_SECURE
static VALUE safe_specs_secure(VALUE self, VALUE arg) {
  rb_secure(FIX2INT(arg));
  return Qnil;
}
#endif

void Init_safe_spec() {
  VALUE cls;
  cls = rb_define_class("CApiSafeSpecs", rb_cObject);

#ifdef HAVE_RB_SAFE_LEVEL
  rb_define_method(cls, "rb_safe_level", safe_specs_safe_level, 0);
#endif

#ifdef HAVE_RB_SECURE
  rb_define_method(cls, "rb_secure", safe_specs_secure, 1);
#endif
}

#ifdef __cplusplus
}
#endif
