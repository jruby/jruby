# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

example "[3, 1, 2][1]", 1

example "[3, 1, 2].sort[1]", 2

# I think this fails due to our iterative partial escape issue
tagged_example "[14].pack('C').getbyte(0)", 14
