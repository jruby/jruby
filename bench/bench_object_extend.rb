require 'benchmark'

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report("1m obj.new.extend") { 1_000_000.times { Object.new.extend(Enumerable) } }
  end
end
