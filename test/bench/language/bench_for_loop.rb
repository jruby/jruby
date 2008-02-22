require 'benchmark'

def bench1
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
  for i in 1..10
    i
  end
end

puts "100k calls to a method containing 5x a for loop over a 10-element range"
5.times {
  puts Benchmark.measure {
    a = 1
    while a < 100_000
      bench1
      a += 1
    end
  }
}
