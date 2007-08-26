require 'java'
require 'benchmark'

import java.util.ArrayList

puts "method invocation, no subclass"
5.times {
  t = ArrayList.new
  puts Benchmark.measure {
    i = 0
    while i < 1000000
      i+= 1
      t.size()
    end
  }
}

class MyArrayList < ArrayList
end

puts "method invocation, with subclass"
5.times {
  t = MyArrayList.new
  puts Benchmark.measure {
    i = 0
    while i < 1000000
      i+= 1
      t.size()
    end
  }
}
