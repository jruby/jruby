require 'benchmark'

class Foo
  def foo; self; end
end

class A < Foo; end; class B < A; end; class C < B; end; class D < C; end; class E < D; end
class F < E; end; class G < F; end; class H < G; end; class I < H; end; class J < I; end

def bench_send(bm)
  bm.report("1m foo calls") { f = Foo.new; 10_000_000.times { f.foo } }
  bm.report("1m foo sends") { f = Foo.new; 10_000_000.times { f.send :foo } }
  bm.report("1m 10th subclass foo sends") { f = J.new; 10_000_000.times { f.send :foo } }
end

if $0 == __FILE__
  Benchmark.bmbm(40) {|bm| bench_send(bm)}
end
