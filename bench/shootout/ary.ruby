#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: ary.ruby,v 1.3 2004-06-20 08:39:45 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/
# with help from Paul Brannan and Mark Hubbart

n = Integer(ARGV.shift || 1)

x = Array.new(n)
y = Array.new(n, 0)

for i in 0 ... n
  x[i] = i + 1
end

(0 .. 999).each do
  (n-1).step(0,-1) do |i|
    y[i] += x.at(i)
  end
end

puts "#{y.first} #{y.last}"
