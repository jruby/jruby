require 'benchmark'

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("control, block with self") { 1_000_000.times { self }}
    bm.report("1m times begin") { 1_000_000.times { begin; end }}
    bm.report("1m times begin/ensure") { 1_000_000.times { begin; ensure; end }}
    bm.report("1m times begin/rescue") { 1_000_000.times { begin; rescue; end }}
    bm.report("1m times begin/rescue/ensure") { 1_000_000.times { begin; rescue; ensure; end }}
  end
end