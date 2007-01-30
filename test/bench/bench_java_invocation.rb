require 'benchmark'
require 'java'

puts Benchmark.measure { sb = java.lang.StringBuffer.new; 100000.times { sb.append("foo") } }
puts Benchmark.measure { sb = java.lang.StringBuffer.new; 100000.times { sb.append("foo") } }
