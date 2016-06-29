# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# TODO CS 8-Nov-15 fail due to a boundary and control flow diverge

# Kernel#binding
tagged example "x = 14; binding.local_variable_get(:x)", 14

# Proc#binding
tagged example "x = 14; p = Proc.new { }; p.binding.local_variable_get(:x)", 14

# set + get
tagged example "b = binding; b.local_variable_set(:x, 14); b.local_variable_get(:x)", 14

# get (2 levels)
tagged example "x = 14; y = nil; 1.times { y = binding.local_variable_get(:x) }; y", 14

# set (2 levels)
tagged example "x = 14; 1.times { binding.local_variable_set(:x, 15) }; x", 15

# get + set (2 levels)
tagged example "x = 14; y = nil; 1.times { binding.local_variable_set(:x, 15); y = binding.local_variable_get(:x) }; y", 15
