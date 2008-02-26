require 'benchmark'

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("control, two fixnums") { 1_000_000.times { 1; 10 }}
    bm.report("two fixnums and Range.new") { 1_000_000.times { Range.new(1,10) }}
    bm.report("literal range ..") { 1_000_000.times { 1..10 }}
    bm.report("literal range ...") { 1_000_000.times { 1...10 }}
  end
end