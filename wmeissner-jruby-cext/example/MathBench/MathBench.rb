# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 

$LOAD_PATH.unshift(File.expand_path(File.join(__FILE__, "..")))
require 'mathbench.so'

b = MathBench.new
puts "MathBench.new returned #{b.inspect}"
puts "h.addi2(1, 2) returns #{b.addi2(1, 2)}"
puts "h.addf2(1.1, 2.2) returns #{b.addf2(1.1, 2.2)}"

def ruby_add2(i1, i2)
 i1 + i2
end

require 'ffi'
module Foreign
  extend FFI::Library
  ffi_lib FFI::Library::CURRENT_PROCESS
  attach_function :addi2, :ffi_addi2, [ :int, :int ], :int
  attach_function :addf2, :ffi_addf2, [ :double, :double ], :double
end

puts "Foreign.addi2(1, 2) returns #{Foreign.addi2(1, 2)}"
puts "Foreign.addf2(1.1, 2.2) returns #{Foreign.addf2(1.1, 2.2)}"

require 'benchmark'

iter = 1000_000
10.times do
 Benchmark.bmbm do |bm|
   bm.report("addi2 C (sml)") { iter.times { b.addi2(1, 2) } }
   bm.report("addi2 C (med)") { iter.times { b.addi2(1111, 2222) } }
   bm.report("addf2 C") { iter.times { b.addf2(1.1, 2.2) } }
   bm.report("addi2 FFI") { iter.times { Foreign.addi2(1, 2) } }
   bm.report("addf2 FFI") { iter.times { Foreign.addi2(1.1, 2.2) } }
   bm.report("addi2 Ruby") { iter.times { ruby_add2(1, 2) } }
   bm.report("addf2 Ruby") { iter.times { ruby_add2(1.1, 2.2) } }
 end
end
