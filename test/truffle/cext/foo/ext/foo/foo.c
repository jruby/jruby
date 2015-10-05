#include <ruby.h>

#include "add.h"

void Init_foo() {
  VALUE Foo = rb_define_module("Foo");
  rb_define_method(Foo, "add", add, 2);
}
