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

TIMES = (ARGV[0] || 5).to_i
N = (ARGV[1] || 300000).to_i
TIMES.times {
   puts Benchmark.measure { fib_iter_ruby(300000) }
}
