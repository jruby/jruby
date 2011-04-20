require 'benchmark'

def fib_ruby(n)
  if n < 2.0
    n
  else
    fib_ruby(n - 2.0) + fib_ruby(n - 1.0)
  end
end

TIMES = (ARGV[0] || 5).to_i
N = (ARGV[1] || 30.0).to_f
TIMES.times {
  puts Benchmark.measure { puts fib_ruby(N) }
}
