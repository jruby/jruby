# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
include Java

m = org.jruby.cext.ModuleLoader.new
m.load(self, "defineclass")

h = Hello.new
puts "Hello.new returned #{h.inspect}"
puts "h.get_hello returns #{h.get_hello}"

def ruby_hello
 "Hello from Ruby"
end

require 'benchmark'

iter = 100_000
10.times do
 Benchmark.bmbm do |bm|
   bm.report("C") { iter.times { h.get_hello } }
   bm.report("Ruby") { iter.times { ruby_hello } }
 end
end
