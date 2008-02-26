require 'benchmark'

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("control, 1 and :to_ary") { 1_000_000.times { 1; :to_ary }}
    bm.report("1m 1.respond_to?(:to_ary)") { 1_000_000.times { 1.respond_to? :to_ary }}
  end
end