require 'benchmark'

c = 0
script = 'a = 1; b = 2; c = c + a + b'
bnd = binding

puts "implicit binding"
puts Benchmark.realtime { 1000000.times { eval script } }
puts "c = #{c}"
puts "explicit binding"
puts Benchmark.realtime { 1000000.times { eval script, bnd } }
puts "c = #{c}"
