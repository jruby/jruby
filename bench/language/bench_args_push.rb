require 'benchmark'

def bench_args_push(bm)
  a = []
  bm.report("control, 1000k element assign") { 1000000.times { a[1,2] = 1 } }
  bm.report("args push, 1000k") { 1000000.times { a[*[1,2]] = 1 } }
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(30) {|bm| bench_args_push(bm)} }
end
