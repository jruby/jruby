require 'benchmark'

class A
  def foo0; end
  def foo0a; end
  def foo0b; end
  def foo1(a); end
  def foo4(a,b,c,d); end
  def nofoo0; foo0; foo0; foo0; foo0; foo0; foo0; foo0; foo0; foo0; foo0; end
  def nofoo1(a); foo1(a); foo1(a); foo1(a); foo1(a); foo1(a); foo1(a); foo1(a); foo1(a); foo1(a); foo1(a); end
  def nofoo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); foo4(a,b,c,d); end
end

class B < A
  def foo0a; super; super; super; super; super; super; super; super; super; super; end
  def foo0b; super(); super(); super(); super(); super(); super(); super(); super(); super(); super(); end
  def foo1(a); super(a); super(a); super(a); super(a); super(a); super(a); super(a); super(a); super(a); super(a); end
  def foo4(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); super(a,b,c,d); end
end

TIMES = (ARGV[0] || 5).to_i
Benchmark.bm(40) do |bm|
  TIMES.times do
    a = A.new
    b = B.new
    bm.report("control foo()") do
      1_000_000.times { a.nofoo0 }
    end
    bm.report("control foo(1)") do
      1_000_000.times { a.nofoo1(1) }
    end
    bm.report("control foo(1,2,3,4)") do
      1_000_000.times { a.nofoo4(1,2,3,4) }
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