require 'benchmark'

def bench_ensure(bm)
  def foo1; 1; end
  bm.report("control: 1m calls to def foo; 1; end") do
    100_000.times do
      foo1; foo1; foo1; foo1; foo1
      foo1; foo1; foo1; foo1; foo1
    end
  end
  
  def foo2; 1; ensure; 1; end
  bm.report("1m calls to foo with ensure") do
    100_000.times do
      foo2; foo2; foo2; foo2; foo2
      foo2; foo2; foo2; foo2; foo2
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_ensure(bm)} }
end
