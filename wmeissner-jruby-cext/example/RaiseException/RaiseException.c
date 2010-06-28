#include <stdio.h>
#include <ruby.h>

static VALUE HelloModule = Qnil;

static VALUE explode(VALUE self);

void
Init_raiseexception()
{
    HelloModule = rb_define_module("RaiseException");
    rb_define_module_function(HelloModule, "explode", explode, 0);
}

static VALUE
explode(VALUE self)
{
    rb_raise(rb_eRuntimeError, "Native module asplodes!");
}
