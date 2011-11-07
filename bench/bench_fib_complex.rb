require 'rubygems'
begin
  require 'benchmark/ips'
rescue loadError
  fail "install the benchmark-suite gem"
end

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

def fib_ruby1(n)
  a = 1
  b = 2
  if n < b
    n
  else
    fib_ruby(n - b) + fib_ruby(n - a)
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

N = (ARGV[1] || 30).to_i

Benchmark.ips do |x|
  x.report "normal fib" do
    fib_ruby(N)
  end
  
  x.report "fib with variables" do
    fib_ruby1(N)
  end
  
  x.report "fib with constants" do
    fib_ruby2(N)
  end
  
  x.report "fib with additional calls" do
    fib_ruby3(N)
  end
  
  x.report "fib with constants and additional calls" do
    fib_ruby4(N)
  end
end  
