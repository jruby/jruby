require 'benchmark'
require 'ffi'

module FFITest
  attach_foreign(:double, :cos, [ :double ], :from => 'm')
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
