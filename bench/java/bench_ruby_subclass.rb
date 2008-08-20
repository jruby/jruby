require 'java'
require 'benchmark'

TIMES = (ARGV[0] || 5).to_i
Benchmark.bm(30) do |bm|
  TIMES.times do
    bm.report("control, Object.new") { 1_000_000.times { Object.new }}
    bm.report("new class < java Object") do
      myClass = Class.new(java.lang.Object)
      myClass.new
      1_000_000.times { myClass.new }
    end
  end
end