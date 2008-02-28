require 'benchmark'

def bench_rescue(bm)
  bm.report("100k rescue with no raise") do
    100_000.times { begin; rescue; end }
  end

  bm.report("100k rescue with raise") do
    100_000.times { begin; raise; rescue; end }
  end

  bm.report("100k inline rescue") do
    100_000.times { raise rescue nil }
  end

  bm.report("100k missing method with raise") do
    100_000.times { __NOMETHOD__ rescue nil }
  end
end

if $0 == __FILE__
  Benchmark.bmbm(40) {|bm| bench_rescue(bm)}
end
