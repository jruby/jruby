#include "ruby.h"

static VALUE CString;

VALUE method_hello(VALUE self, VALUE str);

void Init_string(void)
{
  CString = rb_define_class("CString", rb_cObject);
  rb_define_method(CString, "hello", method_hello, 1);
  rb_const_set(CString, rb_intern("fubar"), Qtrue);
}

VALUE method_hello(VALUE self, VALUE str)
{
  printf("In C, this string has the length: %d\n", (int)RSTRING_LEN(str));
  printf("string address=%p\n", RSTRING_PTR(str));
  printf("fubar=%llx\n", (long long) rb_const_get(self, rb_intern("fubar")));
  return rb_str_new2(RSTRING_PTR(str));
}

