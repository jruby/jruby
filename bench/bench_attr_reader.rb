require 'benchmark/ips'

Benchmark.ips do |bm|
  bm.report("attr_reader") do |i|
    Class.new do
      while i > 0
        i-=1
        attr_reader :foo
      end
    end
  end
end