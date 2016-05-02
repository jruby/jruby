#include <ruby.h>

VALUE add(VALUE self, VALUE a, VALUE b) {
  return INT2NUM(NUM2INT(a) + NUM2INT(b));
}

void Init_method() {
  VALUE module = rb_define_module("MethodExtension");
  rb_define_module_function(module, "add", &add, 2);
}
