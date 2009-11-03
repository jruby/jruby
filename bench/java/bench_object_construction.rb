require 'java'
require 'benchmark'

JObject = java.lang.Object

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("1M java.lang.Object.new") { 1_000_000.times { JObject.new } }
  end
end
