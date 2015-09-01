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

COMMAND = ARGV[0]
OPTIONS = ARGV[1]

TOLERANCE = 1
UPPER_FACTOR = 4

def can_run(heap)
  print "trying #{heap} MB... "

  output = `#{COMMAND} -J-Xmx#{heap}m #{OPTIONS} 2>&1`
  can_run = !output.include?('OutOfMemoryError')

  if can_run
    puts "yes"
  else
    puts "no"
  end

  can_run
end

puts "looking for an upper bound..."

lower = 0
upper = 1

while !can_run(upper)
  lower = upper
  upper *= UPPER_FACTOR
end

puts "binary search between #{lower} and #{upper} MB..."

while upper - lower > TOLERANCE
  mid = lower + (upper - lower) / 2

  if can_run(mid)
    upper = mid
  else
    lower = mid
  end
end

puts "minimum heap: #{upper} MB"
