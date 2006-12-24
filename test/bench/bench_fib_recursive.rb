require 'benchmark'

def fib_ruby(n)
  if n < 2
    n
  else
    fib_ruby(n - 2) + fib_ruby(n - 1)
  end
end

puts Benchmark.measure { fib_ruby(30) }
puts Benchmark.measure { fib_ruby(30) }
