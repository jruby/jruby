require 'benchmark'
require 'ffi'
iter = 1000000
len = 10
str = "test" * len

module LibC
  extend FFI::Library
  ffi_lib 'c'
  attach_function :strlen, [ :string ], :int
end

if LibC.strlen("test") != 4
  raise ArgumentError, "jruby FFI.strlen returned incorrect value"
end

puts "Benchmark jruby FFI api strlen(3) memoized performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { LibC.strlen(str) }
  }
}

puts "Benchmark jruby FFI api strlen(3) new string performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { LibC.strlen("test" * len) }
  }
}
