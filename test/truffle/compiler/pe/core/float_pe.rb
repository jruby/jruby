# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

example "14.2", 14.2

example "14.0 + 2", 16.0
counter example "14.0 + 0xfffffffffffffffffffffffffffffff"
example "14.0 + 2.0", 16.0
counter example "14.0 + rand"

example "14.0 * 2", 28.0
counter example "14.0 * 0xfffffffffffffffffffffffffffffff"
example "14.0 * 2.0", 28.0
counter example "14.0 * rand"

example "14.0 / 2", 7.0
counter example "14.0 / 0xfffffffffffffffffffffffffffffff"
example "14.0 / 2.0", 7.0
counter example "14.0 / rand"
