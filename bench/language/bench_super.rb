require 'benchmark'

class A
  def foo0; end
  alias foo0a foo0
  alias foo0b foo0
  def foo1(a); end
  def foo4(a,b,c,d); end
end

class B < A
  def foo0a; super;end
  def foo0b; super();end
  def foo1(a); super(a); end
  def foo4(a,b,c,d); super(a,b,c,d); end
end

TIMES = (ARGV[0] || 5).to_i
Benchmark.bm(40) do |bm|
  TIMES.times do
    a = A.new
    b = B.new
    bm.report("control foo()") do
      1_000_000.times { a.foo0 }
    end
    bm.report("control foo(1)") do
      1_000_000.times { a.foo1(1) }
    end
    bm.report("control foo(1,2,3,4)") do
      1_000_000.times { a.foo4(1,2,3,4) }
    end
    bm.report("super foo") do
      1_000_000.times { b.foo0a }
    end
    bm.report("super foo()") do
      1_000_000.times { b.foo0b }
    end
    bm.report("super foo(1)") do
      1_000_000.times { b.foo1(1) }
    end
    bm.report("super foo(1,2,3,4)") do
      1_000_000.times { b.foo4(1,2,3,4) }
    end
  end
end