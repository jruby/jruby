p Truffle::CExt.supported?

Truffle::CExt.inline %{
  VALUE add(VALUE self, VALUE a, VALUE b) {
    return INT2NUM(NUM2INT(a) + NUM2INT(b));
  }
}, %{
  VALUE Test = rb_define_module("Test");
  rb_define_method(Test, "add", add, 2);
}

p Test.add(14, 2)
