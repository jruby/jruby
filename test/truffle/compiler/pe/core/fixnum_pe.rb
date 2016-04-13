# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

example "14", 14

example "14 + 2", 16
tagged_counter_example "14 + 0xfffffffffffffffffffffffffffffff" # Graal error
example "14 + 2.0", 16.0
counter_example "14 + rand"

example "14 * 2", 28
counter_example "14 * 0xfffffffffffffffffffffffffffffff"
example "14 * 2.0", 28.0
counter_example "14 * rand"

example "14 / 2", 7
example "14 / 0xfffffffffffffffffffffffffffffff", 0
example "14 / 2.0", 7.0
counter_example "14 / rand"

example "14 <=> 2", 1
