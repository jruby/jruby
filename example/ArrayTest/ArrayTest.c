#include <stdio.h>
#include <ruby.h>

static VALUE ArrayModule = Qnil;

static VALUE array_new_size(VALUE self, VALUE size);
static VALUE array_new_elements(VALUE self, VALUE elements);
static VALUE array_new_empty(VALUE self);
static VALUE array_unshift(VALUE self, VALUE array, VALUE value);

void
Init_array()
{
    ArrayModule = rb_define_module("ArrayTest");
    rb_define_module_function(ArrayModule, "new_size", array_new_size, 1);
    rb_define_module_function(ArrayModule, "new_elements", array_new_elements, 1);
    rb_define_module_function(ArrayModule, "new_empty", array_new_empty, 0);
    rb_define_module_function(ArrayModule, "unshift", array_unshift, 2);
}

static VALUE 
array_new_size(VALUE self, VALUE size)
{
    return rb_ary_new2(NUM2INT(size));
}
static VALUE 
array_new_elements(VALUE self, VALUE elements)
{
    return rb_Array(elements);
}
static VALUE 
array_new_empty(VALUE self) 
{
    return rb_ary_new();
}

static VALUE 
array_unshift(VALUE self, VALUE array, VALUE value)
{
    return rb_ary_unshift(array, value);
}
