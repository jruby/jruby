#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: objinst.ruby,v 1.1.1.1 2004-05-19 18:11:03 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/
# with help from Aristarkh Zagorodnikov

class Toggle
    def initialize(start_state)
	@bool = start_state
    end

    def value
	@bool
    end

    def activate
	@bool = !@bool
	self
    end
end

class NthToggle < Toggle
    def initialize(start_state, max_counter)
	super start_state
	@count_max = max_counter
	@counter = 0
    end

    def activate
	@counter += 1
	if @counter >= @count_max
	    @bool = !@bool
	    @counter = 0
	end
	self
    end
end

n = (ARGV.shift || 1).to_i

toggle = Toggle.new 1
5.times do
    puts toggle.activate.value ? 'true' : 'false'
end
n.times do
    toggle = Toggle.new 1
end

puts

ntoggle = NthToggle.new 1, 3
8.times do
    puts ntoggle.activate.value ? 'true' : 'false'
end
n.times do
    ntoggle = NthToggle.new 1, 3
end

