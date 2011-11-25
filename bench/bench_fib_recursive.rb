require 'benchmark'

def fib_ruby(n)
  if n < 2
    n
  else
    fib_ruby(n - 2) + fib_ruby(n - 1)
  end
end

TIMES = (ARGV[0] || 5).to_i
N = (ARGV[1] || 30).to_i
TIMES.times {
  puts Benchmark.measure { fib_ruby(N) }
}
