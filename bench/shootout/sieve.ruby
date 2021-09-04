#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: sieve.ruby,v 1.4 2004-11-10 06:48:59 bfulgham Exp $
# http://shootout.alioth.debian.org/
#
# Revised implementation by Paul Sanchez

NUM = Integer(ARGV.shift || 1)

max, flags = 8192, nil
flags0 = Array.new(max+1)
for i in 2 .. max
  flags0[i] = i
end

i=j=0

NUM.times do
    flags = flags0.dup
    #for i in 2 .. Math.sqrt(max)	#<-- This is much faster
    for i in 2 .. 8192 
	next unless flags[i]
	# remove all multiples of prime: i
	(i+i).step(max, i) do |j|
	    flags[j] = nil
	end
    end
end

print "Count: ", flags.compact.size, "\n"
