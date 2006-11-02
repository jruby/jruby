# This file defines some simple functions for the compiler and then attempts to run them
# Run this file using "jruby -C" to test the compiler

def fib_java(n)
	if n < 2
	  n
	else
	  fib_java(n - 2) + fib_java(n - 1)
	end
end

def fib_iter_java(n)
   i = 0
   j = 1
   cur = 1
   while cur <= n
     k = i
     i = j
     j = k + j
     cur = cur + 1
   end
   i
end

def comp_test
  begin
    nil
    1111111111111111111111111111111111111111111111111111111111
    1.0
    false
    true
    [1, 2, 3, 4, 5]
    "hello"
    x = 1
    while (x < 5)
      puts x
      x = x + 1
    end
    @@x = 5
    p @@x
  end
end

p comp_test()
p fib_java(30)
p fib_iter_java(300000)
