# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

simple_string = 'test'

example "Truffle::Primitive.create_simple_string.length", simple_string.length
example "Truffle::Primitive.create_simple_string.getbyte(0)", simple_string.getbyte(0)


example "'abc'.length", 3
example "'abc' == 'abc'", true
