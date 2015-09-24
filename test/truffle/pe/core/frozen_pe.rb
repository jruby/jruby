# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1


example "true.frozen?", true

example "false.frozen?", true

# int
example "3.frozen?", true

# long
example "(2**62).frozen?", true

# Bignum
example "(10 ** 100).frozen?", true

# float
example "3.5.frozen?", true

# double
example "(2**62).to_f.frozen?", true

example ":symbol.frozen?", true

example "nil.frozen?", true

example "'abc'.frozen?", false

example "'abc'.freeze.frozen?", true
