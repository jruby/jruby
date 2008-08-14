require 'benchmark'

Benchmark.bm(30) do |bm|
  x1 = Object.new
  x2 = Object.new
  def x1.foo; end
  def x2.foo; end

  5.times do
    bm.report("control, monomorphic dispatch") do
      x = x1
      y = x1
      i = 0
      while i < 1_000_000
        x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo
        x, y = y, x
        i += 1
      end
    end

    bm.report("test, bimorphic dispatch") do
      x = x1
      y = x2
      i = 0
      while i < 1_000_000
        x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo
        x, y = y, x
        i += 1
      end
    end
  end
end
