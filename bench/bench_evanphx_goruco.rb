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