require 'benchmark'

class BenchYield
  def foocall(arg)
    arg
  end
  def foo
    yield 1
    yield 1
    yield 1
    yield 1
    yield 1
    yield 1
    yield 1
    yield 1
    yield 1
    yield 1
  end
  def foo2
    yield 1,2
    yield 1,2
    yield 1,2
    yield 1,2
    yield 1,2
    yield 1,2
    yield 1,2
    yield 1,2
    yield 1,2
    yield 1,2
  end
  def foo2_5
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
    yield 1,2,3
  end
  def foo3
    yield
    yield
    yield
    yield
    yield
    yield
    yield
    yield
    yield
    yield
  end
  def foo4
    foocall(1)
    foocall(1)
    foocall(1)
    foocall(1)
    foocall(1)
    foocall(1)
    foocall(1)
    foocall(1)
    foocall(1)
    foocall(1)
  end
end

def bench_yield(bm)
  bbi = BenchYield.new
  bm.report "1m x10 yield 1 to { }" do
    a = 5; 
    i = 0;
    while i < 1000000
      bbi.foo {}
      i += 1;
    end
  end
  
  bm.report "1m x10 yield to { }" do
    a = 5;
    i = 0;
    while i < 1000000
      bbi.foo3 {}
      i += 1;
    end
  end

  bm.report "1m x10 yield 1 to {|j| j}" do
    a = 5;
    i = 0;
    while i < 1000000
      bbi.foo {|j| j}
      i += 1;
    end
  end

  bm.report "1m x10 yield 1,2 to {|j,k| k}" do
    a = 5; 
    i = 0;
    while i < 1000000
      bbi.foo2 {|j,k| k}
      i += 1;
    end
  end

  bm.report "1m x10 yield 1,2,3 to {|j,k,l| k}" do
    a = 5; 
    i = 0;
    while i < 1000000
      bbi.foo2_5 {|j,k,l| k}
      i += 1;
    end
  end

  bm.report "1m x10 yield to {|j,k,l| k}" do
    a = 5;
    i = 0;
    while i < 1000000
      bbi.foo3 {|j,k,l| k}
      i += 1;
    end
  end

  bm.report "1m x10 yield 1,2,3 to {|*j| j}" do
    a = 5; 
    i = 0;
    while i < 1000000
      bbi.foo2_5 {|*j| j}
      i += 1;
    end
  end

  bm.report "1m x10 yield to {1}" do
    a = 5; 
    i = 0;
    while i < 1000000
      bbi.foo3 {1}
      i += 1;
    end
  end

  bm.report "1m x10 call(1) to def foo(a); a; end " do
    a = 5; 
    i = 0;
    while i < 1000000
      bbi.foo4
      i += 1;
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_yield(bm)} }
end