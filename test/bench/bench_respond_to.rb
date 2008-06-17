require 'benchmark'

(ARGV[0] || 10).to_i.times do
  obj = Object.new
  def obj.respond_to?(sym)
    true
  end
  Benchmark.bm(35) do |bm|
    bm.report("control, 1 and :to_ary") { 1_000_000.times { 1; :to_ary }}
    bm.report("1m 1.respond_to?(:to_ary)") { 1_000_000.times { 1.respond_to? :to_ary }}
    bm.report("1m 1.respond_to?(:to_ary,true)") { 1_000_000.times { 1.respond_to?(:to_ary, true) }}
    bm.report("1m 1.respond_to?(:next)") { 1_000_000.times { 1.respond_to? :next }}
    bm.report("1m redefined obj.respond_to?") { 1_000_000.times { obj.respond_to? :next }}
  end
end
