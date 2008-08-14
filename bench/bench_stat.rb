require 'benchmark'

puts "10k File.stat(file)"
10.times {
  puts Benchmark.measure {
    i = 0
    while i < 10_000
      File.stat("/tmp")
      i = i + 1
    end
  }
}

