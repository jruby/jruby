# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# TODO CS 3-Feb-16 Not compliant with MRI - here as a regression test

require_relative 'backtraces'

def m1
  send :m2
end

def m2
  raise 'message'
end

check('send.backtrace') do
  m1
end
