require 'benchmark'

ONE = 1
TWO = 2
def plus(a, b)
  a + b
end

def minus(a, b)
  a - b
end

def lt(a, b)
  a < b
end

def fib_ruby(n)
  if n < 2
    n
  else
    fib_ruby(n - 2) + fib_ruby(n - 1)
  end
end

def fib_ruby2(n)
  if n < TWO
    n
  else
    plus(fib_ruby2(n - TWO), fib_ruby2(n - ONE))
  end
end

def fib_ruby3(n)
  if lt(n, 2)
    n
  else
    plus(fib_ruby3(minus(n, 2)), fib_ruby3(minus(n, 1)))
  end
end

def fib_ruby4(n)
  if lt(n, TWO)
    n
  else
    plus(fib_ruby4(minus(n, TWO)), fib_ruby4(minus(n, ONE)))
  end
end

TIMES = (ARGV[0] || 5).to_i
N = (ARGV[1] || 30).to_i
TIMES.times {
  puts "normal fib"
  puts Benchmark.measure { puts fib_ruby(N) }
  puts "fib with constants"
  puts Benchmark.measure { puts fib_ruby2(N) }
  puts "fib with additional calls"
  puts Benchmark.measure { puts fib_ruby3(N) }
  puts "fib with constants and additional calls"
  puts Benchmark.measure { puts fib_ruby4(N) }
}
