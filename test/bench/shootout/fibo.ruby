#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: fibo.ruby,v 1.3 2005-04-25 19:01:38 igouy-guest Exp $
# http://www.bagley.org/~doug/shootout/

def fib(n)
    if n < 2 then
	   1
    else
	   fib(n-2) + fib(n-1)
    end
end

N = Integer(ARGV.shift || 1)
puts fib(N)
