require 'benchmark'

(ARGV[0] || 10).to_i.times do
  Benchmark.bm(20) do |bm|
    bm.report('control loop') { 10_000.times { 1 } }
    bm.report('Thread.new.join loop') { 10_000.times { Thread.new { 1 }.join } }
  end
end
