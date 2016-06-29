# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

example "true.frozen?", true
example "nil.frozen?", true

# int
example "3.frozen?", true

# long
example "(2**62).frozen?", true

# Bignum
example "(10 ** 100).frozen?", true

# double
example "3.5.frozen?", true

# Symbols are always frozen
example ":symbol.frozen?", true

# Object
example "Object.new.frozen?", false
example "Object.new.freeze.frozen?", true
