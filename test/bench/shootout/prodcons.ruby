#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: prodcons.ruby,v 1.3 2005-06-10 00:57:22 igouy-guest Exp $
# http://www.bagley.org/~doug/shootout/

require 'thread'

def main(n)
    mutex = Mutex.new
    access = ConditionVariable.new
    count = data = consumed = produced = 0
    consumer = Thread.new do
	i = 0
	loop do
	    mutex.synchronize {
		while count == 0 do access.wait(mutex) end
		i = data
		count = 0
		access.signal
	    }
	    consumed += 1
	    if i == n then break end
	end
    end
    producer = Thread.new do
	for i in 1 .. n do
	    mutex.synchronize {
		while count == 1 do access.wait(mutex) end
		data = i
		count = 1
		access.signal
	    }
	    produced += 1
	end
    end
    producer.join
    consumer.join
    puts "#{produced} #{consumed}"
end

main(Integer(ARGV.shift || 1))
