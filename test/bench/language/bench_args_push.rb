require 'benchmark'

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(30) do |bm|
    a = []
    bm.report("control, 1000k element assign") { 1000000.times { a[1,2] = 1 } }
    bm.report("args push, 1000k") { 1000000.times { a[*[1,2]] = 1 } }
  end
end
