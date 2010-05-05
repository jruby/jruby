require 'benchmark'

TIMES = (ARGV[0] || 1).to_i
ITERS = (ARGV[1] || 10_000).to_i
TIMES.times do
  Benchmark.bm(40) do |bm|
    bm.report("5-way x#{ITERS} conc class << Object.new") do
      (1..5).to_a.map do |i|
        Thread.new do
          ITERS.times do
            class << Object.new
            end
          end
        end
      end.map(&:join)
    end
  end
end