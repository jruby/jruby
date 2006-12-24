require 'benchmark'

def fib_iter_ruby(n)
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

puts Benchmark.measure { fib_iter_ruby(300000) }
puts Benchmark.measure { fib_iter_ruby(300000) }
