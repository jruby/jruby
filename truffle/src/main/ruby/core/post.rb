# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

ARGV.push *Truffle::Primitive.original_argv

# We defined Psych at the top level becuase several things depend on its name.
# Here we fix that up and put it back into Truffle.

Truffle::Psych = Psych

class Object
  remove_const :Psych
end
