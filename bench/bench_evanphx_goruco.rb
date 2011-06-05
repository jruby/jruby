# This benchmark was created by Evan Phoenix for GoRuCo 2011 to demonstrate
# Rubinius performance (especially GC performance) and memory effects. It
# largely just creates a lot of dead objects in a tight loop and then a few
# live ones in a chain. The original code used `ps` results to parse out memory
# size; I have removed that here since it isn't interesting as a perf benchmark
# metric and it muddies the remainder of the benchmark.

require 'benchmark'

class Simple
  attr_accessor :next
end

(ARGV[0] || 5).to_i.times {
  puts Benchmark.measure {
    outer = 10
    total = 100000
    per = 100

    top = Simple.new
    a = nil

    outer.times do

      total.times do
        per.times { Simple.new }
        s = Simple.new
        top.next = s
        top = s
      end
    end
  }
}