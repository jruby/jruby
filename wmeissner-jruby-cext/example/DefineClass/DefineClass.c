#include <stdio.h>
#include <ruby.h>

VALUE HelloModule = Qnil;
VALUE HelloClass = Qnil;

VALUE say_hello(VALUE self, VALUE hello);
VALUE get_hello(VALUE self);

void
Init_defineclass()
{
    HelloClass = rb_define_class("Hello", rb_cObject);
    rb_define_method(HelloClass, "get_hello", get_hello, 0);
    rb_define_method(HelloClass, "say_hello", say_hello, 1);
}

VALUE
say_hello(VALUE self, VALUE hello)
{
    return Qnil;
}
VALUE
get_hello(VALUE self)
{
    return rb_str_new2("Hello, World");
}
