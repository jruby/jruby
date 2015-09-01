#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Calculates where the time goes for important regions while running a program.

# For example:
# $ test/truffle/startup/jruby-timed -X+T -Xtruffle.metrics.time=true -e 'puts 14' 2>&1 | ruby test/truffle/startup/process-times.rb

before_times = {}
after_times = {}
nesting = 0
accounted = 0

ARGF.each do |line|
  if line =~ /([\w-]+) (\d+\.\d+)/
    id = $1.split('-')
    relative = id.first
    region = id.drop(1).join('-')
    time = $2.to_f

    case relative
    when 'before'
      before_times[region] = time
      nesting += 1
    when 'after'
      after_times[region] = time
      elapsed = time - before_times[region]

      # 3 means ignore launcher, ignore main, then count regions within that
      if nesting == 3 && !(['launcher', 'main'].include? region)
        accounted += elapsed
      end

      puts "#{region} #{elapsed}"
      nesting -= 1
    end
  end
end

total = after_times['launcher'] - before_times['launcher']

puts "accounted #{accounted}"
puts "unaccounted #{total - accounted}"
