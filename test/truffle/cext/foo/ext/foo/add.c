#include <ruby.h>

#include "add.h"

VALUE add(VALUE self, VALUE a, VALUE b) {
  return INT2NUM(NUM2INT(a) + NUM2INT(b));
}
