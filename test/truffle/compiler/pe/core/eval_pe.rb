# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

example "eval('14')", 14

example "eval('14 + 2')", 16

example "eval('[1, 2, 3]')[1]", 2

# TODO (nirvdrum 27-Jan-16): This started timing out after moving some things to ropes. It's probably a matter of caching that generally needs to be fixed.
tagged_example "eval([1, 2, 3].inspect)[1]", 2

counter_example "eval(rand.to_s)"
