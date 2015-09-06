#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Calculates the total memory allocated to run a program.

# For example:
# $ bin/jruby -X+T -Xtruffle.metrics.memory_used_on_exit=true -J-verbose:gc -e 'puts 14' 2>&1 | ruby test/truffle/memory/total-allocation.rb

on_exit = nil
allocated = 0

ARGF.each do |line|
  if line =~ /(\d+)K->(\d+)K/
    before = $1.to_i * 1024
    after = $2.to_i * 1024
    collected = before - after
    allocated += collected
  elsif line =~ /^allocated (\d+)$/
    on_exit = $1.to_i
  else
    puts line
  end
end

allocated += on_exit

puts "total-allocation: #{allocated/1024.0} KB"
