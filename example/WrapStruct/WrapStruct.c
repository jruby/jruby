#include <stdio.h>
#include <string.h>
#include <ruby.h>

VALUE HelloModule = Qnil;
VALUE HelloClass = Qnil;


struct Hello {
  VALUE str;
};

VALUE say_hello(VALUE self, VALUE hello);
VALUE get_hello(VALUE self);
static VALUE hello_s_allocate(VALUE);
static void hello_mark(struct Hello *);
static void hello_free(struct Hello *);


void
Init_wrapstruct()
{
    HelloClass = rb_define_class("Hello", rb_cObject);
    rb_define_alloc_func(HelloClass, hello_s_allocate);
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
    struct Hello* h = DATA_PTR(self);
    return h->str;
}

static VALUE
hello_s_allocate(VALUE klass)
{
    struct Hello* h;
    VALUE obj = Data_Make_Struct(klass, struct Hello, hello_mark, hello_free, h);
    h->str = rb_str_new_cstr("Hello, World");
    return obj;
}

static void
hello_mark(struct Hello* h)
{
//    printf("marking instance of Hello=%p\n", h);
    rb_gc_mark(h->str);
}

static void 
hello_free(struct Hello *h)
{
    xfree(h);
}

void*
ffi_get_hello(void)
{
    return "Hello, World";
}
