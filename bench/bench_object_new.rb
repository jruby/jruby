require 'benchmark'
 
class Blah; end
 
total = 10_000_000
 
def control(tot)
  puts Benchmark.measure {
    total = tot
    i = 0
    while i < total
      Blah
      i += 1
    end
  }
end
 
puts "10M control loops:"
5.times {
  control total
}

def foo(tot)
  puts Benchmark.measure {
    total = tot
    i = 0
    while i < total
      Blah.allocate
      i += 1
    end
  }
end
 
puts "10M .allocate calls:"
5.times {
  foo total
}
 
def bar(tot)
  puts Benchmark.measure {
    total = tot
    i = 0
    while i < total
      Blah.new
      i += 1
    end
  }
end
 
puts "10M .new calls:"
5.times {
  bar total
}
