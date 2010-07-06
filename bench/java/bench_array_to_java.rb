require 'java'
require 'benchmark'

TIMES = (ARGV[0] || 5).to_i

TIMES.times do
  Benchmark.bm(30) do |bm|
    bm.report("control") {a = [1,2,3,4]; 100_000.times {a}}
    bm.report("ary.to_java") {a = [1,2,3,4]; 100_000.times {a.to_java}}
    bm.report("ary.to_java :object") {a = [1,2,3,4]; 100_000.times {a.to_java :object}}
    bm.report("ary.to_java :string") {a = [1,2,3,4]; 100_000.times {a.to_s.to_java :string}}
  end
end
