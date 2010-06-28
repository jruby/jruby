#include <stdio.h>
#include <ruby.h>

VALUE HelloModule = Qnil;

VALUE say_hello(int argc, VALUE* argv, VALUE recv);
VALUE get_hello(VALUE self);

void
Init_hello()
{
    HelloModule = rb_define_module("Hello");
    rb_define_method(HelloModule, "get_hello", get_hello, 0);
    rb_define_method(HelloModule, "say_hello", say_hello, -1);
}

VALUE
say_hello(int argc, VALUE* argv, VALUE recv)
{
    VALUE hello = Qnil;
    rb_scan_args(argc, argv, "01", &hello);
    printf("Printing via C stdout: [%s]\n", hello == Qnil ? ":nil" : RSTRING_PTR(hello));
    return Qnil;
}
VALUE
get_hello(VALUE self)
{
    return rb_str_new2("Hello, World");
}
