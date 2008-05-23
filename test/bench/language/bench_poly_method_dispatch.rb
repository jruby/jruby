require 'benchmark'

Benchmark.bm(30) do |bm|
  5.times do
    bm.report("control, monomorphic dispatch") do
      x = Object.new
      y = x
      def x.foo; end
      i = 0
      while i < 1_000_000
        x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo
        x, y = y, x
        i += 1
      end
    end

    bm.report("test, bimorphic dispatch") do
      x = Object.new
      y = Object.new
      def x.foo; end
      def y.foo; end
      i = 0
      while i < 1_000_000
        x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo; x.foo; y.foo
        x, y = y, x
        i += 1
      end
    end
  end
end