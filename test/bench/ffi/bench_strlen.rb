require 'benchmark'
require 'ffi'
iter = 100000
str = "test"

module JLibC
  extend JFFI::Library
  attach_function :strlen, [ :string ], :int
end
module RbxLibC
  extend FFI::Library
  attach_function :strlen, [ :string ], :int
end

if RbxLibC.strlen("test") != 4
  raise ArgumentError, "rubinius FFI.strlen returned incorrect value"
end
if JLibC.strlen("test") != 4
  raise ArgumentError, "jruby FFI.strlen returned incorrect value"
end
puts "Benchmark rubinius FFI api strlen(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { RbxLibC.strlen(str) }
  }
}

puts "Benchmark jruby FFI api strlen(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { JLibC.strlen(str) }
  }
}
