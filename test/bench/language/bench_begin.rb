require 'benchmark'

def bench_begin(bm)
  bm.report("control, block with self") { 1_000_000.times { self }}
  bm.report("1m times begin") { 1_000_000.times { begin; end }}
  bm.report("1m times begin/ensure") { 1_000_000.times { begin; ensure; end }}
  bm.report("1m times begin/rescue") { 1_000_000.times { begin; rescue; end }}
  bm.report("1m times begin/rescue/ensure") { 1_000_000.times { begin; rescue; ensure; end }}
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(30) {|bm| bench_begin(bm)} }
end