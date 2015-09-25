# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# why do the tagged_example fail? identity of boxing?

# Kernel#binding
tagged_example "x = 14; binding.local_variable_get(:x)", 14
example "x = 14; binding.local_variable_get(:x) * 2", 28

# Proc#binding
tagged_example "x = 14; p = Proc.new { }; p.binding.local_variable_get(:x)", 14
example "x = 14; p = Proc.new { }; p.binding.local_variable_get(:x) * 2", 28

# set + get
tagged_example "b = binding; b.local_variable_set(:x, 14); b.local_variable_get(:x)", 14
example "b = binding; b.local_variable_set(:x, 14); b.local_variable_get(:x) * 2", 28

# get (2 levels)
tagged_example "x = 14; y = nil; 1.times { y = binding.local_variable_get(:x) }; y", 14
example "x = 14; y = nil; 1.times { y = binding.local_variable_get(:x) }; y * 2", 28

# set (2 levels)
tagged_example "x = 14; 1.times { binding.local_variable_set(:x, 15) }; x", 15
example "x = 14; 1.times { binding.local_variable_set(:x, 15) }; x * 2", 30

# get + set (2 levels)
tagged_example "x = 14; y = nil; 1.times { binding.local_variable_set(:x, 15); y = binding.local_variable_get(:x) }; y", 15
example "x = 14; y = nil; 1.times { binding.local_variable_set(:x, 15); y = binding.local_variable_get(:x) }; y * 2", 30
