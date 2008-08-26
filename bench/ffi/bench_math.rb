require 'benchmark'
require 'ffi'
iter = 100_000

module FFIMath
  extend FFI::Library
  ffi_lib 'm'
  attach_function :cos, [ :double ], :double
end
if FFIMath.cos(0) != 1
  raise ArgumentError, "FFI.cos returned incorrect value"
end
puts "Benchmark FFI cos(0) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { FFIMath.cos(0) }
  }
}

puts "Benchmark Math.cos(0) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { Math.cos(0) }
  }
}
