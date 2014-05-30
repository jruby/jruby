#include "ruby.h"
#include "rubyspec.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_RB_RANGE_NEW
VALUE range_spec_rb_range_new(int argc, VALUE* argv, VALUE self) {
  int exclude_end = 0;
  if(argc == 3) {
    exclude_end = RTEST(argv[2]);
  }
  return rb_range_new(argv[0], argv[1], exclude_end);
}
#endif

void Init_range_spec() {
  VALUE cls;
  cls = rb_define_class("CApiRangeSpecs", rb_cObject);

#ifdef HAVE_RB_RANGE_NEW
  rb_define_method(cls, "rb_range_new", range_spec_rb_range_new, -1);
#endif
}

#ifdef __cplusplus
}
#endif
