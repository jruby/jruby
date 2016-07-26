require 'java'
require 'benchmark'

TIMES = (ARGV[0] || 5).to_i

TIMES.times do
  Benchmark.bm(30) do |bm|
    bm.report("control") { ary = [1,2,3,4,5,6,7,8]; 1_000_000.times { ary } }
    bm.report("ary.to_java") { ary = [1,2,3,4,5,6,7,8]; 1_000_000.times { ary.to_java } }
    bm.report("ary.to_java :object") { ary = [1,2,3,4,5,6,7,8]; 1_000_000.times { ary.to_java :object } }
    bm.report("ary.to_java java.lang.Integer") { ary = [1,2,3,4,5,6,7,8]; 1_000_000.times { ary.to_java java.lang.Integer } }
    bm.report("ary.to_java :int") { ary = [1,2,3,4,5,6,7,8]; 1_000_000.times { ary.to_java :int } }
    bm.report("ary.to_java Java::int") { ary = [1,2,3,4,5,6,7,8]; 1_000_000.times { ary.to_java Java::int } }
    bm.report("ary.to_java :short") { ary = [1,2,3,4,5,6,7,8]; 1_000_000.times { ary.to_java :short } }

    bm.report("long_ary.to_java") do
      long_ary = (0..255).to_a
      100_000.times { long_ary.to_java }
    end
    bm.report("long_ary.to_java :int") do
      long_ary = (0..255).to_a
      100_000.times { long_ary.to_java :int }
    end
    bm.report("long_ary.to_java Java::int") do
      long_ary = (0..255).to_a
      100_000.times { long_ary.to_java Java::int }
    end
    bm.report("long_ary.to_java :short") do
      long_ary = (0..255).to_a
      100_000.times { long_ary.to_java :short }
    end
  end
end
