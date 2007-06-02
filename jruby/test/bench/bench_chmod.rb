require 'benchmark'

puts "Benchmark chmod performance, 1000x changing mode"
5.times {
  puts Benchmark.measure {
    1000.times { File.chmod(0622, "README") }
  }
}
