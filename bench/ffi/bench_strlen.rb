require 'benchmark'
require 'ffi'
iter = 100000
str = "test"

module LibC
  extend FFI::Library
  attach_function :strlen, [ :string ], :int
end

if LibC.strlen("test") != 4
  raise ArgumentError, "jruby FFI.strlen returned incorrect value"
end

puts "Benchmark jruby FFI api strlen(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { LibC.strlen(str) }
  }
}
