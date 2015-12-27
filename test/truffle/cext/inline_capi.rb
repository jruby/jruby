# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

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
