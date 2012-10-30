#include "ruby.h"
#include "rubyspec.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_GC_REGISTER_ADDRESS
VALUE registered_tagged_value;
VALUE registered_reference_value;

static VALUE registered_tagged_address(VALUE self) {
  return registered_tagged_value;
}

static VALUE registered_reference_address(VALUE self) {
  return registered_reference_value;
}
#endif

void Init_gc_spec() {
  VALUE cls;
  cls = rb_define_class("CApiGCSpecs", rb_cObject);

#ifdef HAVE_RB_GC_REGISTER_ADDRESS
  registered_tagged_value    = INT2NUM(10);
  registered_reference_value = rb_str_new2("Globally registered data");

  rb_gc_register_address(&registered_tagged_value);
  rb_gc_register_address(&registered_reference_value);

  rb_define_method(cls, "registered_tagged_address", registered_tagged_address, 0);
  rb_define_method(cls, "registered_reference_address", registered_reference_address, 0);
#endif
}

#ifdef __cplusplus
}
#endif
