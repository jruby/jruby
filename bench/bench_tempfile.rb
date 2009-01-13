require 'benchmark'
require 'tempfile'

puts "10k Tempfile.open(file)"
10.times {
  puts Benchmark.measure {
    i = 0
    while i < 1_000
      Tempfile.new("heh")
      i = i + 1
    end
  }
}

