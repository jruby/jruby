#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: except.ruby,v 1.1.1.1 2004-05-19 18:09:43 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/

$HI = 0
$LO = 0
NUM = Integer(ARGV[0] || 1)


class Lo_Exception < Exception
    def initialize(num)
        @value = num
        return self
    end
end

class Hi_Exception < Exception
    def initialize(num)
        @value = num
        return self
    end
end

def some_function(num)
    begin
	hi_function(num)
    rescue
        print "We shouldn't get here, exception is: #{$!.type}\n"
    end
end

def hi_function(num)
    begin
	lo_function(num)
    rescue Hi_Exception
	$HI = $HI + 1
    end
end

def lo_function(num)
    begin
	blowup(num)
    rescue Lo_Exception
	$LO = $LO + 1
    end
end

def blowup(num)
    if num % 2 == 0
	raise Lo_Exception.new(num)
    else
	raise Hi_Exception.new(num)
    end
end


for iter in 1 .. NUM
    some_function(iter)
end
print "Exceptions: HI=", $HI, " / LO=", $LO, "\n"
