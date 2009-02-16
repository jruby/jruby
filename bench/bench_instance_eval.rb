require 'benchmark'

Benchmark.bmbm do |bm|
  bm.report("Control") { 10_000_000.times { 1 + 1 } }
  bm.report("instance_eval a block") { 10_000_000.times { instance_eval { 1 + 1 } } }
  if defined? instance_exec
    bm.report("instance_exec a block") { 10_000_000.times { instance_exec { 1 + 1 } } }
  end
  bm.report("instance_eval a string (* 0.1 times)") { 1_000_000.times { instance_eval " 1 + 1 " } }
end
