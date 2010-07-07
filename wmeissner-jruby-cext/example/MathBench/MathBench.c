#include <stdio.h>
#include <ruby.h>

VALUE BenchClass = Qnil;

static VALUE addi2(VALUE self, VALUE i1, VALUE i2);
static VALUE addf2(VALUE self, VALUE f1, VALUE f2);


void
Init_mathbench()
{
    BenchClass = rb_define_class("MathBench", rb_cObject);
    rb_define_method(BenchClass, "addi2", addi2, 2);
    rb_define_method(BenchClass, "addf2", addf2, 2);
}

VALUE
addi2(VALUE self, VALUE i1, VALUE i2)
{
    return INT2NUM(NUM2INT(i1) + NUM2INT(i2));
}

VALUE
addf2(VALUE self, VALUE f1, VALUE f2)
{
    return rb_float_new(NUM2DBL(f1) + NUM2DBL(f2));
}

int 
ffi_addi2(int i1, int i2)
{
  return i1 + i2;
}

double 
ffi_addf2(double f1, double f2)
{
  return f1 + f2;
}

