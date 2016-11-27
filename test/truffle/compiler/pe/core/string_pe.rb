# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

simple_string = 'test'

example "Truffle::Ropes.create_simple_string.length", simple_string.length
example "Truffle::Ropes.create_simple_string.getbyte(0)", simple_string.getbyte(0)
example "Truffle::Ropes.create_simple_string.ord", simple_string.ord

example "'abc'.length", 3
example "'こにちわ'.length", 4

example "'abc'.bytesize", 3
example "'こにちわ'.bytesize", 12

example "'abc' == 'abc'", true
example "x = 'abc'; x == x", true
example "x = 'abc'; x == x.dup", true
example "x = 'abc'; 'abc' == x.dup", true

example "'A' == String.from_codepoint(65, Encoding::US_ASCII)", true
example "'A' == 65.chr", true
example "'A'.ord == 65", true

example "'aba'[0] == 'aca'[-1]", true

example "x = 'abc'; x == x.b", true

example "'abc'.ascii_only?", true
example "'こにちわ'.ascii_only?", false

example "'abc'.valid_encoding?", true
example "'こにちわ'.valid_encoding?", true

example "''.empty?", true
example "'abc'.empty?", false
example "'こにちわ'.empty?", false

example "x = 'abc'; y = 'xyz'; x.replace(y) == y", true

example "'abc'.getbyte(0) == 97", true
example "'abc'.getbyte(-1) == 99", true
example "'abc'.getbyte(10_000) == nil", true

example "14.to_s.length", 2
counter example "14.to_s.getbyte(0)" # Doesn't work becuase the bytes are only populated on demand and so aren't constant
