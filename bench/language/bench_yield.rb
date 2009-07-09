require 'benchmark'

class BenchYield
  def single_arg_method(arg)
    arg
  end

  def yield_one_arg
    yield 1; yield 1; yield 1; yield 1; yield 1 
    yield 1; yield 1; yield 1; yield 1; yield 1
  end

  def yield_two_args
    yield 1,2; yield 1,2; yield 1,2; yield 1,2; yield 1,2
    yield 1,2; yield 1,2; yield 1,2; yield 1,2; yield 1,2
  end

  def yield_three_args
    yield 1,2,3; yield 1,2,3; yield 1,2,3; yield 1,2,3; yield 1,2,3
    yield 1,2,3; yield 1,2,3; yield 1,2,3; yield 1,2,3; yield 1,2,3
  end

  def yield_no_args
    yield; yield; yield; yield; yield
    yield; yield; yield; yield; yield
  end

  def method_dispatch
    single_arg_method(1); single_arg_method(1); single_arg_method(1)
    single_arg_method(1); single_arg_method(1); single_arg_method(1)
    single_arg_method(1); single_arg_method(1); single_arg_method(1)
    single_arg_method(1)
  end
end

def bench_yield(bm)
  bbi = BenchYield.new
  bm.report "1m x10 yield 1 to { }" do
    i = 0
    while i < 1000000
      bbi.yield_one_arg {}
      i += 1
    end
  end
  
  bm.report "1m x10 yield to { }" do
    i = 0
    while i < 1000000
      bbi.yield_no_args {}
      i += 1
    end
  end

  bm.report "1m x10 yield 1 to {|j| j}" do
    i = 0
    while i < 1000000
      bbi.yield_one_arg {|j| j}
      i += 1
    end
  end

  bm.report "1m x10 yield 1,2 to {|j,k| k}" do
    i = 0
    while i < 1000000
      bbi.yield_two_args {|j,k| k}
      i += 1
    end
  end

  bm.report "1m x10 yield 1,2,3 to {|j,k,l| k}" do
    i = 0
    while i < 1000000
      bbi.yield_three_args {|j,k,l| k}
      i += 1
    end
  end

  bm.report "1m x10 yield to {|j,k,l| k}" do
    i = 0
    while i < 1000000
      bbi.yield_no_args {|j,k,l| k}
      i += 1
    end
  end

  bm.report "1m x10 yield 1,2,3 to {|*j| j}" do
    i = 0
    while i < 1000000
      bbi.yield_three_args {|*j| j}
      i += 1
    end
  end

  bm.report "1m x10 yield to {1}" do
    i = 0
    while i < 1000000
      bbi.yield_no_args {1}
      i += 1
    end
  end

  bm.report "1m x10 call() to a_method(p); p; end " do
    i = 0
    while i < 1000000
      bbi.method_dispatch
      i += 1
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_yield(bm)} }
end
