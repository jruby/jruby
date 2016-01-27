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
example "'こにちわ'.length", 4

example "'abc'.bytesize", 3
example "'こにちわ'.bytesize", 12

tagged_example "'abc' == 'abc'", true # seems to fail sometimes
example "x = 'abc'; x == x", true
example "x = 'abc'; x == x.dup", true

example "'abc'.ascii_only?", true
example "'こにちわ'.ascii_only?", false

example "''.ascii?", false
example "'abc'.ascii?", true

example "'abc'.valid_encoding?", true
example "'こにちわ'.valid_encoding?", true

example "''.empty?", true
example "'abc'.empty?", false
example "'こにちわ'.empty?", false
