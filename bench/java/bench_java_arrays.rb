require 'java'
require 'benchmark'

TIMES = (ARGV[0] || 5).to_i

TIMES.times do
  Benchmark.bm(30) do |bm|
    bm.report("control") {a = [1,2,3,4].to_java; 100_000.times {a}}
    bm.report("java_ary[0]") {a = [1,2,3,4].to_java; 100_000.times {a[0]}}
    bm.report("java_ary[1,2]") {a = [1,2,3,4].to_java; 100_000.times {a[1,2]}}
    bm.report("java_ary.each {|x|}") {a = [1,2,3,4].to_java; 100_000.times {a.each {|x|}}}
    bm.report("java_ary + java_ary") {a = [1,2,3,4].to_java; 100_000.times {a + a}}
    bm.report("java_ary.to_a") {a = [1,2,3,4].to_java; 100_000.times {a.to_a}}
  end
end