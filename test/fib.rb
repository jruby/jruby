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
	last_good = 1
	current = 1

	begin
		while (true)
			fib(current)
			
			last_good = current
			current = last_good * 2
		end
	rescue SystemStackError
		if (current == last_good + 1)
			return last_good
		else
			current = current - (current - last_good) / 2
			retry
		end
	end
end
