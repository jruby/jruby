require 'benchmark/ips'

Custom = Class.new(Object)
# create these once and hold references
SINGLETON_COUNT = 1000
many_singletons = SINGLETON_COUNT.times.map { c = Custom.new; class << c; end; c }

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
    bm.report("#{count} thread Custom.subclasses with #{SINGLETON_COUNT} singletons") do
      count.times.map {
        Thread.new {
          i = 10_000 / count
          while i > 0
            Custom.subclasses
            i-=1
          end
        }
      }.each(&:join)
    end
  end
end