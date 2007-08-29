#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: nestedloop.ruby,v 1.1.1.1 2004-05-19 18:10:57 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/
# from Avi Bryant

n = Integer(ARGV.shift || 1)
x = 0
n.times do
    n.times do
	n.times do
	    n.times do
		n.times do
		    n.times do
			x += 1
		    end
		end
	    end
	end
    end
end
puts x
