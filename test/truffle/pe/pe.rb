# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# This file relies on some implementation details of JRuby+Truffle and Truffle,
# so be careful as you edit. Every block that you pass to example must be
# unique - so you can't always build up examples by running in a loop or using
# helper method. truffle_assert_constant looks like a method but is replaced
# in the parser with a specific node.

$failures = 0

def example
  1_000_000.times do
    yield
  end

  print "."
rescue RubyTruffleError
  $failures += 1
  print "E"
end

def counter_example
  1_000_000.times do
    yield
  end

  $failures += 1
  print "E"
rescue RubyTruffleError
  print "."
end

# Two simple tests to check we're working

example { truffle_assert_constant 14 }
counter_example { truffle_assert_constant rand }

# Tests organised by class

$: << File.expand_path('..', __FILE__)

require "core/truefalse_pe.rb"
require "core/fixnum_pe.rb"
require "core/float_pe.rb"
require "core/symbol_pe.rb"

print "\n"

exit 1 unless $failures.zero?
