require 'benchmark'
require 'ffi'

module FFITest
  extend FFI::Library
  ffi_lib 'm'
  attach_function :cos, [ :double ], :double
end
if FFITest.cos(0) != 1
  raise ArgumentError, "FFI.cos returned incorrect value"
end
puts "Benchmark FFI cos(3m) performance, 10000x"
10.times {
  puts Benchmark.measure {
    10000.times { FFITest.cos(0) }
  }
}
