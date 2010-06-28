#include <stdio.h>
#include <ruby.h>

VALUE BenchClass = Qnil;

static VALUE addi2(VALUE self, VALUE i1, VALUE i2);


void
Init_mathbench()
{
    BenchClass = rb_define_class("MathBench", rb_cObject);
    rb_define_method(BenchClass, "addi2", addi2, 2);
}

VALUE
addi2(VALUE self, VALUE i1, VALUE i2)
{
    return INT2NUM(NUM2INT(i1) + NUM2INT(i2));
}

int 
ffi_addi2(int i1, int i2)
{
  return i1 + i2;
}

