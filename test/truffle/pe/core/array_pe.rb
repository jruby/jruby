# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

example "[3, 1, 2][1]"

# fails because of a call to <=> that is not inlined
tagged_example "[3, 1, 2].sort[1]"

# why does this fail but the next one work?
tagged_example "[14].pack('C').getbyte(0)"

example "[14].pack('C').getbyte(0) * 2"
