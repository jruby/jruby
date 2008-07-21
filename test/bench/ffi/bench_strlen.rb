# 
# This benchmark is not very real world, but does benchmark string argument passing
#
 
require 'benchmark'
require 'ffi'

module FFITest
  attach_foreign(:int, :strlen, [ :string ])
end
if FFITest.strlen("test") != 4
  raise ArgumentError, "FFI.strlen returned incorrect value"
end
puts "Benchmark FFI strlen(3) performance, 10000x"
10.times {
  puts Benchmark.measure {
    10000.times { FFITest.strlen("test") }
  }
}
