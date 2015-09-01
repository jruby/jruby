#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Counts how many classes are loaded to run a program.

# For example:
# $ bin/jruby -X+T -J-XX:+TraceClassLoading -e 'puts 14' 2>&1 | ruby test/truffle/startup/count-classes.rb

classes = 0

ARGF.each do |line|
  if line.start_with? '[Loaded '
    classes += 1
  else
    puts line
  end
end

puts "classes-loaded #{classes}"
