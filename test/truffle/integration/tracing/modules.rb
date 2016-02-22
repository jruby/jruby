# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# TODO CS 19-Feb-16 Not compliant with MRI - here as a regression test

require_relative 'tracing'

def add(a, b)
  a + b
end

set_trace_func $trace_proc

module Foo
end

class Bar
end

set_trace_func nil

check('modules.trace')
