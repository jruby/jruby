#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Calculates the approximate minimum heap sized needed to run hello world.
# Not run it fast - just run it at all.

# For example:
# $ ruby test/truffle/memory/minimum-heap.rb bin/jruby "-X+T -e 'puts 14'"

tolerance = 5
max_iterations = 100

lower = 0
upper = 4 * 1024
iterations = 0

while upper - lower > tolerance && iterations < max_iterations
  mid = lower + (upper - lower) / 2

  print "trying #{mid}m... "
  can_run = !(`#{ARGV[0]} -J-Xmx#{mid}m #{ARGV[1]} 2>&1`.include? 'GC overhead limit exceeded')

  if can_run
    puts "yes"
    upper = mid
  else
    puts "no"
    lower = mid
  end
  
  iterations += 1
end

puts "minimum heap: #{upper}m"
