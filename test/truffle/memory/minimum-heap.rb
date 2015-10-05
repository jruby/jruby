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
# $ ruby test/truffle/memory/minimum-heap.rb foo bin/jruby "-X+T -e 'puts 14'"

NAME = ARGV[0]
COMMAND = ARGV[1]
OPTIONS = ARGV[2]

TOLERANCE = 1024
UPPER_FACTOR = 4

begin
  `timeout --help`
  TIMEOUT = 'timeout'
rescue
  TIMEOUT = 'gtimeout'
end

def can_run(heap)
  print "trying #{heap} KB... "

  output = `#{TIMEOUT} 120s #{COMMAND} -J-Xmx#{heap}k #{OPTIONS} 2>&1`
  can_run = $?.exitstatus != 124 && !output.include?('OutOfMemoryError')

  if can_run
    puts "yes"
  else
    puts "no"
  end

  can_run
end

puts "looking for an upper bound..."

lower = 0
upper = 1024

while !can_run(upper)
  lower = upper
  upper *= UPPER_FACTOR
end

puts "binary search between #{lower} and #{upper} KB..."

while upper - lower > TOLERANCE
  mid = lower + (upper - lower) / 2

  if can_run(mid)
    upper = mid
  else
    lower = mid
  end
end

puts "#{NAME}: #{upper} KB"
