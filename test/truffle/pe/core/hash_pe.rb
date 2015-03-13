# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# fails because of a call to eql? that is not inlined
tagged_example "({a: 0, b: 1, c: 2})[:b]"

tagged_example "({a: 0, b: 1, c: 2}).map{ |k, v| v }[0]"
