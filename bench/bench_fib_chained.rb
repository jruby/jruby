require 'benchmark'

class Fib
  def fib_ruby(n)
    if n < 2
      n
    else
      fib5(n - 2) + fib5(n - 1)
    end
  end
  def fibx(n); fib_ruby(n); end
  alias_method :fib, :fibx
  def fib2x(n); fib(n); end
  alias_method :fib2, :fib2x
  def fib3x(n); fib2(n); end
  alias_method :fib3, :fib3x
  def fib4x(n); fib3(n); end
  alias_method :fib4, :fib4x
  def fib5x(n); fib4(n); end
  alias_method :fib5, :fib5x
end

TIMES = (ARGV[0] || 5).to_i
N = (ARGV[1] || 30).to_i
TIMES.times {
  puts Benchmark.measure { Fib.new.fib5(N) }
}

