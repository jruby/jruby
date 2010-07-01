#include "ruby.h"

static VALUE CString;

VALUE method_hello(VALUE self, VALUE str);

void Init_string(void)
{
  CString = rb_define_class("CString", rb_cObject);
  rb_define_method(CString, "hello", method_hello, 1);
}

VALUE method_hello(VALUE self, VALUE str)
{
  return rb_str_new2(RSTRING_PTR(str));
}

