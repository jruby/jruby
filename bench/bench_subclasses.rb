require 'benchmark/ips'

Benchmark.ips do |bm|
  [1, 5, 10, 50].each do |count|
    bm.report("#{count} thread Numeric.subclasses") do
      count.times.map {
        Thread.new {
          i = 10_000 / count
          while i > 0
            Numeric.subclasses
            i-=1
          end
        }
      }.each(&:join)
    end
    bm.report("#{count} thread Object.subclasses") do
      count.times.map {
        Thread.new {
          i = 10_000 / count
          while i > 0
            Object.subclasses
            i-=1
          end
        }
      }.each(&:join)
    end
  end
end