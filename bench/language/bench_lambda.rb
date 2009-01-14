require 'benchmark'

TIMES = (ARGV[0] || 1).to_i

TIMES.times do
  Benchmark.bm(30) do |bm|
    bm.report("control, 10m times") do
      ell = nil
      10_000_000.times { ell }
    end
    bm.report("10m lambda.call no args") do
      ell = lambda {self}
      10_000_000.times { ell.call }
    end
  end
end