# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1


example "true.frozen?"

example "false.frozen?"

# int
example "3.frozen?"

# long
example "(2**62).frozen?"

# Bignum
example "(10 ** 100).frozen?"

# float
example "3.5.frozen?"

# double
example "(2**62).to_f.frozen?"

example ":symbol.frozen?"

example "nil.frozen?"

example "'abc'.frozen?"

example "'abc'.freeze.frozen?"
