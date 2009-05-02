require 'timeout'
require 'benchmark'

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report('control') do
      10_000.times { 1.times { self } }
    end
    bm.report('10k timeout calls') do
      10_000.times { Timeout.timeout(1) { self } }
    end
  end
end
