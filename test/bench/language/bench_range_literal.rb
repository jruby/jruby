require 'benchmark'

def bench_range_literal(bm)
  bm.report("control, two fixnums") { 1_000_000.times { 1; 10 }}
  bm.report("two fixnums and Range.new") { 1_000_000.times { Range.new(1,10) }}
  bm.report("literal range ..") { 1_000_000.times { 1..10 }}
  bm.report("literal range ...") { 1_000_000.times { 1...10 }}
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_range_literal(bm)} }
end