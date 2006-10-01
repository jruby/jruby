# This script implements a simple single-recursive fib algorithm and a test for successively approximating
# the maximum fib recursion depth possible.

def fib(i)
	fib_int(0, 1, 1, i)
end

def fib_int(i1, i2, count, max)
	if (count == max)
		i2
	else
		fib_int(i2, i2 + i1, count + 1, max)
	end
end

def fib_test
	puts "Estimating max fib recursion. This will be slightly lower than actual."

	last_good = 1
	current = 1
	last_bad = nil

	begin
		while (true)
			fib(current)
			
			last_good = current
			puts "good: #{last_good}"
			
			if last_bad
			  return last_good if last_bad == last_good + 1
			  current = last_good + (last_bad - last_good) / 2
			else
			  current = last_good * 2
			end
		end
	rescue SystemStackError
	    last_bad = current
	    puts "bad: #{last_bad}"
		if (last_bad == last_good + 1)
			return last_good
		else
			current = last_bad - (last_bad - last_good) / 2
			retry
		end
	end
end
