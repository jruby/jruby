require 'benchmark'

(ARGV[0] || 10).to_i.times do
  method = method(:to_s)
  method_proc = method.to_proc
  Benchmark.bm(30) do |bm|
    bm.report("control, 1000k to_s calls") { 1000000.times { to_s } }
    bm.report("Method.call, 1000k calls") { 1000000.times { method.call } }
    bm.report("to_proc'ed Method.call, 1000k calls") { 1000000.times { method_proc.call } }
  end
end
