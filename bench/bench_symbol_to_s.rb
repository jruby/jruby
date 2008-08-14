require 'benchmark'

5.times do
  puts Benchmark.measure { 1_000_000.times { :foo.to_s }}
end