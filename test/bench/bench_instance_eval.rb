require 'benchmark'

Benchmark.bmbm do |bm|
  bm.report("Control") { 10_000_000.times { 1 + 1 } }
  bm.report("instance_eval") { 10_000_000.times { instance_eval { 1 + 1 } } }
end
