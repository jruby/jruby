require 'benchmark/ips'

Benchmark.ips do |bm|
  bm.report("'%g' % 1.0") do |n|
    format = '%g'
    n.times { format % 1.0 }
  end

  bm.report("4x contended '%g' % 1.0") do |n|
    format = '%g'
    (1..4).map { Thread.new { n.times { format % 1.0 } } }.map(&:join)
  end
end
