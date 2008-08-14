require 'benchmark'

class Foo
  def foo; self; end
end

class A < Foo; end; class B < A; end; class C < B; end; class D < C; end; class E < D; end
class F < E; end; class G < F; end; class H < G; end; class I < H; end; class J < I; end
class A2 < J; end; class B2 < A2; end; class C2 < B2; end; class D2 < C2; end; class E2 < D2; end
class F2 < E2; end; class G2 < F2; end; class H2 < G2; end; class I2 < H2; end; class J2 < I2; end
class A3 < J2; end; class B3 < A3; end; class C3 < B3; end; class D3 < C3; end; class E3 < D3; end
class F3 < E3; end; class G3 < F3; end; class H3 < G3; end; class I3 < H3; end; class J3 < I3; end

def bench_send(bm)
  bm.report("1m foo calls") { f = Foo.new; 10_000_000.times { f.foo } }
  bm.report("1m send :foo") { f = Foo.new; 10_000_000.times { f.send :foo } }
  bm.report("1m send 'foo'") { f = Foo.new; 10_000_000.times { f.send 'foo' } }
  bm.report("1m send \"\#{x}\"") { f = Foo.new; x = 'foo'; 10_000_000.times { f.send "#{x}" } }
  bm.report("1m 10th sub send :foo") { f = J.new; 10_000_000.times { f.send :foo } }
  bm.report("1m 20th sub send :foo") { f = J2.new; 10_000_000.times { f.send :foo } }
  bm.report("1m 30th sub send :foo") { f = J3.new; 10_000_000.times { f.send :foo } }
end

if $0 == __FILE__
  Benchmark.bmbm(40) {|bm| bench_send(bm)}
end
