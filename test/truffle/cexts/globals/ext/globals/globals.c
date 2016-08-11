#include <ruby.h>

VALUE global;
static VALUE static_global;
extern VALUE extern_global;

VALUE set_global(VALUE self, VALUE value) {
  global = value;
  return Qnil;
}

VALUE get_global(VALUE self) {
  return global;
}

VALUE set_static_global(VALUE self, VALUE value) {
  static_global = value;
  return Qnil;
}

VALUE get_static_global(VALUE self) {
  return static_global;
}

VALUE set_extern_global(VALUE self, VALUE value) {
  extern_global = value;
  return Qnil;
}

VALUE get_extern_global(VALUE self) {
  return extern_global;
}

void Init_globals() {
  VALUE module = rb_define_module("GlobalsExtension");
  rb_define_module_function(module, "global=", &set_global, 1);
  rb_define_module_function(module, "global", &get_global, 0);
  rb_define_module_function(module, "static_global=", &set_static_global, 1);
  rb_define_module_function(module, "static_global", &get_static_global, 0);
  rb_define_module_function(module, "extern_global=", &set_extern_global, 1);
  rb_define_module_function(module, "extern_global", &get_extern_global, 0);
}
